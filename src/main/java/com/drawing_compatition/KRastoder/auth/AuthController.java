package com.drawing_compatition.KRastoder.auth;

import com.drawing_compatition.KRastoder.user.ProfileService;
import com.drawing_compatition.KRastoder.user.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final ProfileService profileService;

    public AuthController(AuthService authService, ProfileService profileService) {
        this.authService = authService;
        this.profileService = profileService;
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken csrfToken) {
        String token = csrfToken.getToken();
        return new CsrfResponse("XSRF-TOKEN", csrfToken.getHeaderName(), token);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        return authService.register(request, httpRequest, httpResponse);
    }

    @PostMapping("/login")
    public UserResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        return authService.login(request, httpRequest, httpResponse);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) {
        authService.logout(request, response, authentication);
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AppUserPrincipal principal) {
        return profileService.get(principal.id());
    }
}
