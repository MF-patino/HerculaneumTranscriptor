package com.mf.HerculaneumTranscriptor.integration;

import annotation.dto.Coordinates;
import annotation.dto.NewBoxRegion;
import annotation.dto.Vote;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mf.HerculaneumTranscriptor.domain.Annotation;
import com.mf.HerculaneumTranscriptor.domain.Scroll;
import com.mf.HerculaneumTranscriptor.domain.User;
import com.mf.HerculaneumTranscriptor.repository.AnnotationRepository;
import com.mf.HerculaneumTranscriptor.repository.ScrollRepository;
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
import user.dto.UserInfo;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional // Ensures each test runs in its own transaction and is rolled back.
@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
public class AnnotationFlowIntegrationTest {
  private final MockMvc mockMvc;
  private final JwtUtil jwtUtil;
  private final ObjectMapper objectMapper;
  private final UserRepository userRepository;
  private final ScrollRepository scrollRepository;
  private final AnnotationRepository annotationRepository;
  private final PasswordEncoder passwordEncoder;

  private User writeUser;
  private Annotation writeUserAnnotation; // An annotation created by 'writeUser'

  private String adminToken;
  private String writeUserToken;
  private String readUserToken;
  private String anotherWriteUserToken;
  private NewBoxRegion newBoxRegionDto;
  private static final String SCROLL_ID = "vesuvius-scroll-1";

  @BeforeEach
  void setUp() {
    // Create Users
    // Test Data
    User adminUser = userRepository.save(new User(null, "admin", "Admin", "User", "admin@test.com", passwordEncoder.encode("pw"), UserInfo.PermissionsEnum.ADMIN));
    writeUser = userRepository.save(new User(null, "writer", "Write", "User", "write@test.com", passwordEncoder.encode("pw"), UserInfo.PermissionsEnum.WRITE));
    User anotherWriteUser = userRepository.save(new User(null, "anotherWriter", "Another", "Writer", "another@test.com", passwordEncoder.encode("pw"), UserInfo.PermissionsEnum.WRITE));
    User readUser = userRepository.save(new User(null, "reader", "Read", "User", "read@test.com", passwordEncoder.encode("pw"), UserInfo.PermissionsEnum.READ));

    // Create Parent Scroll
    Scroll scroll = scrollRepository.save(new Scroll(null, SCROLL_ID, "Test Scroll", null, null, null, null, null));

    // Create Existing Annotation by 'writeUser'
    writeUserAnnotation = new Annotation();
    writeUserAnnotation.setRegionId(UUID.randomUUID());
    writeUserAnnotation.setAuthor(writeUser);
    writeUserAnnotation.setScroll(scroll);
    writeUserAnnotation.setTranscription("Original text");
    writeUserAnnotation = annotationRepository.save(writeUserAnnotation);

    // Generate Tokens
    adminToken = jwtUtil.generateToken(adminUser.getUsername());
    writeUserToken = jwtUtil.generateToken(writeUser.getUsername());
    anotherWriteUserToken = jwtUtil.generateToken(anotherWriteUser.getUsername());
    readUserToken = jwtUtil.generateToken(readUser.getUsername());

    // Create DTO for requests
    Coordinates coords = new Coordinates().x(10f).y(10f).width(100f).height(100f);
    newBoxRegionDto = new NewBoxRegion().coordinates(coords).transcription("New transcription");
  }

  // Tests for Create Region

