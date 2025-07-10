package com.mf.HerculaneumTranscriptor.service.impl;

import com.mf.HerculaneumTranscriptor.dto.AuthenticationResponse;
import com.mf.HerculaneumTranscriptor.exception.ResourceNotFoundException;
import com.mf.HerculaneumTranscriptor.exception.UserAlreadyExistsException;
import com.mf.HerculaneumTranscriptor.service.UserService;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import user.dto.ChangePermissions;
import user.dto.UserInfo;
import user.dto.UserLoginInfo;
import user.dto.UserRegisterInfo;

@Service
public class UserServiceImpl implements UserService {
  @Override
  public UserInfo registerNewUser(UserRegisterInfo registrationInfo) throws UserAlreadyExistsException {
    return null;
  }

  @Override
  public AuthenticationResponse login(UserLoginInfo loginInfo) throws AuthenticationException {
    return null;
  }

  @Override
  public UserInfo findUserByUsername(String username) throws ResourceNotFoundException {
    return null;
  }

  @Override
  public void deleteUserByUsername(String username) throws ResourceNotFoundException {

  }

  @Override
  public void updateUserProfile(String username, UserRegisterInfo updateInfo) throws ResourceNotFoundException {

  }

  @Override
  public void changeUserPermissions(String username, ChangePermissions newPermissions) {

  }
}
