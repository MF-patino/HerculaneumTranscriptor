package com.mf.HerculaneumTranscriptor.controller;

import user.api.UserApi;
import user.dto.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController implements UserApi {
  @Override
  public ResponseEntity<UserInfo> createUser(UserRegisterInfo userRegisterInfo) {
    return null;
  }

  @Override
  public ResponseEntity<Void> deleteUser(String username) {
    return null;
  }

  @Override
  public ResponseEntity<UserInfo> getUserByName(String username) {
    return null;
  }

  @Override
  public ResponseEntity<UserInfo> loginUser(UserLoginInfo userLoginInfo) {
    return null;
  }

  @Override
  public ResponseEntity<Void> updateUser(String username, UserRegisterInfo userRegisterInfo) {
    return null;
  }

  @Override
  public ResponseEntity<Void> updateUserPermissions(String username, ChangePermissions changePermissions) {
    return null;
  }
}
