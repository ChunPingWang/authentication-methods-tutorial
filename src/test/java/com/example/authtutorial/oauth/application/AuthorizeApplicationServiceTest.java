package com.example.authtutorial.oauth.application;

import com.example.authtutorial.common.domain.DomainException;
import com.example.authtutorial.oauth.adapter.out.InMemoryAccessTokenAdapter;
import com.example.authtutorial.oauth.domain.model.AccessToken;
import com.example.authtutorial.oauth.domain.port.in.AuthorizeApplicationUseCase;
import com.example.authtutorial.oauth.domain.port.in.AuthorizeApplicationUseCase.AuthorizationCommand;
import com.example.authtutorial.oauth.domain.port.in.AuthorizeApplicationUseCase.ResourceAccessResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 應用服務 {@link AuthorizeApplicationService} 測試：示範 OAuth「授權 → 拿 token →
 * 以 token 存取資源」的完整流程，以及「token 控制的是權限而非身分」。
 */
@DisplayName("AuthorizeApplicationService 應用服務")
class AuthorizeApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-18T10:00:00Z");

    private AuthorizeApplicationUseCase oauth;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        InMemoryAccessTokenAdapter tokenAdapter = new InMemoryAccessTokenAdapter(clock);
        oauth = new AuthorizeApplicationService(tokenAdapter, tokenAdapter, clock);
    }

    private AccessToken authorizeReadOnly() {
        return oauth.authorize(new AuthorizationCommand(
                "app-A", "alice",
                Set.of("contacts.read", "contacts.write"),
                Set.of("contacts.read"),
                true));
    }

    @Test
    @DisplayName("使用者同意後 → 核發含核准 scope 的存取權杖")
    void issuesTokenWithApprovedScopes() {
        AccessToken token = authorizeReadOnly();

        assertThat(token.value()).startsWith("at_");
        assertThat(token.scopes().asStringSet()).containsExactly("contacts.read");
    }

    @Test
    @DisplayName("未同意 → 不核發權杖")
    void deniesWithoutConsent() {
        assertThatThrownBy(() -> oauth.authorize(new AuthorizationCommand(
                "app-A", "alice", Set.of("contacts.read"), Set.of("contacts.read"), false)))
                .isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("持有正確 scope 的權杖 → 可存取資源")
    void grantsResourceAccessWithMatchingScope() {
        AccessToken token = authorizeReadOnly();

        ResourceAccessResult result = oauth.accessResource(token.value(), "contacts.read");

        assertThat(result.granted()).isTrue();
        assertThat(result.subject()).isEqualTo("alice");
    }

    @Test
    @DisplayName("缺少所需 scope → 拒絕存取（控制的是「能做什麼」）")
    void deniesResourceAccessWhenScopeMissing() {
        AccessToken token = authorizeReadOnly();

        ResourceAccessResult result = oauth.accessResource(token.value(), "contacts.write");

        assertThat(result.granted()).isFalse();
        assertThat(result.reason()).contains("scope");
    }

    @Test
    @DisplayName("無效權杖 → 拒絕存取")
    void deniesUnknownToken() {
        ResourceAccessResult result = oauth.accessResource("at_does_not_exist", "contacts.read");

        assertThat(result.granted()).isFalse();
        assertThat(result.reason()).contains("無效");
    }
}
