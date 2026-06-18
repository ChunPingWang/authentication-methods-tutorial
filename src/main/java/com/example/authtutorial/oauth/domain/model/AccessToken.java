package com.example.authtutorial.oauth.domain.model;

import com.example.authtutorial.common.domain.DomainException;
import com.example.authtutorial.common.domain.Subject;

import java.time.Instant;

/**
 * 值物件：存取權杖 (Access Token)。
 *
 * <p>對應圖中 OAuth 流程的「Gets an access token」與「Calls App B's API with token」。
 * 重點：權杖承載的是「<b>權限 (scopes)</b>」，用來控制 App「能存取什麼」，
 * 而<b>不是</b>用來證明使用者身分（那是 OIDC 的 ID Token 的工作）。</p>
 */
public record AccessToken(
        String value,
        Subject resourceOwner,
        ClientId client,
        Scopes scopes,
        Instant issuedAt,
        Instant expiresAt) {

    public AccessToken {
        if (value == null || value.isBlank()) {
            throw new DomainException("Access token 值不可為空白");
        }
        if (resourceOwner == null || client == null || scopes == null) {
            throw new DomainException("Access token 缺少必要欄位");
        }
        if (expiresAt == null || issuedAt == null || !expiresAt.isAfter(issuedAt)) {
            throw new DomainException("Access token 的到期時間必須晚於發行時間");
        }
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    /**
     * 此權杖是否足以存取「需要某個 scope」的資源。
     * 需同時滿足：尚未過期，且持有該 scope。
     */
    public boolean authorizes(Scope requiredScope, Instant now) {
        return !isExpired(now) && scopes.contains(requiredScope);
    }
}
