package com.mf.HerculaneumTranscriptor.configuration;

import com.mf.HerculaneumTranscriptor.security.JwtAuthenticationFilter;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.crypto.SecretKey;

@Configuration
public class SecurityConfiguration {
  @Value( "${security.secret}" )
  private String jwtSecret;

  @Bean
  public SecretKey secretKey() {
    byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
    return Keys.hmacShaKeyFor(keyBytes);
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http,
                                         JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception{
    return http
            .csrf(AbstractHttpConfigurer::disable) // CSRF is disabled as JWTs are in headers
            // We are using JWT, so sessions are stateless
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests((auth) -> auth
                    // Defining public endpoints
                    .requestMatchers("/register").permitAll()
                    .requestMatchers(HttpMethod.POST,"/user").permitAll() // Matches the POST login endpoint

                    // Securing other endpoints
                    // Must have 'ADMIN' role/authority to change permissions
                    .requestMatchers(HttpMethod.PUT, "/permissions/{username}").hasAuthority("ROLE_ADMIN")
                    // Any other request must be authenticated
                    .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
  }
}
