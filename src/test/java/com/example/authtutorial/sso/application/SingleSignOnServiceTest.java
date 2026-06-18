package com.example.authtutorial.sso.application;

import com.example.authtutorial.common.domain.DomainException;
import com.example.authtutorial.sso.adapter.out.DemoIdentityProviderAdapter;
import com.example.authtutorial.sso.adapter.out.InMemorySsoSessionRepository;
import com.example.authtutorial.sso.domain.model.SessionId;
import com.example.authtutorial.sso.domain.model.SsoSession;
import com.example.authtutorial.sso.domain.port.in.SingleSignOnUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 應用服務 {@link SingleSignOnService} 的測試。
 *
 * <p>採用「真實的記憶體 adapter + 固定時鐘」而非 mock，示範六角形架構如何
 * 讓 application 層能在<b>不啟動 Spring</b> 的情況下被完整測試（毫秒級）。</p>
 */
@DisplayName("SingleSignOnService 應用服務")
class SingleSignOnServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-18T10:00:00Z");

    private SingleSignOnUseCase sso;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(NOW, ZoneOffset.UTC);
        sso = new SingleSignOnService(
                new DemoIdentityProviderAdapter(),
                new InMemorySsoSessionRepository(),
                fixedClock,
                Duration.ofMinutes(30));
    }

    @Test
    @DisplayName("正確帳密 → 建立工作階段（登入一次）")
    void establishesSessionWithValidCredentials() {
        SsoSession session = sso.establishSession(
                new SingleSignOnUseCase.EstablishSessionCommand("alice", "wonderland"));

        assertThat(session.subject().value()).isEqualTo("alice");
        assertThat(session.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(30)));
    }

    @Test
    @DisplayName("錯誤帳密 → 認證失敗")
    void rejectsInvalidCredentials() {
        assertThatThrownBy(() -> sso.establishSession(
                new SingleSignOnUseCase.EstablishSessionCommand("alice", "wrong")))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("認證失敗");
    }

    @Test
    @DisplayName("用既有工作階段存取多個 App，全程不需再次提供密碼（SSO 精髓）")
    void accessesMultipleAppsWithoutReauthentication() {
        SsoSession session = sso.establishSession(
                new SingleSignOnUseCase.EstablishSessionCommand("alice", "wonderland"));
        SessionId id = session.id();

        sso.accessApplication(new SingleSignOnUseCase.AccessApplicationCommand(id, "gmail"));
        SsoSession after = sso.accessApplication(
                new SingleSignOnUseCase.AccessApplicationCommand(id, "calendar"));

        assertThat(after.accessedApplications()).containsExactlyInAnyOrder("gmail", "calendar");
    }

    @Test
    @DisplayName("不存在的工作階段 → 要求重新登入")
    void rejectsUnknownSession() {
        assertThatThrownBy(() -> sso.accessApplication(
                new SingleSignOnUseCase.AccessApplicationCommand(SessionId.of("does-not-exist"), "gmail")))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("找不到工作階段");
    }
}
