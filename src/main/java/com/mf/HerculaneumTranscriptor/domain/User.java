package com.mf.HerculaneumTranscriptor.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import user.dto.UserInfo;

@Entity @Table(name="USERS")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class User {
  @Id
  @GeneratedValue(strategy= GenerationType.AUTO)
  private Long id;

  private String username;
  private String firstName;
  private String lastName;
  private String contact;
  private String passwordHash;
  private UserInfo.PermissionsEnum permissions;
}
