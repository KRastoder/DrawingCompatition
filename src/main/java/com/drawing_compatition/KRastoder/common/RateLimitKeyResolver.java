package com.drawing_compatition.KRastoder.common;

import com.drawing_compatition.KRastoder.auth.AppUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Key resolver for Bucket4j rate limiting.
 * Used via SpEL expressions in bucket4j configuration.
 */
@Component("rateLimitKeyResolver")
public class RateLimitKeyResolver {

    /**
     * Returns user ID if authenticated, otherwise client IP.
     * Used as cache key for rate limiting.
     */
    public String userOrIp() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return "unknown";
        }

        // Try to get authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof AppUserPrincipal principal) {
            return "user:" + principal.id();
        }

        // Fallback to IP
        return "ip:" + getClientIp(request);
    }

    /**
     * Returns client IP address.
     */
    public String clientIp() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return "unknown";
        }
        return "ip:" + getClientIp(request);
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    private String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        String ip;
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            ip = forwardedFor.split(",")[0].trim();
        } else {
            ip = request.getRemoteAddr();
        }

        // Normalize IPv6 localhost
        if ("0:0:0:0:0:0:0:1".equals(ip) || "[::1]".equals(ip)) {
            ip = "127.0.0.1";
        }
        return ip;
    }
}