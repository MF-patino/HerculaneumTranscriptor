package com.mf.HerculaneumTranscriptor.security;

import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtUtil {
  @Value( "${security.expiration}" )
  private Integer expirationTime;
  private final SecretKey secretKey;

  /**
   * Generates a JWT token for a given username.
   *
   * @param username the username for which the token is to be generated
   * @return a JWT token string that is signed and set to expire in 1 hour
   */
  public String generateToken(String username) {
    return Jwts.builder()
            .subject(username)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expirationTime))
            .signWith(secretKey)
            .compact();
  }

  /**
   * Verifies that a JWT token is valid and has not expired.
   *
   * @param token the JWT token to validate
   * @return true if the token is valid, false otherwise
   */
  public boolean validateToken(String token) {
    try {
      Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Extracts the subject from a JWT token.
   * The subject contains the username of the user to which the token belongs.
   *
   * @param token the JWT token from which to extract the subject
   * @return the subject claim extracted from the token
   */
  public String extractSubject(String token) {
     return Jwts.parser()
                .verifyWith(secretKey).build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
  }
}
