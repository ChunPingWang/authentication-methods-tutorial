package com.example.authtutorial.sso.domain.model;

import com.example.authtutorial.common.domain.DomainException;
import com.example.authtutorial.common.domain.Subject;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 聚合根 (Aggregate Root)：SSO 單一登入工作階段。
 *
 * <p>這是「SSO 是體驗、不是協定」這句話的程式化呈現：使用者
 * <b>登入一次</b>(建立 session)，之後便能<b>免再次登入</b>存取多個 App。</p>
 *
 * <p>聚合根負責守護自身的業務規則 (invariants)：
 * <ul>
 *   <li>工作階段過期後，不能再用來存取任何 App。</li>
 *   <li>會記錄此 session 已存取過哪些 App（對應圖中 App A、App B）。</li>
 * </ul>
 */
public class SsoSession {

    private final SessionId id;
    private final Subject subject;
    private final Instant issuedAt;
    private final Instant expiresAt;
    private final Set<String> accessedApplications;

    private SsoSession(SessionId id, Subject subject, Instant issuedAt, Instant expiresAt) {
        this.id = id;
        this.subject = subject;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.accessedApplications = new LinkedHashSet<>();
    }

    /**
     * 工廠方法：使用者成功登入後，由 IdP 建立一個新的工作階段。
     * 對應圖中 SSO 第 4 步「IdP creates session/token」。
     *
     * @param subject 已被驗證的主體。
     * @param now     現在時間（由外部 Clock 提供，方便測試）。
     * @param ttl     存活時間 (time-to-live)。
     */
    public static SsoSession start(Subject subject, Instant now, Duration ttl) {
        if (subject == null) {
            throw new DomainException("建立工作階段需要已驗證的主體");
        }
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new DomainException("工作階段存活時間必須為正值");
        }
        return new SsoSession(SessionId.newId(), subject, now, now.plus(ttl));
    }

    /** 重建既有工作階段（供 repository 從儲存體還原時使用）。 */
    public static SsoSession rehydrate(SessionId id, Subject subject, Instant issuedAt,
                                       Instant expiresAt, Set<String> accessedApplications) {
        SsoSession session = new SsoSession(id, subject, issuedAt, expiresAt);
        session.accessedApplications.addAll(accessedApplications);
        return session;
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    /**
     * 用既有工作階段存取一個 App —— 這就是 SSO 的精髓：
     * 「User can access App B without logging in」(圖中第 5 步)。
     *
     * @throws DomainException 若工作階段已過期。
     */
    public void accessApplication(String applicationId, Instant now) {
        if (applicationId == null || applicationId.isBlank()) {
            throw new DomainException("App 識別碼不可為空白");
        }
        if (isExpired(now)) {
            throw new DomainException("工作階段已過期，必須重新登入");
        }
        accessedApplications.add(applicationId);
    }

    public boolean hasAccessed(String applicationId) {
        return accessedApplications.contains(applicationId);
    }

    public SessionId id() {
        return id;
    }

    public Subject subject() {
        return subject;
    }

    public Instant issuedAt() {
        return issuedAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Set<String> accessedApplications() {
        return Collections.unmodifiableSet(accessedApplications);
    }
}
