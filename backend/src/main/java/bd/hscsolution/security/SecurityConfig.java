package bd.hscsolution.security;

import java.util.Arrays;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {
  @Bean PasswordEncoder passwordEncoder(){ return new BCryptPasswordEncoder(12); }

  @Bean SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter,
      JsonAuthenticationEntryPoint entryPoint, JsonAccessDeniedHandler deniedHandler) throws Exception {
    http.csrf(csrf -> csrf.disable())
      .cors(cors -> {})
      .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint).accessDeniedHandler(deniedHandler))
      .authorizeHttpRequests(a -> a
        .requestMatchers("/api/v1/auth/**", "/api/v1/catalog/**", "/api/v1/health", "/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/actuator/health").permitAll()
        .requestMatchers(HttpMethod.GET, "/api/v1/admin/audit/**").hasAuthority("PERM_AUDIT_READ")
        .anyRequest().authenticated())
      .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  @Bean CorsConfigurationSource corsConfigurationSource(SecurityProperties p){
    CorsConfiguration c = new CorsConfiguration();
    c.setAllowedOrigins(Arrays.stream(p.corsOrigins().split(",")).map(String::trim).filter(s->!s.isBlank()).toList());
    c.setAllowedMethods(Arrays.asList("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
    c.setAllowedHeaders(Arrays.asList("Authorization","Content-Type","X-Correlation-Id","X-Client-Type"));
    c.setAllowCredentials(true);
    c.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", c); return source;
  }
}