  @Test
  void createRegion_shouldReturnCreated_whenUserHasWritePermission() throws Exception {
    String requestBody = objectMapper.writeValueAsString(newBoxRegionDto);

    mockMvc.perform(post("/scrolls/{scrollId}/regions", SCROLL_ID)
                    .header("Authorization", "Bearer " + writeUserToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.authorUsername").value(writeUser.getUsername()));
  }

  @Test
  void createRegion_shouldReturn403_whenUserHasOnlyReadPermission() throws Exception {
    String requestBody = objectMapper.writeValueAsString(newBoxRegionDto);

    mockMvc.perform(post("/scrolls/{scrollId}/regions", SCROLL_ID)
                    .header("Authorization", "Bearer " + readUserToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isForbidden());
  }

  // Tests for Update Region

  @Test
  void updateRegion_shouldReturnOk_whenAdminUpdatesAnotherUsersRegion() throws Exception {
    String requestBody = objectMapper.writeValueAsString(newBoxRegionDto);

    mockMvc.perform(put("/scrolls/{scrollId}/regions/{regionId}", SCROLL_ID, writeUserAnnotation.getRegionId())
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.basic_info.transcription").value("New transcription"));
  }

  @Test
  void updateRegion_shouldReturnOk_whenAuthorUpdatesTheirOwnRegion() throws Exception {
    String requestBody = objectMapper.writeValueAsString(newBoxRegionDto);

    mockMvc.perform(put("/scrolls/{scrollId}/regions/{regionId}", SCROLL_ID, writeUserAnnotation.getRegionId())
                    .header("Authorization", "Bearer " + writeUserToken) // User is the author
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isOk());
  }

  @Test
  void updateRegion_shouldReturn403_whenNonAuthorTriesToUpdateRegion() throws Exception {
    String requestBody = objectMapper.writeValueAsString(newBoxRegionDto);

    mockMvc.perform(put("/scrolls/{scrollId}/regions/{regionId}", SCROLL_ID, writeUserAnnotation.getRegionId())
                    .header("Authorization", "Bearer " + anotherWriteUserToken) // User is NOT the author
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isForbidden());
  }

  @Test
  void updateRegion_shouldReturn403_whenReaderTriesToUpdateRegion() throws Exception {
    String requestBody = objectMapper.writeValueAsString(newBoxRegionDto);

    mockMvc.perform(put("/scrolls/{scrollId}/regions/{regionId}", SCROLL_ID, writeUserAnnotation.getRegionId())
                    .header("Authorization", "Bearer " + readUserToken) // User is NOT the author
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isForbidden());
  }

  // Tests for Delete Region

  @Test
  void deleteRegion_shouldReturnNoContent_whenAdminDeletesAnotherUsersRegion() throws Exception {
    mockMvc.perform(delete("/scrolls/{scrollId}/regions/{regionId}", SCROLL_ID, writeUserAnnotation.getRegionId())
                    .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isNoContent());

    assertThat(annotationRepository.findByRegionId(writeUserAnnotation.getRegionId())).isEmpty();
  }

  @Test
  void deleteRegion_shouldReturnNoContent_whenAuthorDeletesTheirOwnRegion() throws Exception {
    mockMvc.perform(delete("/scrolls/{scrollId}/regions/{regionId}", SCROLL_ID, writeUserAnnotation.getRegionId())
                    .header("Authorization", "Bearer " + writeUserToken)) // User is the author
            .andExpect(status().isNoContent());
  }

  @Test
  void deleteRegion_shouldReturn403_whenNonAuthorTriesToDeleteRegion() throws Exception {
    mockMvc.perform(delete("/scrolls/{scrollId}/regions/{regionId}", SCROLL_ID, writeUserAnnotation.getRegionId())
                    .header("Authorization", "Bearer " + anotherWriteUserToken)) // User is NOT the author
            .andExpect(status().isForbidden());
  }

  @Test
  void deleteRegion_shouldReturn403_whenReaderTriesToDeleteRegion() throws Exception {
    mockMvc.perform(delete("/scrolls/{scrollId}/regions/{regionId}", SCROLL_ID, writeUserAnnotation.getRegionId())
                    .header("Authorization", "Bearer " + readUserToken)) // User is NOT the author
            .andExpect(status().isForbidden());
  }

  // Tests for voteOnRegion

  @Test
  void voteOnRegion_shouldReturnOk_whenUserHasWritePermission() throws Exception {
    Vote voteDto = new Vote().vote(5);
    String requestBody = objectMapper.writeValueAsString(voteDto);

    mockMvc.perform(post("/scrolls/{scrollId}/regions/{regionId}/vote", SCROLL_ID, writeUserAnnotation.getRegionId())
                    .header("Authorization", "Bearer " + anotherWriteUserToken) // A different user with WRITE
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.certaintyScore").isNotEmpty());
  }

  @Test
  void voteOnRegion_shouldReturn403_whenUserHasOnlyReadPermission() throws Exception {
    Vote voteDto = new Vote().vote(5);
    String requestBody = objectMapper.writeValueAsString(voteDto);

    mockMvc.perform(post("/scrolls/{scrollId}/regions/{regionId}/vote", SCROLL_ID, writeUserAnnotation.getRegionId())
                    .header("Authorization", "Bearer " + readUserToken) // User has READ role
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isForbidden());
  }
}
