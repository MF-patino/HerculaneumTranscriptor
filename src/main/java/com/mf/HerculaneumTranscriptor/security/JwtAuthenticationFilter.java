package com.mf.HerculaneumTranscriptor.security;

import com.mf.HerculaneumTranscriptor.domain.User;
import com.mf.HerculaneumTranscriptor.exception.ResourceNotFoundException;
import com.mf.HerculaneumTranscriptor.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@AllArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtUtil jwtUtil;
  private final UserRepository userRepository;


  /**
   * Retrieves the JWT token from the Authorization header of the request, or null if
   * the header is not present or does not start with "Bearer ".
   *
   * @param request The request from which to retrieve the token.
   * @return The token if it exists, or null otherwise.
   */
  private String getTokenFromRequest(HttpServletRequest request) {
    String header = request.getHeader("Authorization");
    return (header != null && header.startsWith("Bearer ")) ? header.substring(7) : null;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
          throws ServletException, IOException {
    String token = getTokenFromRequest(request);

    if (token != null && jwtUtil.validateToken(token)) {
      // The JWT is valid, we can build an authentication object from it
      String subject = jwtUtil.extractSubject(token);

      User user = userRepository.findByUsername(subject)
              .orElseThrow(() -> new ResourceNotFoundException("User not found: " + subject));
      UserDetails userDetails = new JwtUserDetails(user);

      WebAuthenticationDetails authDetails = new WebAuthenticationDetailsSource().buildDetails(request);
      Authentication authentication = new JwtAuthentication(userDetails, token, authDetails);

      SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    chain.doFilter(request, response);
  }
}