package com.mf.HerculaneumTranscriptor.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import java.util.Collection;

public class JwtAuthentication implements Authentication {
  private final Collection<? extends GrantedAuthority> authorities;
  private final String token;
  private final UserDetails userDetails;
  private final WebAuthenticationDetails details;
  private Boolean authenticated;

  public JwtAuthentication(UserDetails userDetails, String token, WebAuthenticationDetails details) {
    authorities = userDetails.getAuthorities();
    this.token = token;
    this.userDetails = userDetails;
    this.details = details;
    authenticated = true;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public Object getCredentials() {
    return token;
  }

  @Override
  public Object getDetails() {
    return details;
  }

  @Override
  public Object getPrincipal() {
    return userDetails;
  }

  @Override
  public boolean isAuthenticated() {
    return authenticated;
  }

  @Override
  public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
    authenticated = isAuthenticated;
  }

  @Override
  public String getName() {
    return userDetails.getUsername();
  }
}
