package com.mf.HerculaneumTranscriptor.service.impl;

import com.mf.HerculaneumTranscriptor.domain.User;
import com.mf.HerculaneumTranscriptor.domain.mapper.UserMapper;
import com.mf.HerculaneumTranscriptor.dto.AuthenticationResponse;
import com.mf.HerculaneumTranscriptor.exception.ResourceNotFoundException;
import com.mf.HerculaneumTranscriptor.exception.UserAlreadyExistsException;
import com.mf.HerculaneumTranscriptor.repository.UserRepository;
import com.mf.HerculaneumTranscriptor.security.JwtUtil;
import com.mf.HerculaneumTranscriptor.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import user.dto.ChangePermissions;
import user.dto.UserInfo;
import user.dto.UserLoginInfo;
import user.dto.UserRegisterInfo;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {
  private final JwtUtil jwtUtil;
  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;

  @Override
  public AuthenticationResponse registerNewUser(UserRegisterInfo registrationInfo) throws UserAlreadyExistsException {
    // Check if username is already taken
    if (userRepository.existsByUsername(registrationInfo.getBasicInfo().getUsername())) {
      throw new UserAlreadyExistsException("Username is already taken: " + registrationInfo.getBasicInfo().getUsername());
    }

    User user = userMapper.userRegisterInfoToUser(registrationInfo);
    user.setPasswordHash(passwordEncoder.encode(registrationInfo.getPassword()));
    // New users get 'read' permissions by default
    user.setPermissions(UserInfo.PermissionsEnum.READ);

    User savedUser = userRepository.save(user);
    return new AuthenticationResponse(jwtUtil.generateToken(savedUser.getUsername()), userMapper.userToUserInfo(savedUser));
  }

  @Override
  public AuthenticationResponse login(UserLoginInfo loginInfo) throws AuthenticationException {
    return null;
  }

  @Override
  public UserInfo findUserByUsername(String username) throws ResourceNotFoundException {
    return userMapper.userToUserInfo(
        userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username))
    );
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
