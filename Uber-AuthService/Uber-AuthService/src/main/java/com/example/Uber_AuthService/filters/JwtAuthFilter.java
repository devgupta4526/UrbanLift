package com.example.Uber_AuthService.filters;

import com.example.Uber_AuthService.services.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;

    public JwtAuthFilter(UserDetailsService userDetailsService, JwtService jwtService) {
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
    }

    private final RequestMatcher uriMatcher =
            new AntPathRequestMatcher("/api/v1/auth/validate", HttpMethod.GET.name());


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
            String  token = null;
            if(request.getCookies() != null) {
                for (Cookie cookie : request.getCookies()) {
                    if (cookie.getName().equals("JWT_TOKEN")) {
                        token = cookie.getValue();
                    }
                }
            }
            if(token != null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            String email = jwtService.extractSubject(token);
            if(email != null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                if(jwtService.isTokenValid(token, userDetails.getUsername())) {
                    UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(userDetails,null,null);
                    usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);

                }
            }
            filterChain.doFilter(request,response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        RequestMatcher matcher = new NegatedRequestMatcher(uriMatcher);
        return matcher.matches(request);
    }
}
