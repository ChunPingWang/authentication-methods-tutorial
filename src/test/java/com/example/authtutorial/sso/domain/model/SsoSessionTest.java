package com.example.authtutorial.sso.domain.model;

import com.example.authtutorial.common.domain.DomainException;
import com.example.authtutorial.common.domain.Subject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 聚合根 {@link SsoSession} 的單元測試 —— 驗證「SSO 是體驗」的核心規則：
 * 登入一次，便能在工作階段有效期間存取多個 App。
 */
@DisplayName("SsoSession 聚合根")
class SsoSessionTest {

    private static final Instant NOW = Instant.parse("2026-06-18T10:00:00Z");
    private static final Subject ALICE = Subject.of("alice");

    @Test
    @DisplayName("建立後，可在有效期間內存取多個 App（免再次登入）")
    void canAccessMultipleApplicationsWithinTtl() {
        SsoSession session = SsoSession.start(ALICE, NOW, Duration.ofMinutes(30));

        session.accessApplication("app-A", NOW.plusSeconds(60));
        session.accessApplication("app-B", NOW.plusSeconds(120));

        assertThat(session.hasAccessed("app-A")).isTrue();
        assertThat(session.hasAccessed("app-B")).isTrue();
        assertThat(session.accessedApplications()).containsExactly("app-A", "app-B");
    }

    @Test
    @DisplayName("工作階段過期後，存取任何 App 都會被拒絕")
    void rejectsAccessAfterExpiry() {
        SsoSession session = SsoSession.start(ALICE, NOW, Duration.ofMinutes(30));

        Instant afterExpiry = NOW.plus(Duration.ofMinutes(31));

        assertThat(session.isExpired(afterExpiry)).isTrue();
        assertThatThrownBy(() -> session.accessApplication("app-A", afterExpiry))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("已過期");
    }

    @Test
    @DisplayName("到期時間「當下」即視為過期（邊界條件）")
    void expiresExactlyAtBoundary() {
        SsoSession session = SsoSession.start(ALICE, NOW, Duration.ofMinutes(30));

        assertThat(session.isExpired(NOW.plus(Duration.ofMinutes(30)))).isTrue();
    }

    @Test
    @DisplayName("存活時間必須為正值")
    void rejectsNonPositiveTtl() {
        assertThatThrownBy(() -> SsoSession.start(ALICE, NOW, Duration.ZERO))
                .isInstanceOf(DomainException.class);
    }
}
