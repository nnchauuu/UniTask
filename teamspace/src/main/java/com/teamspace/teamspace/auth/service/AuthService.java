package com.teamspace.teamspace.auth.service;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.teamspace.teamspace.auth.dto.AuthResponse;
import com.teamspace.teamspace.auth.dto.LoginRequest;
import com.teamspace.teamspace.auth.dto.RegisterRequest;
import com.teamspace.teamspace.auth.dto.UserResponse;
import com.teamspace.teamspace.exception.BadRequestException;
import com.teamspace.teamspace.exception.ResourceNotFoundException;
import com.teamspace.teamspace.exception.UnauthorizedException;
import com.teamspace.teamspace.user.entity.Role;
import com.teamspace.teamspace.user.entity.User;
import com.teamspace.teamspace.user.enums.RoleName;
import com.teamspace.teamspace.user.repository.RoleRepository;
import com.teamspace.teamspace.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String GOOGLE_ISSUER = "https://accounts.google.com";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Value("${app.google.client-id:}")
    private String googleClientId;

    @Value("${app.google.client-secret:}")
    private String googleClientSecret;

    private volatile NimbusJwtDecoder googleJwtDecoder;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BadRequestException("Email da duoc su dung");
        }

        Role memberRole = roleRepository.findByName(RoleName.MEMBER)
                .orElseThrow(() -> new ResourceNotFoundException("Role MEMBER chua duoc khoi tao"));

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(new HashSet<>(Set.of(memberRole)))
                .enabled(true)
                .build();

        return createAuthResponse(userRepository.save(user));
    }

    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
            );
        } catch (BadCredentialsException exception) {
            throw new UnauthorizedException("Email hoac mat khau khong dung");
        } catch (DisabledException exception) {
            throw new UnauthorizedException("Tai khoan da bi khoa");
        }

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UnauthorizedException("Email hoac mat khau khong dung"));
        return createAuthResponse(user);
    }

    @Transactional
    public AuthResponse loginWithGoogle(String code, String redirectUri, String requestedWith) {
        if (!"XMLHttpRequest".equals(requestedWith)) {
            throw new UnauthorizedException("Yeu cau dang nhap Google khong hop le");
        }

        GoogleTokenResponse tokenResponse = exchangeGoogleCode(code, redirectUri);
        Jwt googleUser = decodeGoogleCredential(tokenResponse.idToken());
        Boolean emailVerified = googleUser.getClaim("email_verified");
        String email = googleUser.getClaimAsString("email");

        if (!Boolean.TRUE.equals(emailVerified) || email == null || email.isBlank()) {
            throw new UnauthorizedException("Tai khoan Google chua xac minh email");
        }

        String normalizedEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseGet(() -> createGoogleUser(googleUser, normalizedEmail));

        if (!user.isEnabled()) {
            throw new UnauthorizedException("Tai khoan da bi khoa");
        }

        return createAuthResponse(user);
    }

    private GoogleTokenResponse exchangeGoogleCode(String code, String redirectUri) {
        if (googleClientId == null || googleClientId.isBlank()
                || googleClientSecret == null || googleClientSecret.isBlank()) {
            throw new BadRequestException("Dang nhap Google chua duoc cau hinh day du");
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("client_id", googleClientId);
        form.add("client_secret", googleClientSecret);
        form.add("redirect_uri", redirectUri);
        form.add("grant_type", "authorization_code");

        try {
            GoogleTokenResponse response = RestClient.create()
                    .post()
                    .uri("https://oauth2.googleapis.com/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(GoogleTokenResponse.class);

            if (response == null || response.idToken() == null || response.idToken().isBlank()) {
                throw new UnauthorizedException("Google khong tra ve thong tin nguoi dung");
            }
            return response;
        } catch (RestClientResponseException exception) {
            throw new UnauthorizedException("Google authorization code khong hop le hoac da het han");
        }
    }

    private User createGoogleUser(Jwt googleUser, String email) {
        Role memberRole = roleRepository.findByName(RoleName.MEMBER)
                .orElseThrow(() -> new ResourceNotFoundException("Role MEMBER chua duoc khoi tao"));
        String fullName = googleUser.getClaimAsString("name");

        User user = User.builder()
                .fullName(fullName == null || fullName.isBlank() ? email : fullName.trim())
                .email(email)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .roles(new HashSet<>(Set.of(memberRole)))
                .enabled(true)
                .build();
        return userRepository.save(user);
    }

    private Jwt decodeGoogleCredential(String credential) {
        if (googleClientId == null || googleClientId.isBlank()) {
            throw new BadRequestException("Dang nhap Google chua duoc cau hinh");
        }

        try {
            return getGoogleJwtDecoder().decode(credential);
        } catch (JwtException exception) {
            throw new UnauthorizedException("Thong tin dang nhap Google khong hop le");
        }
    }

    private NimbusJwtDecoder getGoogleJwtDecoder() {
        if (googleJwtDecoder == null) {
            synchronized (this) {
                if (googleJwtDecoder == null) {
                    NimbusJwtDecoder decoder = (NimbusJwtDecoder) JwtDecoders.fromIssuerLocation(GOOGLE_ISSUER);
                    OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(GOOGLE_ISSUER);
                    OAuth2TokenValidator<Jwt> audienceValidator = jwt -> jwt.getAudience().contains(googleClientId)
                            ? OAuth2TokenValidatorResult.success()
                            : OAuth2TokenValidatorResult.failure(
                                    new OAuth2Error("invalid_token", "Google token sai audience", null));
                    decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator));
                    googleJwtDecoder = decoder;
                }
            }
        }
        return googleJwtDecoder;
    }

    private AuthResponse createAuthResponse(User user) {
        return AuthResponse.builder()
                .accessToken(jwtService.generateToken(user.getEmail()))
                .tokenType("Bearer")
                .user(UserResponse.from(user))
                .build();
    }

    private record GoogleTokenResponse(
            @JsonProperty("id_token") String idToken,
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("refresh_token") String refreshToken) {
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new UnauthorizedException("Ban chua dang nhap");
        }

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UnauthorizedException("Khong tim thay user hien tai"));
        return UserResponse.from(user);
    }
}
