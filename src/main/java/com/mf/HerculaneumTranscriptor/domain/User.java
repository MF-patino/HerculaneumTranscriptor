package com.mf.HerculaneumTranscriptor.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import user.dto.UserInfo;

import java.util.Objects;

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

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    User user = (User) o;
    return Objects.equals(username, user.username) && Objects.equals(firstName, user.firstName) && Objects.equals(lastName, user.lastName) && Objects.equals(contact, user.contact) && Objects.equals(passwordHash, user.passwordHash) && permissions == user.permissions;
  }

  @Override
  public int hashCode() {
    return Objects.hash(username, firstName, lastName, contact, passwordHash, permissions);
  }
}
