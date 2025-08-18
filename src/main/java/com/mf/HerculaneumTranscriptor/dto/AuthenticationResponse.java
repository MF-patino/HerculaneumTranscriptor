package com.mf.HerculaneumTranscriptor.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import user.dto.UserInfo;

// This DTO encapsulates the result of a successful login
@Getter @Setter @AllArgsConstructor
public class AuthenticationResponse {
  private String token;
  private UserInfo userInfo;
}
