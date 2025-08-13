package com.mf.HerculaneumTranscriptor.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mf.HerculaneumTranscriptor.configuration.SecurityConfiguration;
import com.mf.HerculaneumTranscriptor.dto.AuthenticationResponse;
import com.mf.HerculaneumTranscriptor.security.JwtUtil;
import com.mf.HerculaneumTranscriptor.service.UserService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import user.dto.BasicUserInfo;
import user.dto.UserInfo;
import user.dto.UserRegisterInfo;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

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

  // Mock of the service layer.
  @MockitoBean
  private UserService userService;

  private AuthenticationResponse authResponse;
  private UserInfo userInfo;
  private UserRegisterInfo registerInfo;

  @BeforeEach
  void setUp() {
    userInfo = new UserInfo();
    BasicUserInfo basicInfo = new BasicUserInfo();
    basicInfo.setUsername("JohnDoe");
    basicInfo.setContact("john.doe@example.com");
    basicInfo.setFirstName("John");
    basicInfo.setLastName("Doe");
    userInfo.setBasicInfo(basicInfo);
    userInfo.setPermissions(UserInfo.PermissionsEnum.READ);
    authResponse = new AuthenticationResponse("token", userInfo);

    registerInfo = new UserRegisterInfo();
    registerInfo.setBasicInfo(basicInfo);
    registerInfo.setPassword("password");
  }

  @Test
  void findUserByName_shouldReturnUserInfo_whenUserExists() throws Exception {
    // Arrange
    when(userService.findUserByUsername("JohnDoe")).thenReturn(userInfo);
    String token = jwtUtil.generateToken("JohnDoe");

    // Act & Assert
    mockMvc.perform(get("/user/{username}", "JohnDoe").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.basic_info.username").value("JohnDoe"));
  }

  @Test
  void registerNewUser_shouldReturnCreated_withValidInfo() throws Exception {
    // Arrange
    when(userService.registerNewUser(any(UserRegisterInfo.class))).thenReturn(authResponse);

    // Act & Assert
    mockMvc.perform(post("/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerInfo))
            )
            .andExpect(status().isOk());
  }
}
