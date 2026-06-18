package com.example.authtutorial.oauth.domain.model;

import com.example.authtutorial.common.domain.DomainException;
import com.example.authtutorial.common.domain.Subject;

/**
 * 聚合根：授權許可 (Authorization Grant)。
 *
 * <p>對應圖中 OAuth 第 2～4 步：使用者被導到授權伺服器、登入並<b>同意 (consent)</b>，
 * 授權伺服器才核發 token。本聚合守護兩條核心規則：
 * <ol>
 *   <li>未經使用者同意，不得核准任何授權。</li>
 *   <li>實際核准的 scope 必須是 App 要求的 scope 的子集。</li>
 * </ol>
 */
public class AuthorizationGrant {

    private final ClientId client;
    private final Subject resourceOwner;
    private final Scopes requestedScopes;
    private boolean consentGiven;

    private AuthorizationGrant(ClientId client, Subject resourceOwner, Scopes requestedScopes) {
        this.client = client;
        this.resourceOwner = resourceOwner;
        this.requestedScopes = requestedScopes;
        this.consentGiven = false;
    }

    /** App 發起授權請求，要求一組 scope。 */
    public static AuthorizationGrant request(ClientId client, Subject resourceOwner, Scopes requestedScopes) {
        if (client == null || resourceOwner == null || requestedScopes == null) {
            throw new DomainException("授權請求缺少必要資訊");
        }
        return new AuthorizationGrant(client, resourceOwner, requestedScopes);
    }

    /** 使用者「同意 (consent)」授權。 */
    public void giveConsent() {
        this.consentGiven = true;
    }

    /**
     * 核准一組 scope，回傳「最終生效的 scope」。
     *
     * @throws DomainException 若尚未取得同意，或核准的 scope 超出要求範圍。
     */
    public Scopes approve(Scopes approvedScopes) {
        if (!consentGiven) {
            throw new DomainException("使用者尚未同意，不能核發授權");
        }
        if (!requestedScopes.containsAll(approvedScopes)) {
            throw new DomainException("核准的 scope 不可超出 App 要求的範圍");
        }
        return approvedScopes;
    }

    public ClientId client() {
        return client;
    }

    public Subject resourceOwner() {
        return resourceOwner;
    }

    public Scopes requestedScopes() {
        return requestedScopes;
    }

    public boolean isConsentGiven() {
        return consentGiven;
    }
}
