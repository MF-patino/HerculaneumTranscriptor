package com.mf.HerculaneumTranscriptor.service.impl;

import com.mf.HerculaneumTranscriptor.domain.User;
import com.mf.HerculaneumTranscriptor.domain.mapper.UserMapper;
import com.mf.HerculaneumTranscriptor.dto.AuthenticationResponse;
import com.mf.HerculaneumTranscriptor.exception.ResourceNotFoundException;
import com.mf.HerculaneumTranscriptor.exception.ResourceAlreadyExistsException;
import com.mf.HerculaneumTranscriptor.repository.UserRepository;
import com.mf.HerculaneumTranscriptor.security.JwtUtil;
import com.mf.HerculaneumTranscriptor.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import user.dto.*;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
  @Value( "${api.user.pageSize}" )
  private Integer PAGE_SIZE;

  private final JwtUtil jwtUtil;
  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;

  @Override
  public AuthenticationResponse registerNewUser(UserRegisterInfo registrationInfo) throws ResourceAlreadyExistsException {
    // Check if username is already taken
    if (userRepository.existsByUsername(registrationInfo.getBasicInfo().getUsername()))
      throw new ResourceAlreadyExistsException("Username is already taken: " + registrationInfo.getBasicInfo().getUsername());

    User user = userMapper.userRegisterInfoToUser(registrationInfo);
    user.setPasswordHash(passwordEncoder.encode(registrationInfo.getPassword()));
    // New users get 'read' permissions by default
    user.setPermissions(UserInfo.PermissionsEnum.READ);

    User savedUser = userRepository.save(user);
    return new AuthenticationResponse(jwtUtil.generateToken(savedUser.getUsername()), userMapper.userToUserInfo(savedUser));
  }

  @Override
  public AuthenticationResponse login(UserLoginInfo loginInfo) throws AuthenticationException {
    AuthenticationException exception = new BadCredentialsException("Incorrect username or password");

    User user = userRepository.findByUsername(loginInfo.getUserName())
        .orElseThrow(() -> exception);

    if (!passwordEncoder.matches(loginInfo.getPassword(), user.getPasswordHash())) {
      throw exception;
    }

    return new AuthenticationResponse(jwtUtil.generateToken(user.getUsername()), userMapper.userToUserInfo(user));
  }

  @Override
  public UserInfo findUserByUsername(String username) throws ResourceNotFoundException {
    return userMapper.userToUserInfo(
        userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username))
    );
  }

  @Override
  public List<UserInfo> findAllUsers(Integer index) {
    index = index == null ? 0 : index;
    int pageNumber = index / PAGE_SIZE;

    // Retrieve a page of users
    Pageable pageable = PageRequest.of(pageNumber, PAGE_SIZE);
    Page<User> userPage = userRepository.findAll(pageable);

    // Convert the content of the page to a list of DTOs.
    return userPage.getContent().stream()
            .map(userMapper::userToUserInfo)
            .collect(Collectors.toList());
  }

  @Override
  public void deleteUserByUsername(String username) throws ResourceNotFoundException {
    User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

    userRepository.delete(user);
  }

  @Override
  public void updateUserProfile(String username, ChangeUserInfo updateInfo) throws ResourceNotFoundException, ResourceAlreadyExistsException {
    User originalUser = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

    if (updateInfo.getPassword() != null) // change of password
      originalUser.setPasswordHash(passwordEncoder.encode(updateInfo.getPassword()));
    else { // change of personal information
      // Check if desired new username is already taken
      String requestedUsername = updateInfo.getBasicInfo().getUsername();
      if (!username.equals(requestedUsername) && userRepository.existsByUsername(requestedUsername))
        throw new ResourceAlreadyExistsException("New username is already taken: " + requestedUsername);

      originalUser.setUsername(updateInfo.getBasicInfo().getUsername());
      originalUser.setFirstName(updateInfo.getBasicInfo().getFirstName());
      originalUser.setLastName(updateInfo.getBasicInfo().getLastName());
      originalUser.setContact(updateInfo.getBasicInfo().getContact());
    }

    // Update user entry
    userRepository.save(originalUser);
  }

  @Override
  public void changeUserPermissions(String username, ChangePermissions newPermissions) {
    User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

    // We perform a "cast" from the more limited ChangePermissions.PermissionsEnum to
    // the broader UserInfo.PermissionsEnum enum
    String newPermissionName = newPermissions.getPermissions().name();
    user.setPermissions(UserInfo.PermissionsEnum.valueOf(newPermissionName));

    // Update user entry
    userRepository.save(user);
  }
}
