package br.com.gmontinny.service;

import br.com.gmontinny.domain.entity.RefreshToken;
import br.com.gmontinny.domain.entity.User;
import br.com.gmontinny.domain.repository.RefreshTokenRepository;
import br.com.gmontinny.domain.repository.UserRepository;
import br.com.gmontinny.dto.request.LoginRequest;
import br.com.gmontinny.dto.request.RefreshTokenRequest;
import br.com.gmontinny.dto.response.AuthResponse;
import br.com.gmontinny.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    /**
     * Autentica o usuário e retorna um par de tokens:
     * - access token (15 min) para uso nas APIs
     * - refresh token (7 dias) para renovar o access token
     */
    @Transactional
    public AuthResponse authenticate(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.username());
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // Revoga todos os refresh tokens anteriores do usuário (single-session)
        refreshTokenRepository.revokeAllByUserId(user.getId());

        String accessToken  = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        persistRefreshToken(user, refreshToken);

        log.info("[AUTH] Login bem-sucedido — user={}", request.username());
        return new AuthResponse(accessToken, refreshToken, request.username(), jwtService.getExpirationMs());
    }

    /**
     * Renova o access token a partir de um refresh token válido.
     * O refresh token atual é revogado e um novo par é emitido (rotation).
     */
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken stored = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token não encontrado"));

        if (!stored.isValid()) {
            log.warn("[AUTH] Refresh token inválido ou expirado — user={}", stored.getUser().getUsername());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expirado ou revogado");
        }

        if (!jwtService.isRefreshTokenValid(request.refreshToken())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token inválido");
        }

        // Revoga o token atual (rotation — evita reutilização)
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        User user = stored.getUser();
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());

        String newAccessToken  = jwtService.generateToken(userDetails);
        String newRefreshToken = jwtService.generateRefreshToken(userDetails);

        persistRefreshToken(user, newRefreshToken);

        log.info("[AUTH] Token renovado — user={}", user.getUsername());
        return new AuthResponse(newAccessToken, newRefreshToken, user.getUsername(), jwtService.getExpirationMs());
    }

    /**
     * Revoga todos os refresh tokens do usuário (logout global).
     */
    @Transactional
    public void logout(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            refreshTokenRepository.revokeAllByUserId(user.getId());
            log.info("[AUTH] Logout — todos os tokens revogados para user={}", username);
        });
    }

    private void persistRefreshToken(User user, String rawToken) {
        RefreshToken entity = new RefreshToken();
        entity.setToken(rawToken);
        entity.setUser(user);
        entity.setExpiresAt(LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000));
        refreshTokenRepository.save(entity);
    }
}
