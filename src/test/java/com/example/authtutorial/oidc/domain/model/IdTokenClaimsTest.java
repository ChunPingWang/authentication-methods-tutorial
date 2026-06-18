package com.example.authtutorial.oidc.domain.model;

import com.example.authtutorial.common.domain.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 值物件 {@link IdTokenClaims} 測試：OIDC 的 claims 業務驗證
 * （過期 / 發行者 / 受眾 / nonce），這些都在簽章驗證「之後」執行。
 */
@DisplayName("IdTokenClaims 值物件（claims 驗證）")
class IdTokenClaimsTest {

    private static final Instant NOW = Instant.parse("2026-06-18T10:00:00Z");
    private static final String ISSUER = "https://idp.tutorial.local";
    private static final String AUDIENCE = "tutorial-app";

    private IdTokenClaims claims(Instant exp, String nonce) {
        return new IdTokenClaims(ISSUER, "alice", List.of(AUDIENCE),
                NOW.minusSeconds(60), exp, nonce, "alice@example.com");
    }

    @Test
    @DisplayName("全部條件符合 → 萃取出已驗證身分（含 email）")
    void validatesAndExtractsIdentity() {
        VerifiedIdentity identity = claims(NOW.plusSeconds(300), "n-123")
                .validate(NOW, ISSUER, AUDIENCE, "n-123");

        assertThat(identity.subject().value()).isEqualTo("alice");
        assertThat(identity.email()).isEqualTo("alice@example.com");
        assertThat(identity.issuer()).isEqualTo(ISSUER);
    }

    @Test
    @DisplayName("已過期 → 拒絕")
    void rejectsExpired() {
        assertThatThrownBy(() -> claims(NOW.minusSeconds(1), null).validate(NOW, ISSUER, AUDIENCE, null))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("過期");
    }

    @Test
    @DisplayName("發行者不受信任 → 拒絕")
    void rejectsUntrustedIssuer() {
        assertThatThrownBy(() -> claims(NOW.plusSeconds(300), null)
                .validate(NOW, "https://evil.example.com", AUDIENCE, null))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("發行者");
    }

    @Test
    @DisplayName("受眾不符 → 拒絕")
    void rejectsWrongAudience() {
        assertThatThrownBy(() -> claims(NOW.plusSeconds(300), null)
                .validate(NOW, ISSUER, "another-app", null))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("受眾");
    }

    @Test
    @DisplayName("nonce 不符 → 拒絕（防重放）")
    void rejectsNonceMismatch() {
        assertThatThrownBy(() -> claims(NOW.plusSeconds(300), "expected")
                .validate(NOW, ISSUER, AUDIENCE, "different"))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("nonce");
    }
}
