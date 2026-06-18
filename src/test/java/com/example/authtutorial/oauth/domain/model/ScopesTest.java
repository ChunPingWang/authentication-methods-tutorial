package com.example.authtutorial.oauth.domain.model;

import com.example.authtutorial.common.domain.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Scopes 值物件（子集規則）")
class ScopesTest {

    @Test
    @DisplayName("containsAll：要求的 scope 是核准的子集時為真")
    void containsAllReflectsSubset() {
        Scopes requested = Scopes.fromStrings(Set.of("contacts.read", "contacts.write"));
        Scopes approved = Scopes.fromStrings(Set.of("contacts.read"));

        assertThat(requested.containsAll(approved)).isTrue();
        assertThat(approved.containsAll(requested)).isFalse();
    }

    @Test
    @DisplayName("空集合不被允許")
    void rejectsEmpty() {
        assertThatThrownBy(() -> Scopes.fromStrings(Set.of()))
                .isInstanceOf(DomainException.class);
    }
}
