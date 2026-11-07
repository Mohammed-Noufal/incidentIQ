package com.example.incidentagent.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import com.example.incidentagent.common.exception.ApiException;
import com.example.incidentagent.common.exception.ErrorCode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthProperties properties;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final UserAccountRepository userAccountRepository;
    private final String encodedPassword;

    public AuthService(
            AuthProperties properties,
            PasswordEncoder passwordEncoder,
            JwtEncoder jwtEncoder,
            UserAccountRepository userAccountRepository) {
        this.properties = properties;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.userAccountRepository = userAccountRepository;
        this.encodedPassword = passwordEncoder.encode(properties.getPassword());
    }

    public TokenResponse login(LoginRequest request) {
        String username = normalizeUsername(request.username());
        UserAccount account = userAccountRepository.findByUsername(username).orElse(null);
        boolean validCredentials;
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Invalid username or password");
        }
        if (account != null) {
            validCredentials = passwordEncoder.matches(request.password(), account.getPasswordHash());
        } else {
            boolean usernameMatches = MessageDigest.isEqual(
                    properties.getUsername().toLowerCase(java.util.Locale.ROOT).getBytes(StandardCharsets.UTF_8),
                    username.getBytes(StandardCharsets.UTF_8));
            boolean passwordMatches = passwordEncoder.matches(request.password(), encodedPassword);
            validCredentials = usernameMatches && passwordMatches;
        }
        if (!validCredentials) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Invalid username or password");
        }
        return createToken(username);
    }

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        String username = normalizeUsername(request.username());
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "Password must not exceed 72 UTF-8 bytes");
        }
        if (username.equals(properties.getUsername().toLowerCase(java.util.Locale.ROOT))
                || userAccountRepository.existsByUsername(username)) {
            throw new ApiException(ErrorCode.INVALID_STATE, "That username is already in use");
        }

        UserAccount account = new UserAccount(username, passwordEncoder.encode(request.password()));
        try {
            userAccountRepository.saveAndFlush(account);
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(ErrorCode.INVALID_STATE, "That username is already in use");
        }
        return createToken(username);
    }

    private TokenResponse createToken(String username) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(properties.getTokenValiditySeconds());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("incident-agent")
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(username)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenResponse(token, "Bearer", properties.getTokenValiditySeconds(), username);
    }

    private static String normalizeUsername(String username) {
        return username.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
