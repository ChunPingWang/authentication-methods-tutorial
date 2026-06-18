package com.example.authtutorial.saml.domain.model;

import com.example.authtutorial.common.domain.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 聚合根 {@link SamlAssertion} 測試：SAML 的 Conditions 驗證
 * （時間區間 / 發行者 / 受眾），在簽章驗證之後執行。
 */
@DisplayName("SamlAssertion 聚合根（Conditions 驗證）")
class SamlAssertionTest {

    private static final Instant NOW = Instant.parse("2026-06-18T10:00:00Z");
    private static final String ISSUER = "https://idp.tutorial.local";
    private static final String AUDIENCE = "https://sp.tutorial.local";

    private SamlAssertion assertion(Instant notBefore, Instant notOnOrAfter) {
        return new SamlAssertion(ISSUER, "alice", AUDIENCE, notBefore, notOnOrAfter,
                Map.of("email", "alice@example.com", "role", "admin"));
    }

    @Test
    @DisplayName("條件全部符合 → 萃取身分與屬性")
    void validatesAndExtractsIdentityWithAttributes() {
        var identity = assertion(NOW.minusSeconds(60), NOW.plusSeconds(300))
                .validate(NOW, ISSUER, AUDIENCE);

        assertThat(identity.subject().value()).isEqualTo("alice");
        assertThat(identity.attributes()).containsEntry("role", "admin");
    }

    @Test
    @DisplayName("尚未生效 (now < notBefore) → 拒絕")
    void rejectsNotYetValid() {
        assertThatThrownBy(() -> assertion(NOW.plusSeconds(60), NOW.plusSeconds(300))
                .validate(NOW, ISSUER, AUDIENCE))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("尚未生效");
    }

    @Test
    @DisplayName("已過期 (now >= notOnOrAfter) → 拒絕")
    void rejectsExpired() {
        assertThatThrownBy(() -> assertion(NOW.minusSeconds(600), NOW.minusSeconds(1))
                .validate(NOW, ISSUER, AUDIENCE))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("過期");
    }

    @Test
    @DisplayName("受眾不符 → 拒絕")
    void rejectsWrongAudience() {
        assertThatThrownBy(() -> assertion(NOW.minusSeconds(60), NOW.plusSeconds(300))
                .validate(NOW, ISSUER, "https://other-sp.example.com"))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("受眾");
    }
}
