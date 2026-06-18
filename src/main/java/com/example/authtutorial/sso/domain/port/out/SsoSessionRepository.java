package com.example.authtutorial.sso.domain.port.out;

import com.example.authtutorial.sso.domain.model.SessionId;
import com.example.authtutorial.sso.domain.model.SsoSession;

import java.util.Optional;

/**
 * 被驅動端埠：工作階段的儲存庫 (Repository)。
 *
 * <p>DDD 的 Repository 介面屬於 domain，實作 (in-memory、Redis…) 屬於 adapter。</p>
 */
public interface SsoSessionRepository {

    void save(SsoSession session);

    Optional<SsoSession> findById(SessionId id);
}
