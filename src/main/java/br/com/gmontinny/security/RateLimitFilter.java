package br.com.gmontinny.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/v1/auth/login";
    private static final String RATE_LIMIT_HEADER = "X-Rate-Limit-Remaining";
    private static final String RETRY_AFTER_HEADER = "Retry-After";

    private final ProxyManager<String> rateLimitProxyManager;

    @Value("${app.rate-limit.login-capacity:20}")
    private long loginCapacity;

    @Value("${app.rate-limit.login-refill-tokens:20}")
    private long loginRefillTokens;

    @Value("${app.rate-limit.login-refill-seconds:60}")
    private long loginRefillSeconds;

    @Value("${app.rate-limit.api-capacity:200}")
    private long apiCapacity;

    @Value("${app.rate-limit.api-refill-tokens:200}")
    private long apiRefillTokens;

    @Value("${app.rate-limit.api-refill-seconds:60}")
    private long apiRefillSeconds;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {

        String ip   = resolveClientIp(request);
        String path = request.getRequestURI();
        boolean isLoginPath = LOGIN_PATH.equals(path);

        String bucketKey = isLoginPath ? "rl:login:" + ip : "rl:api:" + ip;
        Supplier<BucketConfiguration> configSupplier = isLoginPath
                ? this::loginBucketConfig
                : this::apiBucketConfig;

        var bucket = rateLimitProxyManager.builder().build(bucketKey, configSupplier);
        var probe  = bucket.tryConsumeAndReturnRemaining(1);

        response.setHeader(RATE_LIMIT_HEADER, String.valueOf(probe.getRemainingTokens()));

        if (probe.isConsumed()) {
            chain.doFilter(request, response);
        } else {
            long waitSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;
            response.setHeader(RETRY_AFTER_HEADER, String.valueOf(waitSeconds));
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"status\":429,\"error\":\"Too Many Requests\"," +
                    "\"message\":\"Rate limit atingido. Tente novamente em " + waitSeconds + " segundos.\"}");
            log.warn("[RATE-LIMIT] Bloqueado — ip={}, path={}, aguarde={}s", ip, path, waitSeconds);
        }
    }

    private BucketConfiguration loginBucketConfig() {
        return BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(loginCapacity)
                        .refillGreedy(loginRefillTokens, Duration.ofSeconds(loginRefillSeconds))
                        .build())
                .build();
    }

    private BucketConfiguration apiBucketConfig() {
        return BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(apiCapacity)
                        .refillGreedy(apiRefillTokens, Duration.ofSeconds(apiRefillSeconds))
                        .build())
                .build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
