package com.example.zuora.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.ALWAYS))
            .authorizeHttpRequests(auth -> auth
                // Public pages
                .requestMatchers("/", "/login", "/signup", "/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                // Email verification and password reset (public)
                .requestMatchers("/verify-email", "/forgot-password", "/reset-password").permitAll()
                .requestMatchers("/api/auth/forgot-password", "/api/auth/reset-password", "/api/auth/resend-verification").permitAll()
                // Customer creation form (publicly accessible)
                .requestMatchers("/customer/create", "/create").permitAll()
                // Admin only
                .requestMatchers("/admin/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")
                // Customer pages (authenticated)
                .requestMatchers("/member/**", "/api/member/**").hasAnyAuthority("ROLE_CUSTOMER", "ROLE_ADMIN")
                // Everything else requires authentication
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/member/dashboard")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .deleteCookies("jwt_token")
                .permitAll()
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
