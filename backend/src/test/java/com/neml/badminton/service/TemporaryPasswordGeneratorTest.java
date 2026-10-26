package com.neml.badminton.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TemporaryPasswordGeneratorTest {
    @Test
    void producesStrongNonRepeatingPresentationCredentials() {
        TemporaryPasswordGenerator generator = new TemporaryPasswordGenerator();
        String first = generator.generate();
        String second = generator.generate();
        assertThat(first).hasSize(14).containsPattern("[A-Z]").containsPattern("[a-z]")
                .containsPattern("[0-9]").containsPattern("[@#$%]");
        assertThat(second).isNotEqualTo(first);
    }
}
