package br.gov.quixada.sports.security.config;

import br.gov.quixada.sports.security.jwt.TokenProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({TokenProperties.class, CorsProperties.class, LoginRateLimitProperties.class})
@RequiredArgsConstructor
public class SecurityConfig {

    private static final List<String> CORS_METHODS =
            List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    private static final List<String> CORS_HEADERS = List.of("Authorization", "Content-Type");

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity,
                                                   CorsConfigurationSource corsConfigurationSource) {

        return httpSecurity
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/users/audit").hasRole("SUPERADMIN")
                        .requestMatchers(HttpMethod.GET, "/v1/users", "/v1/users/**")
                        .hasAnyRole("ADMIN", "SUPERADMIN")
                        .requestMatchers("/v1/users", "/v1/users/**").hasRole("SUPERADMIN")

                        .requestMatchers(HttpMethod.GET, "/v1/**")
                        .hasAnyRole("ADMIN", "USER")

                        .requestMatchers(HttpMethod.POST, "/v1/**")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.PATCH, "/v1/**")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.PUT, "/v1/**")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.DELETE, "/v1/**")
                        .hasRole("ADMIN")

                        .anyRequest().hasRole("USER"))

                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.allowedOrigins());
        configuration.setAllowedMethods(CORS_METHODS);
        configuration.setAllowedHeaders(CORS_HEADERS);
        configuration.setExposedHeaders(List.of("Location"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
