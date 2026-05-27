package com.skillboost.controller;

import com.skillboost.model.AppUser;
import com.skillboost.repository.UserRepository;
import com.skillboost.security.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthController(UserRepository users,
                          PasswordEncoder encoder,
                          JwtService jwtService,
                          AuthenticationManager authenticationManager) {
        this.users = users;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 64) String username,
            @Email String email,
            @NotBlank @Size(min = 6, max = 128) String password) {}

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password) {}

    public record AuthResponse(String token, String username, String role, long expiresInMs) {}

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        if (users.existsByUsername(req.username())) {
            return ResponseEntity.status(409).body(Map.of("error", "Username already taken"));
        }
        if (req.email() != null && !req.email().isBlank() && users.existsByEmail(req.email())) {
            return ResponseEntity.status(409).body(Map.of("error", "Email already registered"));
        }
        AppUser user = new AppUser(
                req.username(),
                req.email() != null && req.email().isBlank() ? null : req.email(),
                encoder.encode(req.password()),
                AppUser.Role.USER);
        users.save(user);
        String token = jwtService.issue(user.getUsername(), user.getRole().name());
        return ResponseEntity.ok(new AuthResponse(
                token, user.getUsername(), user.getRole().name(), jwtService.getExpirationMillis()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.username(), req.password()));
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid username or password"));
        }
        AppUser user = users.findByUsername(req.username()).orElseThrow();
        String token = jwtService.issue(user.getUsername(), user.getRole().name());
        return ResponseEntity.ok(new AuthResponse(
                token, user.getUsername(), user.getRole().name(), jwtService.getExpirationMillis()));
    }
}
