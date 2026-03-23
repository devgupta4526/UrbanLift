package com.example.Uber_DriverService.controllers;


import com.example.Uber_DriverService.dtos.*;
import com.example.Uber_DriverService.services.DriverAuthService;
import com.example.Uber_DriverService.services.JwtAuthService;
import com.example.Uber_EntityService.Models.Driver;
import com.example.Uber_EntityService.Models.DriverApprovalStatus;
import com.example.Uber_DriverService.repositories.DriverRepository;
import jakarta.servlet.http.Cookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import com.example.Uber_DriverService.util.EmailNormalizer;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/v1/driver/auth")
public class DriverAuthController {

    private static final Logger log = LoggerFactory.getLogger(DriverAuthController.class);

    private final DriverAuthService driverAuthService;
    private final JwtAuthService jwtService;
    private final DriverRepository driverRepository;
    private final PasswordEncoder passwordEncoder;

    public DriverAuthController(DriverAuthService driverAuthService, JwtAuthService jwtService, DriverRepository driverRepository, PasswordEncoder passwordEncoder) {
        this.driverAuthService = driverAuthService;
        this.jwtService = jwtService;
        this.driverRepository = driverRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/signup")
    public ResponseEntity<DriverDto> signUp(@Valid @RequestBody DriverSignUpRequestDto dto) {
        DriverDto driver = driverAuthService.signUp(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(driver);
    }

    @PostMapping("/signin")
    public ResponseEntity<AuthResponseDto> signIn(@Valid @RequestBody AuthRequestDto dto, HttpServletResponse response) {
        String email = EmailNormalizer.normalize(dto.getEmail());
        return driverRepository.findByEmail(email)
                .filter(d -> d.getDriverApprovalStatus() == DriverApprovalStatus.APPROVED)
                .filter(d -> passwordEncoder.matches(dto.getPassword(), d.getPassword()))
                .map(driver -> {
                    String token = jwtService.createToken(driver.getEmail());
                    ResponseCookie cookie = ResponseCookie.from("DRIVER_JWT", token)
                            .httpOnly(true)
                            .maxAge(7 * 24 * 3600)
                            .path("/")
                            .build();
                    response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());
                    return ResponseEntity.ok(AuthResponseDto.builder().success(true).build());
                })
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(AuthResponseDto.builder().success(false).build()));
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validate(HttpServletRequest request) {
        String token = null;
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if ("DRIVER_JWT".equals(c.getName())) {
                    token = c.getValue();
                    break;
                }
            }
        }
        if (token == null) {
            String auth = request.getHeader("Authorization");
            if (auth != null && auth.startsWith("Bearer ")) {
                token = auth.substring(7);
            }
        }
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No token");
        }
        try {
            String email = jwtService.extractSubject(token);
            if (jwtService.isTokenValid(token, email)) {
                return ResponseEntity.ok(AuthResponseDto.builder().success(true).build());
            }
        /* REMOVED: catch (Exception e) — narrowed to JwtException for token parsing/validation failures. */
        } catch (JwtException e) {
            log.debug("Driver JWT validation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
    }
}
