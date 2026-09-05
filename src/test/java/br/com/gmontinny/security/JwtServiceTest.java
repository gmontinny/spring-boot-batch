package br.com.gmontinny.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtService — Testes Unitários")
class JwtServiceTest {

    private JwtService jwtService;

    private static final String SECRET =
            "3cfa76ef14937c1c0ea519f8fc057a80fcd04a7420f8e8bcd0a7567c272e007b";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationMs", 900000L);
        ReflectionTestUtils.setField(jwtService, "refreshExpirationMs", 604800000L);
    }

    @Test
    @DisplayName("Access token deve conter username e tipo 'access'")
    void shouldGenerateAccessToken() {
        UserDetails user = user("admin");
        String token = jwtService.generateToken(user);

        assertThat(jwtService.extractUsername(token)).isEqualTo("admin");
        assertThat(jwtService.extractTokenType(token)).isEqualTo(JwtService.TOKEN_TYPE_ACCESS);
    }

    @Test
    @DisplayName("Refresh token deve conter username e tipo 'refresh'")
    void shouldGenerateRefreshToken() {
        UserDetails user = user("admin");
        String token = jwtService.generateRefreshToken(user);

        assertThat(jwtService.extractUsername(token)).isEqualTo("admin");
        assertThat(jwtService.extractTokenType(token)).isEqualTo(JwtService.TOKEN_TYPE_REFRESH);
    }

    @Test
    @DisplayName("isTokenValid deve aceitar access token válido")
    void shouldValidateAccessToken() {
        UserDetails user = user("joao");
        String token = jwtService.generateToken(user);

        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    @DisplayName("isTokenValid deve rejeitar refresh token (tipo errado)")
    void shouldRejectRefreshTokenAsAccessToken() {
        UserDetails user = user("joao");
        String refreshToken = jwtService.generateRefreshToken(user);

        assertThat(jwtService.isTokenValid(refreshToken, user)).isFalse();
    }

    @Test
    @DisplayName("isRefreshTokenValid deve aceitar refresh token válido")
    void shouldValidateRefreshToken() {
        String token = jwtService.generateRefreshToken(user("alice"));

        assertThat(jwtService.isRefreshTokenValid(token)).isTrue();
    }

    @Test
    @DisplayName("isRefreshTokenValid deve rejeitar access token (tipo errado)")
    void shouldRejectAccessTokenAsRefreshToken() {
        String token = jwtService.generateToken(user("alice"));

        assertThat(jwtService.isRefreshTokenValid(token)).isFalse();
    }

    @Test
    @DisplayName("Token expirado deve ser inválido")
    void shouldRejectExpiredToken() {
        ReflectionTestUtils.setField(jwtService, "expirationMs", -1000L);
        UserDetails user = user("admin");
        String token = jwtService.generateToken(user);

        assertThat(jwtService.isTokenValid(token, user)).isFalse();
    }

    @Test
    @DisplayName("Token de usuário diferente deve ser inválido")
    void shouldRejectTokenForDifferentUser() {
        String token = jwtService.generateToken(user("alice"));

        assertThat(jwtService.isTokenValid(token, user("bob"))).isFalse();
    }

    private UserDetails user(String username) {
        return User.withUsername(username).password("pass").authorities(List.of()).build();
    }
}
