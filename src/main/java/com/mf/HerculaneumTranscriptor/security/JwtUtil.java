package com.mf.HerculaneumTranscriptor.security;

import com.mf.HerculaneumTranscriptor.service.UserService;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import user.dto.UserInfo;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtUtil {
  @Value( "${security.expiration}" )
  private Integer expirationTime;
  private final SecretKey secretKey;
  private final UserService userService;

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
   * Given a JWT token, extract the associated UserDetails object.
   *
   * @param token the JWT token
   * @return the associated UserDetails object
   */
  public UserDetails extractDetails(String token) {
    String subject = Jwts.parser()
                      .verifyWith(secretKey).build()
                      .parseSignedClaims(token)
                      .getPayload()
                      .getSubject();

    UserInfo userInfo = userService.findUserByUsername(subject);
    return new JwtUserDetails(userInfo);
  }
}
