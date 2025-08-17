package com.mf.HerculaneumTranscriptor.security;

import com.mf.HerculaneumTranscriptor.domain.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class JwtUserDetails implements UserDetails {
  private final String username;
  private final List<GrantedAuthority> authorities;

  public JwtUserDetails(User user) {
    this.username = user.getUsername();
    this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getPermissions().getValue()));
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public String getPassword() {
    return ""; // no need for password
  }

  @Override
  public String getUsername() {
    return username;
  }
}
