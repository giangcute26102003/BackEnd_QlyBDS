package com.example.datn_realeaste_crm.config;

import com.example.datn_realeaste_crm.security.*;
import com.example.datn_realeaste_crm.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtTokenFilter jwtTokenFilter;
    private final AppCheckFilter appCheckFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/api-docs/**", "/webjars/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/properties/**").permitAll()

                        // Admin access - full system access
                        .requestMatchers("/admin/**").hasRole(RoleEnum.ADMIN.name())
//                        .requestMatchers("/users/**").hasAnyRole(RoleEnum.ADMIN.name(), RoleEnum.MANAGER.name())
                        .requestMatchers(HttpMethod.POST, "/users").hasRole(RoleEnum.ADMIN.name())
                        .requestMatchers(HttpMethod.DELETE, "/users/**").hasRole(RoleEnum.ADMIN.name())
                        .requestMatchers("/users/{id}/roles").hasRole(RoleEnum.ADMIN.name())
                        .requestMatchers("/departments/**").hasRole(RoleEnum.ADMIN.name())
                        .requestMatchers("/audit-logs/**").hasRole(RoleEnum.ADMIN.name())
                        .requestMatchers("/system-config/**").hasRole(RoleEnum.ADMIN.name())

                        // District management
                        .requestMatchers(HttpMethod.GET, "/districts/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/districts/**").hasRole(RoleEnum.ADMIN.name())
                        .requestMatchers(HttpMethod.PUT, "/districts/**").hasRole(RoleEnum.ADMIN.name())
                        .requestMatchers(HttpMethod.DELETE, "/districts/**").hasRole(RoleEnum.ADMIN.name())

                        // Manager access - department management
                        .requestMatchers("/reports/department/**")
                        .hasAnyRole(RoleEnum.ADMIN.name(), RoleEnum.MANAGER.name())
                        .requestMatchers("/properties/department/**")
                        .hasAnyRole(RoleEnum.ADMIN.name(), RoleEnum.MANAGER.name())
                        .requestMatchers("/customers/department/**")
                        .hasAnyRole(RoleEnum.ADMIN.name(), RoleEnum.MANAGER.name())
                        .requestMatchers("/interactions/department/**")
                        .hasAnyRole(RoleEnum.ADMIN.name(), RoleEnum.MANAGER.name())

                        // Reviewer access
                        .requestMatchers("/properties/pending-approval/**")
                        .hasAnyRole(RoleEnum.ADMIN.name(), RoleEnum.REVIEWER.name())
                        .requestMatchers("/properties/*/approve/**")
                        .hasAnyRole(RoleEnum.ADMIN.name(), RoleEnum.REVIEWER.name())
                        .requestMatchers("/properties/*/reject/**")
                        .hasAnyRole(RoleEnum.ADMIN.name(), RoleEnum.REVIEWER.name())

                        // Property Owner access
                        .requestMatchers("/properties/owned/**").hasRole(RoleEnum.PROPERTY_OWNER.name())
                        .requestMatchers("/notifications/property-owner/**").hasRole(RoleEnum.PROPERTY_OWNER.name())

                        // Consultant access
                        .requestMatchers("/properties/assigned/**").hasRole(RoleEnum.CONSULTANT.name())
                        .requestMatchers("/customers/assigned/**").hasRole(RoleEnum.CONSULTANT.name())
                        .requestMatchers("/interactions/own/**").hasRole(RoleEnum.CONSULTANT.name())

                        // Common access
                        .requestMatchers("/users/me/**").authenticated()
                        .requestMatchers("/appointments/**").authenticated()

                        // Default - authenticated
                        .anyRequest().authenticated())
                .authenticationProvider(authenticationProvider())
//                .addFilterBefore(appCheckFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Firebase-AppCheck"));
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}