package com.keplerops.groundcontrol.shared.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;

/**
 * Wires the API access control filter chain.
 *
 * <p>When {@code groundcontrol.security.enabled=true}, the chain runs IP allowlist → bearer token
 * authn → Spring Security authorization, with the path matrix set up so {@code /api/v1/admin/**}
 * and the privileged subpaths require {@code ROLE_ADMIN}, the rest of {@code /api/v1/**} requires
 * authentication, and only health/info actuator probes are anonymous.
 *
 * <p>When disabled, the chain still owns request mapping but every path is permitAll — the same
 * Spring Security framework object stays in charge so behavior never silently degrades.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(SecurityProperties.class)
public class ApiSecurityConfig {

    private static final String ROLE_ADMIN = "ADMIN";

    @Bean
    public BearerTokenAuthFilter bearerTokenAuthFilter(SecurityProperties properties) {
        return new BearerTokenAuthFilter(properties);
    }

    @Bean
    public IpAllowlistFilter ipAllowlistFilter(SecurityProperties properties, ObjectMapper objectMapper) {
        return new IpAllowlistFilter(properties, objectMapper);
    }

    /**
     * Spring Boot auto-registers any {@link jakarta.servlet.Filter} bean with the servlet
     * container. Both filters above are also wired into the {@link SecurityFilterChain} below;
     * leaving servlet auto-registration on would run them twice (once outside the security
     * chain). Disabling auto-registration keeps the filters scoped to the chain only.
     */
    @Bean
    public FilterRegistrationBean<BearerTokenAuthFilter> bearerTokenAuthFilterRegistration(
            BearerTokenAuthFilter filter) {
        return disableServletAutoRegistration(filter);
    }

    @Bean
    public FilterRegistrationBean<IpAllowlistFilter> ipAllowlistFilterRegistration(IpAllowlistFilter filter) {
        return disableServletAutoRegistration(filter);
    }

    private static <F extends jakarta.servlet.Filter> FilterRegistrationBean<F> disableServletAutoRegistration(
            F filter) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public ApiAuthenticationEntryPoint apiAuthenticationEntryPoint(ObjectMapper objectMapper) {
        return new ApiAuthenticationEntryPoint(objectMapper);
    }

    @Bean
    public ApiAccessDeniedHandler apiAccessDeniedHandler(ObjectMapper objectMapper) {
        return new ApiAccessDeniedHandler(objectMapper);
    }

    @Bean
    public SecurityFilterChain apiSecurityFilterChain(
            HttpSecurity http,
            SecurityProperties properties,
            BearerTokenAuthFilter bearerTokenAuthFilter,
            IpAllowlistFilter ipAllowlistFilter,
            ApiAuthenticationEntryPoint authenticationEntryPoint,
            ApiAccessDeniedHandler accessDeniedHandler)
            throws Exception {
        // CSRF is disabled by design: this is a stateless REST API authenticated by
        // Authorization: Bearer <token>. There are no session cookies, no form-login
        // flow, and no logout endpoint — the three things a CSRF attack relies on to
        // ride a user's authenticated session. Bearer tokens are explicit per request,
        // so a malicious cross-origin page cannot trigger an authenticated call from a
        // signed-in browser. ADR-026 documents this; do not re-enable CSRF without
        // simultaneously moving to cookie-based sessions and re-evaluating the model.
        http.csrf(csrf -> csrf.disable())
                // Use Spring Security's CORS support so the registered MVC CorsConfigurationSource
                // is honored on preflight requests. With cors.disable() preflight OPTIONS calls hit
                // the security chain before MVC and would be rejected for protected routes.
                .cors(Customizer.withDefaults())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .anonymous(anon -> anon.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(ipAllowlistFilter, AuthorizationFilter.class)
                .addFilterBefore(bearerTokenAuthFilter, AuthorizationFilter.class)
                .exceptionHandling(ex ->
                        ex.authenticationEntryPoint(authenticationEntryPoint).accessDeniedHandler(accessDeniedHandler));

        if (!properties.isEnabled()) {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }

        http.authorizeHttpRequests(auth -> {
            auth.requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info")
                    .permitAll()
                    .requestMatchers("/error")
                    .permitAll();
            if (properties.isOpenapiPublic()) {
                auth.requestMatchers(
                                "/api/openapi.json",
                                "/api/docs/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html")
                        .permitAll();
            } else {
                auth.requestMatchers(
                                "/api/openapi.json",
                                "/api/docs/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html")
                        .authenticated();
            }
            auth.requestMatchers("/api/v1/admin/**")
                    .hasRole(ROLE_ADMIN)
                    .requestMatchers("/api/v1/embeddings/**")
                    .hasRole(ROLE_ADMIN)
                    .requestMatchers("/api/v1/analysis/sweep/**")
                    .hasRole(ROLE_ADMIN)
                    .requestMatchers("/api/v1/pack-registry/**")
                    .hasRole(ROLE_ADMIN)
                    .requestMatchers("/api/v1/trust-policies/**")
                    .hasRole(ROLE_ADMIN)
                    .requestMatchers("/api/v1/pack-install-records/**")
                    .hasRole(ROLE_ADMIN)
                    .requestMatchers("/api/v1/**")
                    .authenticated()
                    .requestMatchers("/actuator/**")
                    .denyAll()
                    // SPA static assets and client-side routes are anonymous so the React
                    // shell loads (the API calls it then makes still require a token, returning
                    // 401 to drive the login UI). Permit only known static asset paths and a
                    // strict GET regex that mirrors SpaController's pattern (no dot in any
                    // segment, first segment not /api or /actuator). Anything outside that
                    // narrow allowlist is denyAll, so a future controller mounted outside the
                    // documented matrix is fail-closed.
                    .requestMatchers(
                            HttpMethod.GET,
                            "/",
                            "/index.html",
                            "/favicon.ico",
                            "/favicon.svg",
                            "/manifest.json",
                            "/robots.txt",
                            "/assets/**",
                            "/static/**")
                    .permitAll()
                    .requestMatchers(RegexRequestMatcher.regexMatcher(
                            HttpMethod.GET, "^/(?!api/|api$|actuator/|actuator$)[^.]+$"))
                    .permitAll()
                    .anyRequest()
                    .denyAll();
        });

        return http.build();
    }
}
