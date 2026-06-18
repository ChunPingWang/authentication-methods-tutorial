package com.example.authtutorial.saml.application;

import com.example.authtutorial.common.domain.DomainException;
import com.example.authtutorial.saml.adapter.out.DemoSamlCodec;
import com.example.authtutorial.saml.domain.model.AssertedIdentity;
import com.example.authtutorial.saml.domain.model.SamlAssertion;
import com.example.authtutorial.saml.domain.port.in.ValidateAssertionUseCase;
import com.example.authtutorial.saml.domain.port.in.ValidateAssertionUseCase.ValidateAssertionCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 應用服務 {@link ValidateAssertionService} 測試：走「簽章驗證 + Conditions 驗證」
 * 完整 SAML 流程。
 */
@DisplayName("ValidateAssertionService 應用服務（SAML）")
class ValidateAssertionServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-18T10:00:00Z");
    private static final String ISSUER = "https://idp.tutorial.local";
    private static final String AUDIENCE = "https://sp.tutorial.local";
    private static final String SECRET = "shared-secret";

    private final DemoSamlCodec idpCodec = new DemoSamlCodec(SECRET);
    private ValidateAssertionUseCase saml;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        saml = new ValidateAssertionService(idpCodec::verifyAndParse, clock, ISSUER);
    }

    private String issuedAssertion(Instant notBefore, Instant notOnOrAfter, String audience) {
        return idpCodec.sign(new SamlAssertion(ISSUER, "alice", audience,
                notBefore, notOnOrAfter, Map.of("email", "alice@example.com")));
    }

    @Test
    @DisplayName("有效且正確簽署的斷言 → 驗證成功並萃取身分")
    void validatesGoodAssertion() {
        String assertion = issuedAssertion(NOW.minusSeconds(60), NOW.plusSeconds(300), AUDIENCE);

        AssertedIdentity identity = saml.validate(new ValidateAssertionCommand(assertion, AUDIENCE));

        assertThat(identity.subject().value()).isEqualTo("alice");
        assertThat(identity.attributes()).containsEntry("email", "alice@example.com");
    }

    @Test
    @DisplayName("簽章有效但已過期 → 由 domain 攔下")
    void rejectsExpiredAssertion() {
        String assertion = issuedAssertion(NOW.minusSeconds(600), NOW.minusSeconds(1), AUDIENCE);

        assertThatThrownBy(() -> saml.validate(new ValidateAssertionCommand(assertion, AUDIENCE)))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("過期");
    }

    @Test
    @DisplayName("受眾不符 → 拒絕")
    void rejectsWrongAudience() {
        String assertion = issuedAssertion(NOW.minusSeconds(60), NOW.plusSeconds(300), "https://evil-sp.example.com");

        assertThatThrownBy(() -> saml.validate(new ValidateAssertionCommand(assertion, AUDIENCE)))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("受眾");
    }
}
