package com.example.authtutorial.common.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 值物件 {@link Subject} 的單元測試。
 *
 * <p>學習重點：值物件「在建構子就保證有效」、以值判斷相等。</p>
 */
@DisplayName("Subject 值物件")
class SubjectTest {

    @Test
    @DisplayName("可由非空白字串建立，並去除前後空白")
    void createsFromNonBlankValueAndTrims() {
        Subject subject = Subject.of("  alice  ");

        assertThat(subject.value()).isEqualTo("alice");
    }

    @Test
    @DisplayName("空白值會被拒絕（守護不變條件）")
    void rejectsBlankValue() {
        assertThatThrownBy(() -> Subject.of("   "))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("不可為空白");
    }

    @Test
    @DisplayName("相同 value 的兩個 Subject 相等")
    void equalsByValue() {
        assertThat(Subject.of("alice")).isEqualTo(Subject.of("alice"));
        assertThat(Subject.of("alice")).isNotEqualTo(Subject.of("bob"));
    }
}
