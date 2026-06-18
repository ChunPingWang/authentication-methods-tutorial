package com.example.authtutorial.oidc.integration;

import com.example.authtutorial.oidc.adapter.out.JwtDecoderIdTokenParser;
import com.example.authtutorial.oidc.application.VerifyIdentityService;
import com.example.authtutorial.oidc.domain.model.VerifiedIdentity;
import com.example.authtutorial.oidc.domain.port.in.VerifyIdentityUseCase;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 整合測試：用「真實的 Keycloak」當作 IdP，端對端驗證 OIDC ID Token。
 *
 * <p>流程：
 * <ol>
 *   <li>以 Testcontainers 啟動 Keycloak，匯入 {@code tutorial} realm（含使用者 alice）。</li>
 *   <li>用 Resource Owner Password 流程向 Keycloak 換取真正的 ID Token。</li>
 *   <li>用 Keycloak 的 JWKS（公鑰）建立 {@link JwtDecoder}，包進我們的 domain 埠。</li>
 *   <li>呼叫 {@link VerifyIdentityService} 驗證 token 並萃取身分，斷言 subject/email。</li>
 * </ol>
 *
 * <p><b>執行方式：</b>{@code mvn verify -Pintegration}（需要可用的 Docker，
 * 且能存取 Keycloak 映像檔來源 quay.io）。預設的 {@code mvn test} 不會跑此測試。</p>
 */
@Tag("integration")
@Testcontainers
@DisplayName("OIDC 整合測試（真實 Keycloak）")
class KeycloakOidcVerificationIT {

    private static final String REALM = "tutorial";
    private static final String CLIENT_ID = "tutorial-app";

    @Container
    static final KeycloakContainer KEYCLOAK = new KeycloakContainer("quay.io/keycloak/keycloak:26.0")
            .withRealmImportFile("keycloak/tutorial-realm.json");

    @Test
    @DisplayName("Keycloak 簽發的 ID Token → 通過我們的驗證並取得身分")
    void verifiesRealKeycloakIdToken() throws Exception {
        String issuer = KEYCLOAK.getAuthServerUrl() + "/realms/" + REALM;
        String jwksUri = issuer + "/protocol/openid-connect/certs";

        // 1) 向 Keycloak 換取真實 ID Token。
        String idToken = obtainIdToken(issuer);

        // 2) 以 Keycloak 公鑰建立只驗簽章的解碼器（時間/受眾/nonce 交給 domain）。
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwksUri).build();
        decoder.setJwtValidator(token -> OAuth2TokenValidatorResult.success());

        VerifyIdentityUseCase oidc = new VerifyIdentityService(
                new JwtDecoderIdTokenParser(decoder), Clock.systemUTC(), issuer);

        // 3) 驗證並萃取身分（Keycloak 的 ID Token 的 aud = client id）。
        VerifiedIdentity identity = oidc.verify(
                new VerifyIdentityUseCase.VerifyIdTokenCommand(idToken, CLIENT_ID, null));

        assertThat(identity.subject().value()).isNotBlank();
        assertThat(identity.email()).isEqualTo("alice@example.com");
        assertThat(identity.issuer()).isEqualTo(issuer);
    }

    /** 以 Resource Owner Password 流程取得 ID Token。 */
    private String obtainIdToken(String issuer) throws Exception {
        Map<String, String> form = Map.of(
                "grant_type", "password",
                "client_id", CLIENT_ID,
                "username", "alice",
                "password", "wonderland",
                "scope", "openid email profile");
        String body = form.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(issuer + "/protocol/openid-connect/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);

        JsonNode json = JsonMapper.builder().build().readTree(response.body());
        return json.get("id_token").asString();
    }
}
