package com.example.authtutorial.saml.domain.model;

import com.example.authtutorial.common.domain.DomainException;

import java.time.Instant;
import java.util.Map;

/**
 * 聚合根：SAML 斷言 (Assertion)。
 *
 * <p>對應圖中 SAML 流程「IdP creates SAML assertion (XML)」。簽章驗證由
 * 被驅動端埠完成；本聚合負責「條件 (Conditions)」的業務規則：
 * <ul>
 *   <li>時間有效區間：notBefore ≤ now &lt; notOnOrAfter。</li>
 *   <li>發行者 (Issuer) 必須受信任。</li>
 *   <li>受眾 (Audience) 必須是本服務 (SP)。</li>
 * </ul>
 *
 * @param issuer        發行此斷言的 IdP。
 * @param subject       斷言所描述的使用者。
 * @param audience      預期接收者 (Service Provider)。
 * @param notBefore     生效時間。
 * @param notOnOrAfter  失效時間（到此刻即失效）。
 * @param attributes    使用者屬性（如 email、role）。
 */
public record SamlAssertion(
        String issuer,
        String subject,
        String audience,
        Instant notBefore,
        Instant notOnOrAfter,
        Map<String, String> attributes) {

    public SamlAssertion {
        if (issuer == null || issuer.isBlank()) {
            throw new DomainException("SAML 斷言缺少 Issuer");
        }
        if (subject == null || subject.isBlank()) {
            throw new DomainException("SAML 斷言缺少 Subject");
        }
        if (audience == null || audience.isBlank()) {
            throw new DomainException("SAML 斷言缺少 Audience");
        }
        if (notBefore == null || notOnOrAfter == null || !notOnOrAfter.isAfter(notBefore)) {
            throw new DomainException("SAML 斷言的時間區間無效");
        }
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    /**
     * 驗證條件並萃取出已斷言的身分。
     *
     * @throws DomainException 任一條件不符。
     */
    public AssertedIdentity validate(Instant now, String expectedIssuer, String expectedAudience) {
        if (now.isBefore(notBefore)) {
            throw new DomainException("SAML 斷言尚未生效");
        }
        if (!now.isBefore(notOnOrAfter)) {
            throw new DomainException("SAML 斷言已過期");
        }
        if (!issuer.equals(expectedIssuer)) {
            throw new DomainException("SAML 斷言的發行者不受信任: " + issuer);
        }
        if (!audience.equals(expectedAudience)) {
            throw new DomainException("SAML 斷言的受眾不符: " + audience);
        }
        return new AssertedIdentity(subject, issuer, attributes);
    }
}
