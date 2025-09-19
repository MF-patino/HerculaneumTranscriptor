package com.mf.HerculaneumTranscriptor.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mf.HerculaneumTranscriptor.security.JwtAuthenticationFilter;
import com.mf.HerculaneumTranscriptor.service.AnnotationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import annotation.dto.*;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AnnotationController.class,
        // Exclude custom JWT filter.
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
        }
)
public class AnnotationControllerTest {
  // Disable CSRF for this test context, since our API uses JWT.
  @TestConfiguration
  static class TestSecurityConfig {
    @Bean
    public SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
      http.csrf(AbstractHttpConfigurer::disable);
      return http.build();
    }
  }

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private AnnotationService annotationService;

  private BoxRegion boxRegionDto;
  private NewBoxRegion newBoxRegionDto;
  private RegionUpdateResponse regionUpdateResponse;
  private Vote voteDto;

  private static final String SCROLL_ID = "vesuvius-scroll-1";
  private static final UUID REGION_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    boxRegionDto = new BoxRegion();
    boxRegionDto.setRegionId(REGION_ID);
    boxRegionDto.setAuthorUsername("testuser");

    Coordinates coords = new Coordinates().x(10f).y(20f).width(100f).height(50f);
    newBoxRegionDto = new NewBoxRegion().coordinates(coords).transcription("test");

    regionUpdateResponse = new RegionUpdateResponse();
    regionUpdateResponse.setRegions(List.of(boxRegionDto));
    regionUpdateResponse.setLastSyncTimestamp(Date.from(Instant.now()));

    voteDto = new Vote().vote(5);
  }

  // Tests for getScrollRegions

  @Test
  void getScrollRegions_shouldReturnRegionUpdateResponse() throws Exception {
    // Arrange
    when(annotationService.getScrollRegions(eq(SCROLL_ID), any())).thenReturn(regionUpdateResponse);

    // Act & Assert
    mockMvc.perform(get("/scrolls/{scrollId}/regions", SCROLL_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.regions[0].regionId").value(REGION_ID.toString()));
  }

  // Tests for createRegion

  @Test
  void createRegion_shouldReturnCreated_withValidData() throws Exception {
    // Arrange
    when(annotationService.createRegion(eq(SCROLL_ID), any(NewBoxRegion.class))).thenReturn(boxRegionDto);
    String requestBody = objectMapper.writeValueAsString(newBoxRegionDto);

    // Act & Assert
    mockMvc.perform(post("/scrolls/{scrollId}/regions", SCROLL_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isCreated()) // As per your OpenAPI spec
            .andExpect(jsonPath("$.regionId").value(REGION_ID.toString()));
  }

  @Test
  void createRegion_shouldReturn400_withInvalidData() throws Exception {
    // Arrange
    String invalidBody = "{\"transcription\":\"test\"}"; // Missing required 'coordinates'

    // Act & Assert
    mockMvc.perform(post("/scrolls/{scrollId}/regions", SCROLL_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidBody))
            .andExpect(status().isBadRequest());
  }


  // Tests for updateRegion

  @Test
  void updateRegion_shouldReturnOk_withValidData() throws Exception {
    // Arrange
    when(annotationService.updateRegion(eq(SCROLL_ID), eq(REGION_ID), any(NewBoxRegion.class))).thenReturn(boxRegionDto);
    String requestBody = objectMapper.writeValueAsString(newBoxRegionDto);

    // Act & Assert
    mockMvc.perform(put("/scrolls/{scrollId}/regions/{regionId}", SCROLL_ID, REGION_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.regionId").value(REGION_ID.toString()));
  }

  @Test
  void updateRegion_shouldReturn400_withInValidData() throws Exception {
    // Arrange
    String invalidBody = "{\"transcription\":\"test\"}"; // Missing required 'coordinates'

    // Act & Assert
    mockMvc.perform(put("/scrolls/{scrollId}/regions/{regionId}", SCROLL_ID, REGION_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidBody))
            .andExpect(status().isBadRequest());
  }

  // Tests for deleteRegion

  @Test
  void deleteRegion_shouldReturnNoContent() throws Exception {
    // Arrange
    doNothing().when(annotationService).deleteRegion(SCROLL_ID, REGION_ID);

    // Act & Assert
    mockMvc.perform(delete("/scrolls/{scrollId}/regions/{regionId}", SCROLL_ID, REGION_ID))
            .andExpect(status().isNoContent()); // 204 No Content is standard for DELETE
  }


  // Tests for voteOnRegion

  @Test
  void voteOnRegion_shouldReturnOk_withValidVote() throws Exception {
    // Arrange
    when(annotationService.voteOnRegion(eq(SCROLL_ID), eq(REGION_ID), any(Vote.class))).thenReturn(boxRegionDto);
    String requestBody = objectMapper.writeValueAsString(voteDto);

    // Act & Assert
    mockMvc.perform(post("/scrolls/{scrollId}/regions/{regionId}/vote", SCROLL_ID, REGION_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.regionId").value(REGION_ID.toString()));
  }

  @Test
  void voteOnRegion_shouldReturn400_withInValidVote() throws Exception {
    // Arrange
    String invalidBody = "{}"; // Missing required 'vote'

    // Act & Assert
    mockMvc.perform(post("/scrolls/{scrollId}/regions/{regionId}/vote", SCROLL_ID, REGION_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidBody))
            .andExpect(status().isBadRequest());
  }
}
