package com.example.Uber_AuthService.controllers;


import com.example.Uber_AuthService.helpers.AuthPassengerDetails;
import com.example.Uber_AuthService.dto.AuthRequestDto;
import com.example.Uber_AuthService.dto.AuthResponseDto;
import com.example.Uber_AuthService.dto.PassengerDto;
import com.example.Uber_AuthService.dto.PassengerSignUpRequestDto;
import com.example.Uber_AuthService.services.AuthService;
import com.example.Uber_AuthService.services.JwtService;
import com.example.Uber_AuthService.util.EmailNormalizer;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(AuthService authService, AuthenticationManager authenticationManager,
                          JwtService jwtService) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }



    @PostMapping("/signup/passenger")
    public ResponseEntity<PassengerDto> signup(@Valid @RequestBody PassengerSignUpRequestDto passengerSignUpRequestDto) {
        PassengerDto passengerDto = authService.signUpPassenger(passengerSignUpRequestDto);
        return new ResponseEntity<>(passengerDto, HttpStatus.CREATED);
    }


    @PostMapping("/signin/passenger")
    public ResponseEntity<AuthResponseDto> signin(@Valid @RequestBody AuthRequestDto authRequestDto,
                                                  HttpServletResponse response) {

        authRequestDto.setEmail(EmailNormalizer.normalize(authRequestDto.getEmail()));
        Authentication authentication = authenticationManager.authenticate(new
                UsernamePasswordAuthenticationToken(authRequestDto.getEmail(), authRequestDto.getPassword()));

        if(authentication.isAuthenticated()) {
            AuthPassengerDetails principal = (AuthPassengerDetails) authentication.getPrincipal();
            Map<String, Object> claims = new HashMap<>();
            claims.put("passengerId", principal.getId());
            claims.put("role", "PASSENGER");
            String jwtToken = jwtService.createToken(claims, authRequestDto.getEmail());
            ResponseCookie cookie = ResponseCookie.from("JWT_TOKEN", jwtToken)
                    .httpOnly(true)
                    .maxAge(7*24*3600)
                    .secure(false)
                    .path("/")
                    .build();
            response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());
            return new ResponseEntity<>(
                    AuthResponseDto.builder()
                            .success(true)
                            .passengerId(principal.getId())
                            .email(principal.getUsername())
                            .build(),
                    HttpStatus.OK);

        }else {
            throw new UsernameNotFoundException("User not found");
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validate(HttpServletRequest request, HttpServletResponse response) {
        if (request.getCookies() == null) {
            return new ResponseEntity<>("No cookies found", HttpStatus.UNAUTHORIZED);
        }
        String jwtToken = null;
        for(Cookie cookie : request.getCookies()) {
            if(cookie.getName().equals("JWT_TOKEN")) {
                jwtToken = cookie.getValue();
                break;
            }
        }
        if(jwtToken == null) {
            return new ResponseEntity<>("Invalid JWT_TOKEN", HttpStatus.UNAUTHORIZED);
        }
        try{
            String email = jwtService.extractSubject(jwtToken);
            if(jwtService.isTokenValid(jwtToken,email)){
                Long passengerId = null;
                try {
                    Object raw = jwtService.extractPayload(jwtToken, "passengerId");
                    if (raw instanceof Number) {
                        passengerId = ((Number) raw).longValue();
                    }
                } catch (Exception ignored) {
                    /* optional claim */
                }
                return new ResponseEntity<>(
                        AuthResponseDto.builder()
                                .success(true)
                                .passengerId(passengerId)
                                .email(email)
                                .build(),
                        HttpStatus.OK
                );
            }
        }
        /* REMOVED: catch (Exception e) — hid non-JWT bugs. Narrowed to JwtException. */
        catch (JwtException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return new ResponseEntity<>("Invalid JWT_TOKEN", HttpStatus.UNAUTHORIZED);
        }
        return new ResponseEntity<>("Invalid token", HttpStatus.UNAUTHORIZED);
    }



}
