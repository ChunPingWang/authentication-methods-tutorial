package com.example.authtutorial.web;

import com.example.authtutorial.oidc.testkit.TestIdTokens;
import com.example.authtutorial.saml.adapter.out.DemoSamlCodec;
import com.example.authtutorial.saml.domain.model.SamlAssertion;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 端對端 Web 測試：啟動完整 Spring 應用程式（真實的 domain + adapter 接線），
 * 透過 HTTP/MockMvc 驗證四種流程的「快樂路徑」。
 *
 * <p>這同時也是「context 能否成功載入」的冒煙測試 (smoke test)。
 * 不需要 Docker，因此屬於預設就會跑的測試。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("四種認證流程的端對端 Web 測試")
class AuthenticationFlowsWebTest {

    @Autowired
    MockMvc mockMvc;

    // ------------------------------------------------------------------ SSO

    @Test
    @DisplayName("SSO：登入一次後，免再次登入即可存取多個 App")
    void ssoLoginOnceAccessMany() throws Exception {
        String loginBody = """
                {"username":"alice","password":"wonderland"}
                """;

        String response = mockMvc.perform(post("/api/sso/login")
                        .contentType(MediaType.APPLICATION_JSON).content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("alice"))
                .andReturn().getResponse().getContentAsString();

        String sessionId = JsonPath.read(response, "$.sessionId");

        mockMvc.perform(post("/api/sso/sessions/{id}/access", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"applicationId\":\"gmail\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessedApplications[0]").value("gmail"));
    }

    @Test
    @DisplayName("SSO：錯誤密碼 → 400")
    void ssoWrongPassword() throws Exception {
        mockMvc.perform(post("/api/sso/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"nope\"}"))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------- OAuth

    @Test
    @DisplayName("OAuth：同意後取得 token，並用 token 存取需要對應 scope 的資源")
    void oauthAuthorizeThenAccessResource() throws Exception {
        String authorizeBody = """
                {
                  "clientId":"app-A",
                  "resourceOwner":"alice",
                  "requestedScopes":["contacts.read","contacts.write"],
                  "approvedScopes":["contacts.read"],
                  "userConsented":true
                }
                """;

        String response = mockMvc.perform(post("/api/oauth/authorize")
                        .contentType(MediaType.APPLICATION_JSON).content(authorizeBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scopes[0]").value("contacts.read"))
                .andReturn().getResponse().getContentAsString();

        String token = JsonPath.read(response, "$.accessToken");

        mockMvc.perform(post("/api/oauth/resource")
                        .param("token", token).param("requiredScope", "contacts.read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.granted").value(true))
                .andExpect(jsonPath("$.subject").value("alice"));

        // 沒有 write 權限 → 被拒。
        mockMvc.perform(post("/api/oauth/resource")
                        .param("token", token).param("requiredScope", "contacts.write"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.granted").value(false));
    }

    // ----------------------------------------------------------------- OIDC

    @Test
    @DisplayName("OIDC：以有效 ID Token 驗證並萃取身分")
    void oidcVerifyIdToken() throws Exception {
        Instant now = Instant.now();
        String idToken = TestIdTokens.signed(TestIdTokens.ISSUER, "alice", TestIdTokens.AUDIENCE,
                now.minus(1, ChronoUnit.MINUTES), now.plus(5, ChronoUnit.MINUTES), "n-1", "alice@example.com");

        String body = "{\"idToken\":\"" + idToken + "\",\"audience\":\"" + TestIdTokens.AUDIENCE
                + "\",\"nonce\":\"n-1\"}";

        mockMvc.perform(post("/api/oidc/verify")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"));
    }

    // ----------------------------------------------------------------- SAML

    @Test
    @DisplayName("SAML：以有效簽章斷言驗證並萃取身分與屬性")
    void samlValidateAssertion() throws Exception {
        Instant now = Instant.now();
        String audience = "https://sp.tutorial.local";
        // 用與應用程式預設相同的密鑰扮演 IdP 簽發斷言。
        DemoSamlCodec idp = new DemoSamlCodec("tutorial-saml-shared-secret");
        String assertion = idp.sign(new SamlAssertion(
                "https://idp.tutorial.local", "alice", audience,
                now.minus(1, ChronoUnit.MINUTES), now.plus(5, ChronoUnit.MINUTES),
                Map.of("role", "admin")));

        String body = "{\"assertion\":\"" + assertion + "\",\"audience\":\"" + audience + "\"}";

        mockMvc.perform(post("/api/saml/validate")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("alice"))
                .andExpect(jsonPath("$.attributes.role").value("admin"));
    }
}
