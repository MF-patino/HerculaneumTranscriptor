package com.mf.HerculaneumTranscriptor.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mf.HerculaneumTranscriptor.configuration.SecurityConfiguration;
import com.mf.HerculaneumTranscriptor.domain.User;
import com.mf.HerculaneumTranscriptor.dto.AuthenticationResponse;
import com.mf.HerculaneumTranscriptor.exception.ResourceNotFoundException;
import com.mf.HerculaneumTranscriptor.exception.UserAlreadyExistsException;
import com.mf.HerculaneumTranscriptor.repository.UserRepository;
import com.mf.HerculaneumTranscriptor.security.JwtUtil;
import com.mf.HerculaneumTranscriptor.service.UserService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import user.dto.*;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
// SecurityConfiguration is needed to test the JWT filters
// JwtUtil is needed to generate JWT tokens
@Import({SecurityConfiguration.class, JwtUtil.class})
@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
public class UserControllerTest {
  // MockMvc allows to send simulated HTTP requests.
  private final MockMvc mockMvc;
  // Helper for converting objects to JSON strings
  private final ObjectMapper objectMapper;
  // Needed to generate JWT tokens
  private final JwtUtil jwtUtil;

  // Mock of the service layer and repository.
  @MockitoBean
  private UserService userService;
  @MockitoBean
  private UserRepository userRepository;

  private AuthenticationResponse authResponse;
  private User user;
  private UserInfo userInfo;
  private UserRegisterInfo registerInfo;
  private ChangeUserInfo changeInfo;
  private ChangePermissions changePermissions;
  private UserLoginInfo loginInfo;

  private static final String USERNAME = "JohnDoe";
  @BeforeEach
  void setUp() {
    userInfo = new UserInfo();
    BasicUserInfo basicInfo = new BasicUserInfo();
    basicInfo.setUsername(USERNAME);
    basicInfo.setContact("john.doe@example.com");
    basicInfo.setFirstName("John");
    basicInfo.setLastName("Doe");
    userInfo.setBasicInfo(basicInfo);
    userInfo.setPermissions(UserInfo.PermissionsEnum.READ);

    authResponse = new AuthenticationResponse("token", userInfo);

    registerInfo = new UserRegisterInfo();
    registerInfo.setBasicInfo(basicInfo);
    registerInfo.setPassword("password");

    changeInfo = new ChangeUserInfo();
    changeInfo.setBasicInfo(basicInfo);

    changePermissions = new ChangePermissions();
    changePermissions.setPermissions(ChangePermissions.PermissionsEnum.READ);

    user = new User();
    user.setId(1L);
    user.setUsername(basicInfo.getUsername());
    user.setContact(basicInfo.getContact());
    user.setFirstName(basicInfo.getFirstName());
    user.setLastName(basicInfo.getLastName());
    user.setPermissions(userInfo.getPermissions());
    user.setPasswordHash("password_hash");

    loginInfo = new UserLoginInfo();
    loginInfo.setUserName(basicInfo.getUsername());
    loginInfo.setPassword(registerInfo.getPassword());
  }

