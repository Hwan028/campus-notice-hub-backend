package com.kkamar.cnh.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NoticeSpecificationsTest {

    @Test
    void likeWildcardsInUserKeywordsAreEscaped() {
        assertThat(NoticeSpecifications.escapeLike("장학금")).isEqualTo("장학금");
        assertThat(NoticeSpecifications.escapeLike("100%")).isEqualTo("100!%");
        assertThat(NoticeSpecifications.escapeLike("a_b")).isEqualTo("a!_b");
        assertThat(NoticeSpecifications.escapeLike("100%_!")).isEqualTo("100!%!_!!");
    }
}
