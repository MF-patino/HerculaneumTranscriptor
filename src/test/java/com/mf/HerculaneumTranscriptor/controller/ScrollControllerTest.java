package com.mf.HerculaneumTranscriptor.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mf.HerculaneumTranscriptor.security.JwtAuthenticationFilter;
import com.mf.HerculaneumTranscriptor.service.ScrollService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import scroll.dto.NewScroll;
import scroll.dto.Scroll;

import java.net.URI;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ScrollController.class, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
})
public class ScrollControllerTest {
  // Disable CSRF for all requests within this test context.
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
  private ScrollService scrollService;

  private Scroll scrollDto;
  private NewScroll newScrollDto;

  private static final String SCROLL_ID = "vesuvius-scroll-1";

  @BeforeEach
  void setUp() {
    scrollDto = new Scroll();
    scrollDto.setScrollId(SCROLL_ID);
    scrollDto.setDisplayName("Vesuvius Scroll 1");

    newScrollDto = new NewScroll();
    newScrollDto.setScrollId(SCROLL_ID);
    newScrollDto.setDisplayName("Vesuvius Scroll 1");
  }

  // Tests for getAllScrolls

  @Test
  void getAllScrolls_shouldReturnScrollList_whenAuthenticated() throws Exception {
    // Arrange
    when(scrollService.getAllScrolls()).thenReturn(List.of(scrollDto));

    // Act & Assert
    mockMvc.perform(get("/scrolls"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].scrollId").value(SCROLL_ID));
  }

  // Tests for createScroll

  @Test
  void createScroll_shouldReturnOk_whenUserUploadsValidData() throws Exception {
    // Arrange
    MockMultipartFile imageFile = new MockMultipartFile(
            "ink_image", "image.png", MediaType.IMAGE_PNG_VALUE, "dummy-image-bytes".getBytes());

    // For multipart/form-data, the JSON part is sent as a string
    String metadataJson = objectMapper.writeValueAsString(newScrollDto);
    MockMultipartFile metadataPart = new MockMultipartFile(
            "metadata", "", MediaType.APPLICATION_JSON_VALUE, metadataJson.getBytes());

    when(scrollService.createScroll(any(NewScroll.class), any(MockMultipartFile.class))).thenReturn(scrollDto);

    // Act & Assert
    mockMvc.perform(multipart("/scrolls")
                    .file(imageFile)
                    .file(metadataPart))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.scrollId").value(SCROLL_ID));
  }

  @Test
  void createScroll_shouldReturn400_whenUserUploadsInvalidData() throws Exception {
    // Arrange
    MockMultipartFile imageFile = new MockMultipartFile(
            "ink_image", "image.png", MediaType.IMAGE_PNG_VALUE, "dummy-image-bytes".getBytes());
    MockMultipartFile metadataPart = new MockMultipartFile("metadata", "", MediaType.APPLICATION_JSON_VALUE, "{}".getBytes());

    when(scrollService.createScroll(any(NewScroll.class), any(MockMultipartFile.class))).thenReturn(scrollDto);

    // Act & Assert
    mockMvc.perform(multipart("/scrolls")
                    .file(imageFile)
                    .file(metadataPart))
            .andExpect(status().isBadRequest());
  }

  // Tests for deleteScroll

  @Test
  void deleteScroll_shouldReturnOk_whenUserDeletesScroll() throws Exception {
    // Arrange
    // A void service method should be mocked with doNothing()
    doNothing().when(scrollService).deleteScroll(SCROLL_ID);

    // Act & Assert
    mockMvc.perform(delete("/scrolls/{scrollId}", SCROLL_ID))
            .andExpect(status().isOk());
  }

  // Tests for getScrollImage

  @Test
  void getScrollImage_shouldReturnImage_whenAuthenticated() throws Exception {
    // Arrange
    byte[] imageBytes = "dummy-image-content".getBytes();
    Resource imageResource = new ByteArrayResource(imageBytes);
    when(scrollService.getScrollImage(SCROLL_ID)).thenReturn(imageResource);

    // Act & Assert
    mockMvc.perform(get("/scrolls/{scrollId}/local-download", SCROLL_ID))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.IMAGE_PNG)) // Assuming the service sets this
            .andExpect(content().bytes(imageBytes));
  }

  // Tests for getScrollImageURL

  @Test
  void getScrollImageURL_shouldReturnURL_whenAuthenticated() throws Exception {
    // Arrange
    URI imageURI = new URI("https://cloudinary.com/signed/url/for/cloud-scroll-1");
    when(scrollService.getScrollImageURL(SCROLL_ID)).thenReturn(imageURI);

    // Act & Assert
    mockMvc.perform(get("/scrolls/{scrollId}", SCROLL_ID))
            .andExpect(status().isFound())
            .andExpect(header().string("Location", imageURI.toString()));
  }

  // Tests for updateScroll

  @Test
  void updateScroll_shouldReturnOk_whenUserUpdatesScroll() throws Exception {
    // Arrange
    when(scrollService.updateScroll(eq(SCROLL_ID), any(NewScroll.class))).thenReturn(scrollDto);
    String requestBody = objectMapper.writeValueAsString(newScrollDto);

    // Act & Assert
    mockMvc.perform(put("/scrolls/{scrollId}", SCROLL_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.scrollId").value(SCROLL_ID));
  }

  @Test
  void updateScroll_shouldReturn400_whenUserUpdatesScrollWithInvalidData() throws Exception {
    // Arrange
    when(scrollService.updateScroll(eq(SCROLL_ID), any(NewScroll.class))).thenReturn(scrollDto);
    String requestBody = "{}";

    // Act & Assert
    mockMvc.perform(put("/scrolls/{scrollId}", SCROLL_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isBadRequest());
  }
}