  @Test
  void findUserByName_shouldReturnUserInfo_whenUserExists() throws Exception {
    // Arrange
    when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

    when(userService.findUserByUsername(USERNAME)).thenReturn(userInfo);
    String token = jwtUtil.generateToken(USERNAME);

    // Act & Assert
    mockMvc.perform(get("/user/{username}", USERNAME).header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            // Check that the response body contains the correct user info
            .andExpect(jsonPath("$.basic_info.username").value(USERNAME));

    verify(userService, times(1)).findUserByUsername(USERNAME);
  }


  @Test
  void findUserByName_shouldReturnNotFound_whenUserDoesNotExists() throws Exception {
    // Arrange
    when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

    when(userService.findUserByUsername(USERNAME)).thenThrow(new ResourceNotFoundException("User not found"));
    String token = jwtUtil.generateToken(USERNAME);

    // Act & Assert
    mockMvc.perform(get("/user/{username}", USERNAME).header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound());
  }

  @Test
  void findUserByName_shouldReturn403_whenNotAuthenticated() throws Exception {
    // Act & Assert
    mockMvc.perform(get("/user/{username}", USERNAME))
            .andExpect(status().isForbidden());
  }

  @Test
  void registerNewUser_shouldReturnOk_withValidInfo() throws Exception {
    // Arrange
    when(userService.registerNewUser(any(UserRegisterInfo.class))).thenReturn(authResponse);

    // Act & Assert
    mockMvc.perform(post("/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerInfo))
            )
            .andExpect(status().isOk());

    verify(userService, times(1)).registerNewUser(registerInfo);
  }

  @Test
  void registerNewUser_shouldReturnConflict_ifUsernameAlreadyExists() throws Exception {
    // Arrange
    when(userService.registerNewUser(any(UserRegisterInfo.class))).thenThrow(new UserAlreadyExistsException("Username already exists"));

    // Act & Assert
    mockMvc.perform(post("/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerInfo))
            )
            .andExpect(status().isConflict());
  }


  @Test
  void loginUser_shouldReturnOkAndToken_whenCredentialsAreValid() throws Exception {
    // Arrange
    when(userService.login(any(UserLoginInfo.class))).thenReturn(authResponse);

    // Act & Assert
    mockMvc.perform(post("/user")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginInfo)))
            .andExpect(status().isOk())
            // Check that the response body contains the correct user info
            .andExpect(jsonPath("$.basic_info.username").value(USERNAME))
            // Check that the Authorization header is present in the response
            .andExpect(header().exists("Authorization"));

    verify(userService, times(1)).login(loginInfo);
  }

  @Test
  void loginUser_shouldReturn401_whenCredentialsAreInvalid() throws Exception {
    // Arrange
    when(userService.login(any(UserLoginInfo.class))).thenThrow(new BadCredentialsException("Invalid credentials"));

    // Act & Assert
    mockMvc.perform(post("/user")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginInfo)))
            .andExpect(status().isUnauthorized());
  }

  @Test
  void deleteUser_shouldReturnOk_whenUserExists() throws Exception {
    // Arrange
    when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

    doNothing().when(userService).deleteUserByUsername(USERNAME);
    String token = jwtUtil.generateToken(USERNAME);

    // Act & Assert
    mockMvc.perform(delete("/user/{username}", USERNAME)
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());

    verify(userService, times(1)).deleteUserByUsername(USERNAME);
  }


  @Test
  void deleteUser_shouldReturnNotFound_whenUserDoesNotExist() throws Exception {
    // Arrange
    when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

    doThrow(new ResourceNotFoundException("User not found")).when(userService).deleteUserByUsername(USERNAME);
    String token = jwtUtil.generateToken(USERNAME);

    // Act & Assert
    mockMvc.perform(delete("/user/{username}", USERNAME)
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound());
  }

  @Test
  void deleteUser_shouldReturn403_whenNotAuthenticated() throws Exception {
    // Arrange
    when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

    doNothing().when(userService).deleteUserByUsername(USERNAME);

    // Act & Assert
    mockMvc.perform(delete("/user/{username}", USERNAME))
            .andExpect(status().isForbidden());
  }

  @Test
  void updateUserProfile_shouldReturnOk_whenUserExists() throws Exception {
    // Arrange
    when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

    doNothing().when(userService).updateUserProfile(USERNAME, changeInfo);
    String token = jwtUtil.generateToken(USERNAME);

    // Act & Assert
    mockMvc.perform(put("/user/{username}", USERNAME)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(changeInfo))
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());

    verify(userService, times(1)).updateUserProfile(USERNAME, changeInfo);
  }

  @Test
  void updateUserProfile_shouldReturnNotFound_whenUserDoesNotExist() throws Exception {
    // Arrange
    when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

    doThrow(new ResourceNotFoundException("User not found")).when(userService).updateUserProfile(USERNAME, changeInfo);
    String token = jwtUtil.generateToken(USERNAME);

    // Act & Assert
    mockMvc.perform(put("/user/{username}", USERNAME)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(changeInfo))
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound());
  }

  @Test
  void updateUserProfile_shouldReturnConflict_whenUsernameAlreadyExists() throws Exception {
    // Arrange
    when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

    doThrow(new UserAlreadyExistsException("Username already exists")).when(userService).updateUserProfile(USERNAME, changeInfo);
    String token = jwtUtil.generateToken(USERNAME);

    // Act & Assert
    mockMvc.perform(put("/user/{username}", USERNAME)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(changeInfo))
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isConflict());
  }

  @Test
  void updateUserProfile_shouldReturn403_whenNotAuthenticated() throws Exception {
    // Arrange
    when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

    doNothing().when(userService).updateUserProfile(USERNAME, changeInfo);

    // Act & Assert
    mockMvc.perform(put("/user/{username}", USERNAME)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(changeInfo)))
            .andExpect(status().isForbidden());
  }

  @Test
  void updateUserPermissions_shouldReturnOk_whenUserExists() throws Exception {
    // Arrange
    when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

    doNothing().when(userService).changeUserPermissions(USERNAME, changePermissions);
    String token = jwtUtil.generateToken(USERNAME);

    // Act & Assert
    mockMvc.perform(put("/permissions/{username}", USERNAME)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(changePermissions))
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());

    verify(userService, times(1)).changeUserPermissions(USERNAME, changePermissions);
  }

  @Test
  void updateUserPermissions_shouldReturnNotFound_whenUserDoesNotExist() throws Exception {
    // Arrange
    when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

    doThrow(new ResourceNotFoundException("User not found")).when(userService).changeUserPermissions(USERNAME, changePermissions);
    String token = jwtUtil.generateToken(USERNAME);

    // Act & Assert
    mockMvc.perform(put("/permissions/{username}", USERNAME)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(changePermissions))
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound());
  }

  @Test
  void updateUserPermissions_shouldReturn403_whenNotAuthenticated() throws Exception {
    // Arrange
    when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

    doNothing().when(userService).changeUserPermissions(USERNAME, changePermissions);

    // Act & Assert
    mockMvc.perform(put("/permissions/{username}", USERNAME)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(changePermissions)))
            .andExpect(status().isForbidden());
  }

  @Test
  void findAllUsers_shouldReturnOk_whenAuthenticated() throws Exception {
    // Arrange
    when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

    List users = List.of(userInfo);
    when(userService.findAllUsers(any())).thenReturn(users);
    String token = jwtUtil.generateToken(USERNAME);

    // Act & Assert
    mockMvc.perform(get("/user")
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            // Check that the response body contains the correct user info
            .andExpect(jsonPath("$[0].basic_info.username").value(USERNAME));

    verify(userService, times(1)).findAllUsers(any());
  }

  @Test
  void findAllUsers_shouldReturn403_whenNotAuthenticated() throws Exception {
    // Arrange
    when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

    List users = List.of(userInfo);
    when(userService.findAllUsers(anyInt())).thenReturn(users);

    // Act & Assert
    mockMvc.perform(get("/user"))
            .andExpect(status().isForbidden());
  }
}
