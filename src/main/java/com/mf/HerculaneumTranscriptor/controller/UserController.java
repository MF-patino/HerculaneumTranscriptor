package com.mf.HerculaneumTranscriptor.controller;

import com.mf.HerculaneumTranscriptor.dto.AuthenticationResponse;
import com.mf.HerculaneumTranscriptor.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import user.api.UserApi;
import user.dto.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class UserController implements UserApi {
  private final UserService userService;

  @Override
  public ResponseEntity<UserInfo> createUser(UserRegisterInfo userRegisterInfo) {
    AuthenticationResponse authInfo = userService.registerNewUser(userRegisterInfo);

    // JWT is returned in the Authorization header
    return ResponseEntity.ok()
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + authInfo.getToken())
            .body(authInfo.getUserInfo());
  }

  @Override
  public ResponseEntity<Void> deleteUser(String username) {
    userService.deleteUserByUsername(username);

    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<UserInfo> getUserByName(String username) {
    UserInfo userInfo = userService.findUserByUsername(username);
    return ResponseEntity.ok(userInfo);
  }

  @Override
  public ResponseEntity<UserInfo> loginUser(UserLoginInfo userLoginInfo) {
    AuthenticationResponse authInfo = userService.login(userLoginInfo);

    // JWT is returned in the Authorization header
    return ResponseEntity.ok()
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + authInfo.getToken())
            .body(authInfo.getUserInfo());
  }

  @Override
  public ResponseEntity<Void> updateUser(String username, UserRegisterInfo userRegisterInfo) {
    userService.updateUserProfile(username, userRegisterInfo);

    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<Void> updateUserPermissions(String username, ChangePermissions changePermissions) {
    userService.changeUserPermissions(username, changePermissions);

    return ResponseEntity.ok().build();
  }
}
