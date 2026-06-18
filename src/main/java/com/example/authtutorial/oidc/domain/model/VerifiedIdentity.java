package com.example.authtutorial.oidc.domain.model;

import com.example.authtutorial.common.domain.DomainException;
import com.example.authtutorial.common.domain.Subject;

/**
 * 值物件：通過驗證後萃取出的「使用者身分」。
 *
 * <p>這就是 OIDC 相較於純 OAuth 的關鍵差異 —— OIDC 回答「<b>你是誰</b>」，
 * 而不只是「你能存取什麼」。</p>
 */
public record VerifiedIdentity(Subject subject, String email, String issuer) {

    public VerifiedIdentity {
        if (subject == null) {
            throw new DomainException("已驗證身分必須包含 subject");
        }
        if (issuer == null || issuer.isBlank()) {
            throw new DomainException("已驗證身分必須包含 issuer");
        }
    }
}
