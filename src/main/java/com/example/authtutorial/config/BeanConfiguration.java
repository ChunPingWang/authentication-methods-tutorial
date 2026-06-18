package com.example.authtutorial.config;

import com.example.authtutorial.oauth.application.AuthorizeApplicationService;
import com.example.authtutorial.oauth.domain.port.in.AuthorizeApplicationUseCase;
import com.example.authtutorial.oauth.domain.port.out.AccessTokenIntrospectionPort;
import com.example.authtutorial.oauth.domain.port.out.AccessTokenIssuerPort;
import com.example.authtutorial.oidc.adapter.out.JwtDecoderIdTokenParser;
import com.example.authtutorial.oidc.application.VerifyIdentityService;
import com.example.authtutorial.oidc.domain.port.in.VerifyIdentityUseCase;
import com.example.authtutorial.oidc.domain.port.out.IdTokenParserPort;
import com.example.authtutorial.saml.application.ValidateAssertionService;
import com.example.authtutorial.saml.domain.port.in.ValidateAssertionUseCase;
import com.example.authtutorial.saml.domain.port.out.AssertionSignatureVerifierPort;
import com.example.authtutorial.sso.application.SingleSignOnService;
import com.example.authtutorial.sso.domain.port.in.SingleSignOnUseCase;
import com.example.authtutorial.sso.domain.port.out.IdentityProviderPort;
import com.example.authtutorial.sso.domain.port.out.SsoSessionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;

/**
 * 組合根 (Composition Root)：六角形架構唯一「認得所有層」的地方。
 *
 * <p>application 層的服務刻意寫成「純 Java、用建構子收埠」，因此這裡負責把
 * domain 埠與具體 adapter 串起來，成為 Spring bean。這保持了 domain/application
 * 對框架的「零依賴」，同時讓 Spring 處理依賴注入（DIP 的實踐）。</p>
 */
@Configuration
public class BeanConfiguration {

    /** 統一的時鐘來源 —— 注入到各服務，讓測試能用固定時間。 */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    // ---------------------------------------------------------------- SSO

    @Bean
    public SingleSignOnUseCase singleSignOnUseCase(IdentityProviderPort identityProvider,
                                                   SsoSessionRepository sessionRepository,
                                                   Clock clock,
                                                   @Value("${sso.session-ttl:PT30M}") Duration ttl) {
        return new SingleSignOnService(identityProvider, sessionRepository, clock, ttl);
    }

    // -------------------------------------------------------------- OAuth

    @Bean
    public AuthorizeApplicationUseCase authorizeApplicationUseCase(AccessTokenIssuerPort issuer,
                                                                   AccessTokenIntrospectionPort introspection,
                                                                   Clock clock) {
        return new AuthorizeApplicationService(issuer, introspection, clock);
    }

    // --------------------------------------------------------------- OIDC

    /**
     * 教學用的 ID Token 解碼器：以 HMAC (HS256) 共享密鑰驗章，完全離線即可運作。
     *
     * <p>關鍵：刻意關閉 Nimbus 預設的「時間驗證」，把 exp/aud/nonce 的判斷
     * 全部交還給 domain（{@code IdTokenClaims.validate}）—— 這樣業務規則才能被
     * 獨立、清楚地測試。正式環境會改用以 IdP JWKS 公鑰建立的解碼器。</p>
     */
    @Bean
    public JwtDecoder idTokenJwtDecoder(@Value("${oidc.hmac-secret:tutorial-oidc-hmac-secret-key-32bytes!!}") String secret) {
        SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        // 只驗簽章，時間/受眾/nonce 留給 domain。
        decoder.setJwtValidator(token -> OAuth2TokenValidatorResult.success());
        return decoder;
    }

    @Bean
    public IdTokenParserPort idTokenParserPort(JwtDecoder idTokenJwtDecoder) {
        return new JwtDecoderIdTokenParser(idTokenJwtDecoder);
    }

    @Bean
    public VerifyIdentityUseCase verifyIdentityUseCase(IdTokenParserPort parser,
                                                       Clock clock,
                                                       @Value("${oidc.trusted-issuer:https://idp.tutorial.local}") String trustedIssuer) {
        return new VerifyIdentityService(parser, clock, trustedIssuer);
    }

    // --------------------------------------------------------------- SAML

    @Bean
    public ValidateAssertionUseCase validateAssertionUseCase(AssertionSignatureVerifierPort verifier,
                                                             Clock clock,
                                                             @Value("${saml.trusted-issuer:https://idp.tutorial.local}") String trustedIssuer) {
        return new ValidateAssertionService(verifier, clock, trustedIssuer);
    }
}
