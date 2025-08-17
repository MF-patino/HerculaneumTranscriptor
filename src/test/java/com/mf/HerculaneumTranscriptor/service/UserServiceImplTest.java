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
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import user.dto.*;

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

  // Reusable test data objects
  private User user;
  private UserInfo userInfo;
  private UserRegisterInfo userRegisterInfo;
  private UserLoginInfo userLoginInfo;

  // Constants for test data objects
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

    userLoginInfo = new UserLoginInfo();
    userLoginInfo.setUserName(USERNAME);
    userLoginInfo.setPassword(PASSWORD);
  }

  // Tests for login

  @Test
  void login_shouldReturnAuthenticationResponse_whenCredentialsAreValid() {
    // Arrange
    // Mock the password checker to flag passwords as matching
    when(passwordEncoder.matches(anyString(), anyString()))
            .thenReturn(true);
    when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
    when(userMapper.userToUserInfo(user)).thenReturn(userInfo);

    // Act
    AuthenticationResponse response = userService.login(userLoginInfo);

    // Assert
    assertThat(response.getUserInfo()).isEqualTo(userInfo);
  }

  @Test
  void login_shouldThrowBadCredentialsException_whenCredentialsAreInvalid() {
    // Arrange
    // Mock the user being correctly found
    when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
    // Mock the password checker to flag passwords as different
    when(passwordEncoder.matches(anyString(), anyString()))
            .thenReturn(false);

    // Act & Assert
    assertThrows(BadCredentialsException.class, () -> userService.login(userLoginInfo));
  }

  @Test
  void login_shouldThrowBadCredentialsException_whenUserNotFound() {
    // Arrange
    // Mock the password checker to detect different passwords
    when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

    // Act & Assert
    // Assert that the specific security exception is thrown
    assertThrows(BadCredentialsException.class, () -> userService.login(userLoginInfo));
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

  // Tests for deleteUserByUsername

  @Test
  @WithMockUser(username = USERNAME, roles = {"ADMIN"})
  void deleteUserByUsername_shouldDeleteUser_whenUserExists() {
    // Arrange
    when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
    doNothing().when(userRepository).delete(any(User.class));

    // Act
    userService.deleteUserByUsername(USERNAME);

    // Assert
    // Verify that the delete method was called exactly once with the correct user object
    verify(userRepository, times(1)).delete(user);
  }

  @Test
  @WithMockUser(username = USERNAME, roles = {"ADMIN"})
  void deleteUserByUsername_shouldThrowResourceNotFoundException_whenUserDoesNotExist() {
    // Arrange
    when(userRepository.findByUsername(UNK_USERNAME)).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(ResourceNotFoundException.class, () -> userService.deleteUserByUsername(UNK_USERNAME));

    // Verify that the delete method was never called
    verify(userRepository, never()).delete(any());
  }


  // Tests for updateUserProfile

  @Test
  @WithMockUser(username = USERNAME, roles = {"READ"})
  void updateUserProfile_shouldUpdateInfo_whenPasswordIsNull() {
    // Arrange
    // Password is null in this DTO, and all personal information is different
    ChangeUserInfo updateInfo = new ChangeUserInfo();
    BasicUserInfo newBasicInfo = new BasicUserInfo();
    newBasicInfo.setUsername("NewJohnDoe");
    newBasicInfo.setFirstName("Jonathan");
    newBasicInfo.setLastName("Doer");
    newBasicInfo.setContact("jonathan.doer@example.com");
    updateInfo.setBasicInfo(newBasicInfo);

    User mappedUpdateUser = new User();
    mappedUpdateUser.setUsername(newBasicInfo.getUsername());
    mappedUpdateUser.setFirstName(newBasicInfo.getFirstName());
    mappedUpdateUser.setLastName(newBasicInfo.getLastName());
    mappedUpdateUser.setContact(newBasicInfo.getContact());

    when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
    when(userRepository.existsByUsername("NewJohnDoe")).thenReturn(false);

    // Act
    userService.updateUserProfile(USERNAME, updateInfo);

    // Assert
    // Use ArgumentCaptor to capture the object passed to the save method
    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(userCaptor.capture());
    User savedUser = userCaptor.getValue();

    // Verify that personal info was updated but the rest of the user data
    // was left unchanged (password and permissions)
    mappedUpdateUser.setPasswordHash(user.getPasswordHash());
    mappedUpdateUser.setPermissions(user.getPermissions());

    assertThat(savedUser).isEqualTo(mappedUpdateUser);
    verify(passwordEncoder, never()).encode(anyString());
  }

  @Test
  @WithMockUser(username = USERNAME, roles = {"READ"})
  void updateUserProfile_shouldUpdatePassword_whenPasswordIsProvided() {
    // Arrange
    ChangeUserInfo updateInfo = new ChangeUserInfo();
    updateInfo.setPassword("newPassword456");

    when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
    when(passwordEncoder.encode("newPassword456")).thenReturn("newEncodedPassword");

    // Act
    userService.updateUserProfile(USERNAME, updateInfo);

    // Assert
    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(userCaptor.capture());
    User savedUser = userCaptor.getValue();

    // Verify that password was updated but personal info was NOT
    assertThat(savedUser.getPasswordHash()).isEqualTo("newEncodedPassword");
    assertThat(savedUser.getUsername()).isEqualTo(USERNAME); // Unchanged
  }

  @Test
  @WithMockUser(username = USERNAME, roles = {"ADMIN"})
  void updateUserProfile_shouldThrowUserAlreadyExistsException_whenNewUsernameIsTaken() {
    // Arrange
    ChangeUserInfo updateInfo = new ChangeUserInfo();
    BasicUserInfo newBasicInfo = new BasicUserInfo();
    newBasicInfo.setUsername("ExistingUser");
    updateInfo.setBasicInfo(newBasicInfo);

    when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
    // Simulate that the desired new username is already taken
    when(userRepository.existsByUsername("ExistingUser")).thenReturn(true);

    // Act & Assert
    assertThrows(UserAlreadyExistsException.class, () -> userService.updateUserProfile(USERNAME, updateInfo));

    // Verify that save was never called
    verify(userRepository, never()).save(any());
  }


  // Tests for changeUserPermissions

  @Test
  @WithMockUser(username = USERNAME, roles = {"ADMIN"})
  void changeUserPermissions_shouldUpdatePermissions_whenUserExists() {
    // Arrange
    ChangePermissions newPermissionsDto = new ChangePermissions();
    newPermissionsDto.setPermissions(ChangePermissions.PermissionsEnum.ADMIN);

    when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

    // Act
    userService.changeUserPermissions(USERNAME, newPermissionsDto);

    // Assert
    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(userCaptor.capture());
    User savedUser = userCaptor.getValue();

    // The original user had 'READ' permissions. Verify it changed to 'ADMIN'.
    assertThat(savedUser.getPermissions()).isEqualTo(UserInfo.PermissionsEnum.ADMIN);
  }

  @Test
  @WithMockUser(username = USERNAME, roles = {"ADMIN"})
  void changeUserPermissions_shouldThrowResourceNotFoundException_whenUserDoesNotExist() {
    // Arrange
    ChangePermissions newPermissionsDto = new ChangePermissions();
    newPermissionsDto.setPermissions(ChangePermissions.PermissionsEnum.ADMIN);
    when(userRepository.findByUsername(UNK_USERNAME)).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(ResourceNotFoundException.class, () -> userService.changeUserPermissions(UNK_USERNAME, newPermissionsDto));

    // Verify that save was never called
    verify(userRepository, never()).save(any());
  }
}
