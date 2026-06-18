package com.example.authtutorial.oidc.domain.model;

import com.example.authtutorial.common.domain.DomainException;
import com.example.authtutorial.common.domain.Subject;

import java.time.Instant;
import java.util.List;

/**
 * 值物件：ID Token（JWT）內的宣告 (claims)。
 *
 * <p>對應圖中 OIDC 流程「IdP issues ID token」之後、App「validates ID token and
 * extracts user identity」之前的資料。簽章驗證由<b>被驅動端埠</b>完成；
 * 至於 claims 是否合法（過期、發行者、受眾、nonce）則是<b>domain 的業務規則</b>。</p>
 *
 * @param issuer    發行者 (iss)，必須是我們信任的 IdP。
 * @param subject   主體 (sub)，使用者的唯一識別碼。
 * @param audience  受眾 (aud)，這個 token 是發給哪些 App 的。
 * @param issuedAt  發行時間 (iat)。
 * @param expiresAt 到期時間 (exp)。
 * @param nonce     防重放的隨機值（可為 null）。
 * @param email     使用者 email（可為 null）。
 */
public record IdTokenClaims(
        String issuer,
        String subject,
        List<String> audience,
        Instant issuedAt,
        Instant expiresAt,
        String nonce,
        String email) {

    public IdTokenClaims {
        if (issuer == null || issuer.isBlank()) {
            throw new DomainException("ID Token 缺少 issuer (iss)");
        }
        if (subject == null || subject.isBlank()) {
            throw new DomainException("ID Token 缺少 subject (sub)");
        }
        if (audience == null || audience.isEmpty()) {
            throw new DomainException("ID Token 缺少 audience (aud)");
        }
        if (expiresAt == null) {
            throw new DomainException("ID Token 缺少 expiration (exp)");
        }
        audience = List.copyOf(audience);
    }

    /**
     * 驗證 claims 並萃取出已驗證的身分。
     *
     * @param now              現在時間。
     * @param expectedIssuer   我們信任的發行者。
     * @param expectedAudience 必須包含的受眾（通常是本 App 的 client id）。
     * @param expectedNonce    若不為 null，必須與 token 內的 nonce 相符。
     * @throws DomainException 任一項驗證失敗。
     */
    public VerifiedIdentity validate(Instant now, String expectedIssuer,
                                     String expectedAudience, String expectedNonce) {
        if (!now.isBefore(expiresAt)) {
            throw new DomainException("ID Token 已過期");
        }
        if (!issuer.equals(expectedIssuer)) {
            throw new DomainException("ID Token 的發行者不受信任: " + issuer);
        }
        if (!audience.contains(expectedAudience)) {
            throw new DomainException("ID Token 的受眾不符，未包含: " + expectedAudience);
        }
        if (expectedNonce != null && !expectedNonce.equals(nonce)) {
            throw new DomainException("ID Token 的 nonce 不符（可能是重放攻擊）");
        }
        return new VerifiedIdentity(Subject.of(subject), email, issuer);
    }
}
