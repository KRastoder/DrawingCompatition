package com.drawing_compatition.KRastoder.auth;

import com.drawing_compatition.KRastoder.common.ConflictException;
import com.drawing_compatition.KRastoder.common.NotFoundException;
import com.drawing_compatition.KRastoder.user.User;
import com.drawing_compatition.KRastoder.user.UserRepository;
import com.drawing_compatition.KRastoder.user.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public UserResponse register(RegisterRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        log.debug("Registering user: {}", username);
        try {
            if (users.existsByUsername(username)) {
                throw new ConflictException("That username is already taken.");
            }

            User user = users.save(new User(
                    username,
                    request.displayName().trim(),
                    passwordEncoder.encode(request.password())
            ));

            AppUserPrincipal principal = AppUserPrincipal.from(user);
            storeAuthentication(principal, httpRequest, httpResponse);
            log.info("Registered new user: {}", username);
            return UserResponse.from(user);
        } catch (ConflictException e) {
            log.warn("Registration failed for username {}: {}", username, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error registering user {}", username, e);
            throw new RuntimeException("Failed to register user", e);
        }
    }

    @Transactional(readOnly = true)
    public UserResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        log.debug("Login attempt for user: {}", username);
        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            username,
                            request.password()
                    )
            );

            AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
            storeAuthentication(principal, httpRequest, httpResponse);
            User user = users.findById(principal.id())
                    .orElseThrow(() -> new NotFoundException("User not found."));
            log.info("User logged in: {}", username);
            return UserResponse.from(user);
        } catch (BadCredentialsException e) {
            log.warn("Failed login attempt for user: {}", username);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during login for user {}", username, e);
            throw new RuntimeException("Failed to login", e);
        }
    }

    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        log.debug("User logging out");
        try {
            new SecurityContextLogoutHandler().logout(request, response, authentication);
            log.info("User logged out successfully");
        } catch (Exception e) {
            log.error("Error during logout", e);
        }
    }

    private void storeAuthentication(
            AppUserPrincipal principal,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        HttpSession session = request.getSession(false);
        if (session != null) {
            request.changeSessionId();
        }

        securityContextRepository.saveContext(context, request, response);
    }
}
