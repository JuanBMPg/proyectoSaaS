package com.juan.reservas.comun;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TokenSecretoTest {

    @Test
    void generaTokensDistintosQueCabenEnUnaUrl() {
        String uno = TokenSecreto.generar();
        String otro = TokenSecreto.generar();

        assertThat(uno).isNotEqualTo(otro).hasSize(43).matches("[A-Za-z0-9_-]+");
    }

    @Test
    void elHashEsDeterministaYNoRevelaElToken() {
        assertThat(TokenSecreto.hash("abc")).isEqualTo(TokenSecreto.hash("abc"));
        assertThat(TokenSecreto.hash("abc")).isNotEqualTo("abc").hasSize(64);
    }
}
