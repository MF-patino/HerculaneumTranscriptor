package com.mf.HerculaneumTranscriptor.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mf.HerculaneumTranscriptor.domain.User;
import com.mf.HerculaneumTranscriptor.repository.ScrollRepository;
import com.mf.HerculaneumTranscriptor.repository.UserRepository;
import com.mf.HerculaneumTranscriptor.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import scroll.dto.NewScroll;
import user.dto.UserInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional // Ensures each test runs in its own transaction and is rolled back.
@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
public class ScrollFlowIntegrationTest {
  @TempDir
  static Path sharedTempDir;

  @DynamicPropertySource
  static void overrideProperties(DynamicPropertyRegistry registry) {
    // Needed to use the temporary folder as the scroll storage location for the tests.
    registry.add("api.scrolls.storageDirectory", () -> sharedTempDir.toString());
  }

  private final MockMvc mockMvc;
  private final JwtUtil jwtUtil;
  private final ObjectMapper objectMapper;
  private final UserRepository userRepository;
  private final ScrollRepository scrollRepository;
  private final PasswordEncoder passwordEncoder;

  private String adminToken;
  private String userToken;

  private NewScroll newScrollDto;

  private final String SCROLL_ID = "vesuvius-scroll-1";
  private final String EXISTING_SCROLL_ID = "vesuvius-scroll-2";

  @BeforeEach
  void setUp() throws IOException {
    String userUsername = "user";
    String adminUsername = "admin";
    String RAW_PASSWORD = "password";

    User adminUser = new User();
    adminUser.setUsername(adminUsername);
    adminUser.setPermissions(UserInfo.PermissionsEnum.ADMIN);
    adminUser.setPasswordHash(passwordEncoder.encode(RAW_PASSWORD));
    userRepository.save(adminUser);

    User regularUser = new User();
    regularUser.setUsername(userUsername);
    regularUser.setPermissions(UserInfo.PermissionsEnum.READ);
    regularUser.setPasswordHash(passwordEncoder.encode(RAW_PASSWORD));
    userRepository.save(regularUser);

    // Generate JWTs for each user
    adminToken = jwtUtil.generateToken(adminUsername);
    userToken = jwtUtil.generateToken(userUsername);

    newScrollDto = new NewScroll();
    newScrollDto.setScrollId(SCROLL_ID);
    newScrollDto.setDisplayName("Vesuvius Scroll 1");

    com.mf.HerculaneumTranscriptor.domain.Scroll scroll = new com.mf.HerculaneumTranscriptor.domain.Scroll();
    scroll.setScrollId(EXISTING_SCROLL_ID);
    scroll.setDisplayName("Vesuvius Challenge Scroll 2");
    scroll.setImagePath(EXISTING_SCROLL_ID + ".png");
    scrollRepository.save(scroll);

    Path dummyFile = sharedTempDir.resolve(EXISTING_SCROLL_ID + ".png").normalize();
    Files.writeString(dummyFile, "dummy image content");
    assertThat(dummyFile).exists();
  }

  @Test
  void createScroll_shouldReturn403_whenRegularUserTriesToCreate() throws Exception {
    // Arrange
    MockMultipartFile imageFile = new MockMultipartFile("ink_image", "image.png", MediaType.IMAGE_PNG_VALUE, "dummy-bytes".getBytes());
    String metadataJson = objectMapper.writeValueAsString(newScrollDto);
    MockMultipartFile metadataPart = new MockMultipartFile("metadata", "", MediaType.APPLICATION_JSON_VALUE, metadataJson.getBytes());

    // Act & Assert
    mockMvc.perform(multipart("/scrolls")
                    .file(imageFile)
                    .file(metadataPart)
                    .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isForbidden());
  }

  @Test
  void createScroll_shouldReturnOk_whenAdminUserTriesToCreate() throws Exception {
    // Arrange
    MockMultipartFile imageFile = new MockMultipartFile("ink_image", "image.png", MediaType.IMAGE_PNG_VALUE, "dummy-bytes".getBytes());
    String metadataJson = objectMapper.writeValueAsString(newScrollDto);
    MockMultipartFile metadataPart = new MockMultipartFile("metadata", "", MediaType.APPLICATION_JSON_VALUE, metadataJson.getBytes());

    // Act & Assert
    mockMvc.perform(multipart("/scrolls")
                    .file(imageFile)
                    .file(metadataPart)
                    .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.scrollId").value(SCROLL_ID));

    assertThat(scrollRepository.findByScrollId(SCROLL_ID)).isNotEmpty();
    Path createdFile = sharedTempDir.resolve(SCROLL_ID + ".png").normalize();
    assertThat(createdFile).exists();
  }

  @Test
  void deleteScroll_shouldReturn403_whenRegularUserDeletesScroll() throws Exception {
    // Act & Assert
    mockMvc.perform(delete("/scrolls/{scrollId}", EXISTING_SCROLL_ID)
                    .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isForbidden());
  }

  @Test
  void deleteScroll_shouldReturnOk_whenAdminDeletesScroll() throws Exception {
    // Act & Assert
    mockMvc.perform(delete("/scrolls/{scrollId}", EXISTING_SCROLL_ID)
                    .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());

    assertThat(scrollRepository.findByScrollId(EXISTING_SCROLL_ID)).isEmpty();
    Path deletedFile = sharedTempDir.resolve(EXISTING_SCROLL_ID + ".png").normalize();
    assertThat(deletedFile).doesNotExist();
  }

  @Test
  void updateScroll_shouldReturnOk_whenAdminUpdatesScroll() throws Exception {
    // Arrange
    String requestBody = objectMapper.writeValueAsString(newScrollDto);

    // Act & Assert
    mockMvc.perform(put("/scrolls/{scrollId}", EXISTING_SCROLL_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
                    .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.scrollId").value(SCROLL_ID));

    assertThat(scrollRepository.findByScrollId(EXISTING_SCROLL_ID)).isEmpty();
    assertThat(scrollRepository.findByScrollId(SCROLL_ID)).isNotEmpty();
  }

  @Test
  void updateScroll_shouldReturn403_whenRegularUserUpdatesScroll() throws Exception {
    // Arrange
    String requestBody = objectMapper.writeValueAsString(newScrollDto);

    // Act & Assert
    mockMvc.perform(put("/scrolls/{scrollId}", EXISTING_SCROLL_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
                    .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isForbidden());
  }
}