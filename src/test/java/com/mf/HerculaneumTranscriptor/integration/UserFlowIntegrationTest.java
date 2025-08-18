package com.mf.HerculaneumTranscriptor.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mf.HerculaneumTranscriptor.domain.User;
import com.mf.HerculaneumTranscriptor.repository.UserRepository;
import com.mf.HerculaneumTranscriptor.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import user.dto.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional // Ensures each test runs in its own transaction and is rolled back.
@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
public class UserFlowIntegrationTest {
  private final MockMvc mockMvc;
  private final JwtUtil jwtUtil;
  private final ObjectMapper objectMapper;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  private ChangeUserInfo updateRequest;

  private String rootToken;
  private String adminToken;
  private String userToken;

  private final String rootUsername = "root";
  private final String adminUsername = "admin";
  private final String userUsername = "user";
  private final String RAW_PASSWORD = "password";

  @BeforeEach
  void setUp() {
    updateRequest = new ChangeUserInfo();
    BasicUserInfo updatedInfo = new BasicUserInfo()
            .username("user-new-name")
            .firstName("Updated")
            .lastName("User")
            .contact("new.email@example.com");
    updateRequest.setBasicInfo(updatedInfo);

    User rootUser = new User();
    rootUser.setUsername("root");
    rootUser.setPermissions(UserInfo.PermissionsEnum.ROOT);
    rootUser.setPasswordHash(passwordEncoder.encode(RAW_PASSWORD));
    userRepository.save(rootUser);

    User adminUser = new User();
    adminUser.setUsername("admin");
    adminUser.setPermissions(UserInfo.PermissionsEnum.ADMIN);
    adminUser.setPasswordHash(passwordEncoder.encode(RAW_PASSWORD));
    userRepository.save(adminUser);

    User regularUser = new User();
    regularUser.setUsername("user");
    regularUser.setPermissions(UserInfo.PermissionsEnum.READ);
    regularUser.setPasswordHash(passwordEncoder.encode(RAW_PASSWORD));
    userRepository.save(regularUser);

    // Generate JWTs for Each User
    rootToken = jwtUtil.generateToken(rootUsername);
    adminToken = jwtUtil.generateToken(adminUsername);
    userToken = jwtUtil.generateToken(userUsername);
  }

  // Critical flow tests

  @Test
  void deleteUser_shouldReturn403_whenAdminTriesToDeleteRoot() throws Exception {
    // Act & Assert
    mockMvc.perform(delete("/user/{username}", rootUsername)
                    .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isForbidden());
  }

  @Test
  void deleteUser_shouldReturn403_whenRootTriesToDeleteThemselves() throws Exception {
    // Act & Assert
    mockMvc.perform(delete("/user/{username}", rootUsername)
                    .header("Authorization", "Bearer " + rootToken))
            .andExpect(status().isForbidden());
  }

  @Test
  void deleteUser_shouldReturn403_whenRegularUserTriesToDeleteAnotherUser() throws Exception {
    // userToken is for 'user'. We are trying to delete 'admin'.

    // Act & Assert
    mockMvc.perform(delete("/user/{username}", adminUsername)
                    .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isForbidden());
  }

  @Test
  void changeUserPermissions_shouldReturn403_whenAdminTriesToChangeRootPermissions() throws Exception {
    // Arrange
    ChangePermissions permissions = new ChangePermissions().permissions(ChangePermissions.PermissionsEnum.ADMIN);

    // Act & Assert
    mockMvc.perform(put("/permissions/{username}", rootUsername)
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(permissions)))
            .andExpect(status().isForbidden());
  }

  @Test
  void changeUserPermissions_shouldReturn403_whenRootTriesToChangeRootPermissions() throws Exception {
    // Arrange
    ChangePermissions permissions = new ChangePermissions().permissions(ChangePermissions.PermissionsEnum.WRITE);

    // Act & Assert
    mockMvc.perform(put("/permissions/{username}", rootUsername)
                    .header("Authorization", "Bearer " + rootToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(permissions)))
            .andExpect(status().isForbidden());
  }

  @Test
  void changeUserPermissions_shouldReturn403_whenRegularUserTriesToChangePermissions() throws Exception {
    // Arrange: userToken is for a user with READ permissions.
    ChangePermissions permissions = new ChangePermissions().permissions(ChangePermissions.PermissionsEnum.WRITE);

    // Act & Assert
    mockMvc.perform(put("/permissions/{username}", userUsername) // Trying to change their own permissions
                    .header("Authorization", "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(permissions)))
            .andExpect(status().isForbidden());
  }

  @Test
  void updateUserProfile_shouldReturn403_whenRegularUserTriesToUpdateAnotherUser() throws Exception {
    // Act & Assert
    mockMvc.perform(put("/user/{username}", adminUsername) // Target is the admin
                    .header("Authorization", "Bearer " + userToken) // Actor is a regular user
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isForbidden());
  }

  @Test
  void updateUserProfile_shouldReturn403_whenAdminTriesToUpdateRoot() throws Exception {
    // Act & Assert
    mockMvc.perform(put("/user/{username}", rootUsername)
                    .header("Authorization", "Bearer " + adminToken) // Actor is an admin
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isForbidden());
  }

  @Test
  void login_shouldReturn401_whenPasswordIsIncorrect() throws Exception {
    // Arrange
    // The real password is 'password'. We try to log in with 'wrong-password'.
    UserLoginInfo loginInfo = new UserLoginInfo()
            .userName(userUsername)
            .password("wrong-" + RAW_PASSWORD);

    // Act & Assert
    mockMvc.perform(post("/user")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginInfo)))
            .andExpect(status().isUnauthorized());
  }

  // Standard flow verification (The happy path)

  @Test
  void deleteUser_shouldReturnOk_whenRootDeletesAdmin() throws Exception {
    // Act & Assert
    mockMvc.perform(delete("/user/{username}", adminUsername)
                    .header("Authorization", "Bearer " + rootToken))
            .andExpect(status().isOk());
  }

  @Test
  void deleteUser_shouldReturnOk_whenUserDeletesThemselves() throws Exception {
    // Act & Assert
    mockMvc.perform(delete("/user/{username}", userUsername)
                    .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isOk());
  }


  @Test
  void updateUserProfile_shouldReturnOk_whenUserUpdatesTheirOwnProfile() throws Exception {
    // Act & Assert
    mockMvc.perform(put("/user/{username}", userUsername)
                    .header("Authorization", "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk());
  }

  @Test
  void updateUserProfile_shouldReturnOk_whenAdminUpdatesRegularUser() throws Exception {
    // Act & Assert
    mockMvc.perform(put("/user/{username}", userUsername)
                    .header("Authorization", "Bearer " + adminToken) // Authenticated as ADMIN
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk());
  }

  @Test
  void login_shouldReturnOkAndToken_whenCredentialsAreValid() throws Exception {
    // Arrange
    UserLoginInfo loginInfo = new UserLoginInfo()
            .userName(userUsername)
            .password(RAW_PASSWORD);

    // Act & Assert
    mockMvc.perform(post("/user")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginInfo)))
            .andExpect(status().isOk())
            .andExpect(header().exists("Authorization"))
            .andExpect(jsonPath("$.basic_info.username").value(userUsername));
  }
}
