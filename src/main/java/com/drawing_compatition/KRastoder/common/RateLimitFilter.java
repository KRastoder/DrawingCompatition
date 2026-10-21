package com.drawing_compatition.KRastoder.common;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting filter using Bucket4j with in-memory ConcurrentHashMap cache.
 * Provides per-IP and per-user rate limits for different endpoint patterns.
 * Runs after Spring Security filter chain (order > SecurityProperties.DEFAULT_FILTER_ORDER)
 * so that authentication is available for user-based rate limiting.
 */
@Component
@Order(100) // After Spring Security filter chain (which is at order 0)
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> bucketCache = new ConcurrentHashMap<>();
    private final RateLimitKeyResolver keyResolver;

    // Rate limit configurations
    private static final RateLimitConfig LOGIN_CONFIG = new RateLimitConfig(
            "login", 10, Duration.ofMinutes(1), RateLimitType.IP
    );
    private static final RateLimitConfig REGISTER_CONFIG = new RateLimitConfig(
            "register", 5, Duration.ofMinutes(1), RateLimitType.IP
    );
    private static final RateLimitConfig DRAWING_CREATE_CONFIG = new RateLimitConfig(
            "drawing:create", 30, Duration.ofHours(1), RateLimitType.USER
    );
    private static final RateLimitConfig IMAGE_REPLACE_CONFIG = new RateLimitConfig(
            "image:replace", 30, Duration.ofHours(1), RateLimitType.USER
    );
    private static final RateLimitConfig VOTE_CONFIG = new RateLimitConfig(
            "vote", 60, Duration.ofMinutes(1), RateLimitType.USER
    );
    private static final RateLimitConfig COMMENT_CONFIG = new RateLimitConfig(
            "comment", 30, Duration.ofMinutes(1), RateLimitType.USER
    );
    private static final RateLimitConfig AVATAR_UPLOAD_CONFIG = new RateLimitConfig(
            "avatar:upload", 10, Duration.ofHours(1), RateLimitType.USER
    );
    private static final RateLimitConfig GENERAL_API_CONFIG = new RateLimitConfig(
            "api:general", 100, Duration.ofMinutes(1), RateLimitType.USER
    );
    private static final RateLimitConfig ACTUATOR_CONFIG = new RateLimitConfig(
            "actuator", 30, Duration.ofMinutes(1), RateLimitType.IP
    );

    public RateLimitFilter(RateLimitKeyResolver keyResolver) {
        this.keyResolver = keyResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Determine which rate limit config applies
        RateLimitConfig config = resolveConfig(path, method);
        if (config == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Resolve key (IP or user)
        String key = config.type == RateLimitType.IP
                ? keyResolver.clientIp()
                : keyResolver.userOrIp();

        String cacheKey = config.name + ":" + key;
        Bucket bucket = bucketCache.computeIfAbsent(cacheKey, k -> createBucket(config));

        // Try to consume a token
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            // Add rate limit headers
            addRateLimitHeaders(response, probe);
            filterChain.doFilter(request, response);
        } else {
            // Rate limit exceeded
            addRateLimitHeaders(response, probe);
            response.setStatus(429); // Too Many Requests
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too many requests\",\"retryAfter\":" + (probe.getNanosToWaitForRefill() / 1_000_000_000) + "}");
        }
    }

    private RateLimitConfig resolveConfig(String path, String method) {
        // Auth endpoints
        if (path.equals("/api/auth/login") && "POST".equals(method)) {
            return LOGIN_CONFIG;
        }
        if (path.equals("/api/auth/register") && "POST".equals(method)) {
            return REGISTER_CONFIG;
        }

        // Drawing creation
        if (path.equals("/api/drawings") && "POST".equals(method)) {
            return DRAWING_CREATE_CONFIG;
        }

        // Image replacement
        if (path.matches("/api/drawings/\\d+/image") && "PUT".equals(method)) {
            return IMAGE_REPLACE_CONFIG;
        }

        // Voting
        if (path.matches("/api/drawings/\\d+/votes") && "POST".equals(method)) {
            return VOTE_CONFIG;
        }

        // Comments
        if (path.matches("/api/drawings/\\d+/comments") && "POST".equals(method)) {
            return COMMENT_CONFIG;
        }

        // Avatar upload
        if (path.equals("/api/profile/avatar") && "PUT".equals(method)) {
            return AVATAR_UPLOAD_CONFIG;
        }

        // Actuator
        if (path.startsWith("/actuator/")) {
            return ACTUATOR_CONFIG;
        }

        // General API endpoints
        if (path.startsWith("/api/")) {
            return GENERAL_API_CONFIG;
        }

        return null; // No rate limiting
    }

    private Bucket createBucket(RateLimitConfig config) {
        Bandwidth limit = Bandwidth.classic(config.capacity, Refill.intervally(config.capacity, config.window));
        return Bucket.builder().addLimit(limit).build();
    }

    private void addRateLimitHeaders(HttpServletResponse response, ConsumptionProbe probe) {
        response.setHeader("X-RateLimit-Limit", String.valueOf(probe.getRemainingTokens() + 1)); // approximate
        response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
        long retryAfter = probe.getNanosToWaitForRefill() / 1_000_000_000;
        if (retryAfter > 0) {
            response.setHeader("Retry-After", String.valueOf(retryAfter));
        }
    }

    private enum RateLimitType {
        IP, USER
    }

    private record RateLimitConfig(String name, int capacity, Duration window, RateLimitType type) {}
}