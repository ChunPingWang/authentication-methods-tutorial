package com.example.authtutorial.oidc.application;

import com.example.authtutorial.common.domain.DomainException;
import com.example.authtutorial.oidc.adapter.out.JwtDecoderIdTokenParser;
import com.example.authtutorial.oidc.domain.model.VerifiedIdentity;
import com.example.authtutorial.oidc.domain.port.in.VerifyIdentityUseCase;
import com.example.authtutorial.oidc.domain.port.in.VerifyIdentityUseCase.VerifyIdTokenCommand;
import com.example.authtutorial.oidc.testkit.TestIdTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 應用服務 {@link VerifyIdentityService} 測試：走「真實 JWT 簽章驗證 + domain claims
 * 驗證」的完整 OIDC 流程，但完全離線（HMAC 共享密鑰，不需 Keycloak）。
 */
@DisplayName("VerifyIdentityService 應用服務（OIDC）")
class VerifyIdentityServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-18T10:00:00Z");

    private VerifyIdentityUseCase oidc;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        var parser = new JwtDecoderIdTokenParser(TestIdTokens.hmacDecoder());
        oidc = new VerifyIdentityService(parser, clock, TestIdTokens.ISSUER);
    }

    @Test
    @DisplayName("有效的已簽章 ID Token → 驗證成功並萃取身分")
    void verifiesValidToken() {
        String token = TestIdTokens.signed(TestIdTokens.ISSUER, "alice", TestIdTokens.AUDIENCE,
                NOW.minusSeconds(30), NOW.plusSeconds(300), "n-1", "alice@example.com");

        VerifiedIdentity identity = oidc.verify(
                new VerifyIdTokenCommand(token, TestIdTokens.AUDIENCE, "n-1"));

        assertThat(identity.subject().value()).isEqualTo("alice");
        assertThat(identity.email()).isEqualTo("alice@example.com");
    }

    @Test
    @DisplayName("被竄改／錯誤密鑰簽署的 token → 簽章驗證失敗")
    void rejectsBadSignature() {
        String forged = TestIdTokens.sign("an-attacker-secret-key-not-the-real!!", TestIdTokens.ISSUER,
                "alice", TestIdTokens.AUDIENCE, NOW.minusSeconds(30), NOW.plusSeconds(300), null, null);

        assertThatThrownBy(() -> oidc.verify(new VerifyIdTokenCommand(forged, TestIdTokens.AUDIENCE, null)))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("簽章");
    }

    @Test
    @DisplayName("簽章正確但已過期 → 由 domain 攔下")
    void rejectsExpiredEvenIfSignatureValid() {
        String expired = TestIdTokens.signed(TestIdTokens.ISSUER, "alice", TestIdTokens.AUDIENCE,
                NOW.minusSeconds(600), NOW.minusSeconds(300), null, null);

        assertThatThrownBy(() -> oidc.verify(new VerifyIdTokenCommand(expired, TestIdTokens.AUDIENCE, null)))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("過期");
    }
}
