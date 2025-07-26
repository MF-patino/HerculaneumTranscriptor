package com.mf.HerculaneumTranscriptor.service;

import com.mf.HerculaneumTranscriptor.domain.User;
import com.mf.HerculaneumTranscriptor.domain.mapper.UserMapper;
import com.mf.HerculaneumTranscriptor.dto.AuthenticationResponse;
import com.mf.HerculaneumTranscriptor.exception.ResourceNotFoundException;
import com.mf.HerculaneumTranscriptor.exception.UserAlreadyExistsException;
import com.mf.HerculaneumTranscriptor.repository.UserRepository;
import com.mf.HerculaneumTranscriptor.service.impl.UserServiceImpl;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import user.dto.BasicUserInfo;
import user.dto.UserInfo;
import user.dto.UserLoginInfo;
import user.dto.UserRegisterInfo;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class) // Enables MockBean annotations
@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
public class UserServiceImplTest {
  private final UserServiceImpl userService;

  // Create mocks for all dependencies of the class under test.
  @MockitoBean
  private UserRepository userRepository;

  @MockitoBean
  private UserMapper userMapper;

  @MockitoBean
  private PasswordEncoder passwordEncoder;

  private User user;
  private UserInfo userInfo;
  private UserRegisterInfo userRegisterInfo;

  private static final String ENCODED_PASSWORD = "encodedPassword";
  private static final String PASSWORD = "password123";
  private static final String USERNAME = "JohnDoe";
  private static final String CONTACT = "john.doe@example.com";
  private static final String FIRST_NAME = "John";
  private static final String LAST_NAME = "Doe";
  private static final String UNK_USERNAME = "UnknownUser";

  @BeforeEach
  void setUp() {
    // Create user entity
    user = new User();
    user.setId(1L);
    user.setUsername(USERNAME);
    user.setContact(CONTACT);
    user.setFirstName(FIRST_NAME);
    user.setLastName(LAST_NAME);
    user.setPermissions(UserInfo.PermissionsEnum.READ);
    user.setPasswordHash(ENCODED_PASSWORD);

    // Create DTOs
    BasicUserInfo basicInfo = new BasicUserInfo();
    basicInfo.setUsername(USERNAME);
    basicInfo.setContact(CONTACT);
    basicInfo.setFirstName(FIRST_NAME);
    basicInfo.setLastName(LAST_NAME);

    userInfo = new UserInfo();
    userInfo.setBasicInfo(basicInfo);
    userInfo.setPermissions(UserInfo.PermissionsEnum.READ);

    userRegisterInfo = new UserRegisterInfo();
    userRegisterInfo.setBasicInfo(basicInfo);
    userRegisterInfo.setPassword(PASSWORD);
  }

  // Tests for registerNewUser

  @Test
  void registerNewUser_shouldCreateAndReturnUser_whenUsernameIsAvailable() {
    // Arrange

    // When checking if user exists, say no.
    when(userRepository.existsByUsername(USERNAME)).thenReturn(false);

    // Mocking non-business logic methods
    when(userMapper.userRegisterInfoToUser(any(UserRegisterInfo.class))).thenReturn(user);
    when(passwordEncoder.encode(PASSWORD)).thenReturn(ENCODED_PASSWORD);
    when(userRepository.save(any(User.class))).thenReturn(user);
    when(userMapper.userToUserInfo(any(User.class))).thenReturn(userInfo);

    // Act
    AuthenticationResponse registerInfo = userService.registerNewUser(userRegisterInfo);
    UserInfo createdUser = registerInfo.getUserInfo();

    // Assert
    assertThat(createdUser).isEqualTo(userInfo);
  }

  @Test
  void registerNewUser_shouldThrowUserAlreadyExistsException_whenUsernameIsTaken() {
    // Arrange
    // When checking if the user exists, say yes.
    when(userRepository.existsByUsername(USERNAME)).thenReturn(true);

    // Act & Assert that the expected exception is thrown
    assertThrows(UserAlreadyExistsException.class, () -> userService.registerNewUser(userRegisterInfo));

    // Verify that the critical method "save" was never called
    verify(userRepository, never()).save(any());
  }

  // Tests for findUserByUsername

  @Test
  void findUserByUsername_shouldReturnUserInfo_whenUserExists() {
    // Arrange
    when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
    when(userMapper.userToUserInfo(any(User.class))).thenReturn(userInfo);

    // Act
    UserInfo foundUser = userService.findUserByUsername(USERNAME);

    // Assert
    assertThat(foundUser).isNotNull();
    assertThat(foundUser).isEqualTo(userInfo);
  }


  @Test
  void findUserByUsername_shouldThrowResourceNotFoundException_whenUserDoesNotExist() {
    // Arrange
    when(userRepository.findByUsername(UNK_USERNAME)).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(ResourceNotFoundException.class, () -> userService.findUserByUsername(UNK_USERNAME));
  }
}
