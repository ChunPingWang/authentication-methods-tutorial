package com.example.authtutorial.oauth.domain.model;

import com.example.authtutorial.common.domain.DomainException;
import com.example.authtutorial.common.domain.Subject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AccessToken 值物件")
class AccessTokenTest {

    private static final Instant NOW = Instant.parse("2026-06-18T10:00:00Z");

    private AccessToken token(Instant issuedAt, Duration ttl) {
        return new AccessToken("at_x", Subject.of("alice"), ClientId.of("app-A"),
                Scopes.fromStrings(Set.of("contacts.read")), issuedAt, issuedAt.plus(ttl));
    }

    @Test
    @DisplayName("到期時間必須晚於發行時間")
    void rejectsInvalidValidityWindow() {
        assertThatThrownBy(() -> new AccessToken("at_x", Subject.of("alice"), ClientId.of("app-A"),
                Scopes.fromStrings(Set.of("contacts.read")), NOW, NOW))
                .isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("未過期且持有 scope → authorizes 為真")
    void authorizesWhenValidAndHasScope() {
        AccessToken token = token(NOW, Duration.ofHours(1));

        assertThat(token.authorizes(Scope.of("contacts.read"), NOW.plusSeconds(60))).isTrue();
    }

    @Test
    @DisplayName("已過期 → 即使持有 scope 也不授權")
    void deniesWhenExpired() {
        AccessToken token = token(NOW, Duration.ofHours(1));

        assertThat(token.authorizes(Scope.of("contacts.read"), NOW.plus(Duration.ofHours(2)))).isFalse();
    }
}
