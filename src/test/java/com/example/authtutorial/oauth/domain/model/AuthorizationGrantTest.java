package com.example.authtutorial.oauth.domain.model;

import com.example.authtutorial.common.domain.DomainException;
import com.example.authtutorial.common.domain.Subject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 聚合根 {@link AuthorizationGrant} 測試：守護「同意」與「scope 子集」規則。
 */
@DisplayName("AuthorizationGrant 聚合根")
class AuthorizationGrantTest {

    private static final ClientId CLIENT = ClientId.of("app-A");
    private static final Subject OWNER = Subject.of("alice");

    private AuthorizationGrant grantRequesting(String... scopes) {
        return AuthorizationGrant.request(CLIENT, OWNER, Scopes.fromStrings(Set.of(scopes)));
    }

    @Test
    @DisplayName("未取得使用者同意 → 不能核發授權")
    void cannotApproveWithoutConsent() {
        AuthorizationGrant grant = grantRequesting("contacts.read");

        assertThatThrownBy(() -> grant.approve(Scopes.fromStrings(Set.of("contacts.read"))))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("尚未同意");
    }

    @Test
    @DisplayName("核准的 scope 超出要求範圍 → 被拒絕（防止權限放大）")
    void cannotApproveBeyondRequested() {
        AuthorizationGrant grant = grantRequesting("contacts.read");
        grant.giveConsent();

        assertThatThrownBy(() -> grant.approve(Scopes.fromStrings(Set.of("contacts.read", "contacts.write"))))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("超出");
    }

    @Test
    @DisplayName("同意後核准子集 scope → 成功")
    void approvesSubsetAfterConsent() {
        AuthorizationGrant grant = grantRequesting("contacts.read", "contacts.write");
        grant.giveConsent();

        Scopes effective = grant.approve(Scopes.fromStrings(Set.of("contacts.read")));

        assertThat(effective.asStringSet()).containsExactly("contacts.read");
        assertThat(grant.isConsentGiven()).isTrue();
    }
}
