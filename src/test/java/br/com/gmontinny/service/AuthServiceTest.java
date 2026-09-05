package br.com.gmontinny.service;

import br.com.gmontinny.domain.entity.RefreshToken;
import br.com.gmontinny.domain.entity.User;
import br.com.gmontinny.domain.repository.RefreshTokenRepository;
import br.com.gmontinny.domain.repository.UserRepository;
import br.com.gmontinny.dto.request.LoginRequest;
import br.com.gmontinny.dto.request.RefreshTokenRequest;
import br.com.gmontinny.dto.response.AuthResponse;
import br.com.gmontinny.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService — Testes Unitários")
class AuthServiceTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserDetailsService userDetailsService;
    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshExpirationMs", 604800000L);
    }

    @Test
    @DisplayName("Login deve retornar access token e refresh token")
    void shouldReturnBothTokensOnLogin() {
        LoginRequest request = new LoginRequest("admin", "Admin@2026");
        UserDetails userDetails = userDetails("admin");
        User user = user("admin");

        when(userDetailsService.loadUserByUsername("admin")).thenReturn(userDetails);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(userDetails)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(userDetails)).thenReturn("refresh-token");
        when(jwtService.getExpirationMs()).thenReturn(900000L);

        AuthResponse response = authService.authenticate(request);

        assertThat(response.token()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.type()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900000L);
        verify(refreshTokenRepository).revokeAllByUserId(user.getId());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Login deve lançar exceção com credenciais inválidas")
    void shouldThrowOnBadCredentials() {
        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThatThrownBy(() -> authService.authenticate(new LoginRequest("admin", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("Refresh deve emitir novo par de tokens e revogar o anterior")
    void shouldRotateTokensOnRefresh() {
        User user = user("joao");
        RefreshToken stored = refreshToken(user, false, false);
        UserDetails userDetails = userDetails("joao");

        when(refreshTokenRepository.findByToken("old-refresh")).thenReturn(Optional.of(stored));
        when(jwtService.isRefreshTokenValid("old-refresh")).thenReturn(true);
        when(userDetailsService.loadUserByUsername("joao")).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("new-access");
        when(jwtService.generateRefreshToken(userDetails)).thenReturn("new-refresh");
        when(jwtService.getExpirationMs()).thenReturn(900000L);

        AuthResponse response = authService.refresh(new RefreshTokenRequest("old-refresh"));

        assertThat(response.token()).isEqualTo("new-access");
        assertThat(response.refreshToken()).isEqualTo("new-refresh");
        assertThat(stored.isRevoked()).isTrue();
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Refresh deve lançar 401 para token revogado")
    void shouldThrowOnRevokedRefreshToken() {
        User user = user("joao");
        RefreshToken stored = refreshToken(user, true, false);
        when(refreshTokenRepository.findByToken("revoked")).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest("revoked")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    @DisplayName("Refresh deve lançar 401 para token expirado")
    void shouldThrowOnExpiredRefreshToken() {
        User user = user("joao");
        RefreshToken stored = refreshToken(user, false, true);
        when(refreshTokenRepository.findByToken("expired")).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest("expired")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    @DisplayName("Logout deve revogar todos os tokens do usuário")
    void shouldRevokeAllTokensOnLogout() {
        User user = user("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        authService.logout("admin");

        verify(refreshTokenRepository).revokeAllByUserId(user.getId());
    }

    private UserDetails userDetails(String username) {
        return org.springframework.security.core.userdetails.User
                .withUsername(username).password("encoded").authorities(List.of()).build();
    }

    private User user(String username) {
        User u = new User();
        u.setUsername(username);
        return u;
    }

    private RefreshToken refreshToken(User user, boolean revoked, boolean expired) {
        RefreshToken rt = new RefreshToken();
        rt.setToken(revoked ? "revoked" : (expired ? "expired" : "old-refresh"));
        rt.setUser(user);
        rt.setRevoked(revoked);
        rt.setExpiresAt(expired
                ? LocalDateTime.now().minusDays(1)
                : LocalDateTime.now().plusDays(7));
        return rt;
    }
}
