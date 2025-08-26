package com.mf.HerculaneumTranscriptor.service;

import com.mf.HerculaneumTranscriptor.domain.Scroll;
import com.mf.HerculaneumTranscriptor.domain.mapper.ScrollMapper;
import com.mf.HerculaneumTranscriptor.exception.ResourceAlreadyExistsException;
import com.mf.HerculaneumTranscriptor.repository.ScrollRepository;
import com.mf.HerculaneumTranscriptor.service.impl.ScrollServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import scroll.dto.NewScroll;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ScrollServiceImplTest {

  @InjectMocks
  private ScrollServiceImpl scrollService;

  @Mock
  private ScrollRepository scrollRepository;

  @Mock
  private ScrollMapper scrollMapper;

  // Reusable test data objects
  private Scroll scroll;
  private scroll.dto.Scroll scrollDto;
  private NewScroll newScrollDto;
  private MockMultipartFile mockImageFile;
  private static final Path TEST_STORAGE_LOCATION = Paths.get("test-uploads");

  private static final String SCROLL_ID = "vesuvius-scroll-1";
  private static final String DISPLAY_NAME = "Vesuvius Challenge Scroll 1";


  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(scrollService, "storageLocation", TEST_STORAGE_LOCATION);

    // Create entity
    scroll = new Scroll();
    scroll.setId(1L);
    scroll.setScrollId(SCROLL_ID);
    scroll.setDisplayName(DISPLAY_NAME);
    scroll.setImagePath(SCROLL_ID + ".png");

    // Create DTOs
    scrollDto = new scroll.dto.Scroll();
    scrollDto.setScrollId(SCROLL_ID);
    scrollDto.setDisplayName(DISPLAY_NAME);

    newScrollDto = new NewScroll();
    newScrollDto.setScrollId(SCROLL_ID);
    newScrollDto.setDisplayName(DISPLAY_NAME);

    // Create a mock file for upload tests
    mockImageFile = new MockMultipartFile(
            "inkImage",
            "vesuvius-scroll-1.png",
            "image/png",
            "dummy image content".getBytes()
    );
  }

  // Tests for getAllScrolls

  @Test
  void getAllScrolls_shouldReturnListOfScrolls_whenScrollsExist() {
    // Arrange
    when(scrollRepository.findAll()).thenReturn(List.of(scroll));
    when(scrollMapper.scrollEntityToScrollDto(scroll)).thenReturn(scrollDto);

    // Act
    List<scroll.dto.Scroll> result = scrollService.getAllScrolls();

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.size()).isEqualTo(1);
    assertThat(result.getFirst()).isEqualTo(scrollDto);
    verify(scrollRepository, times(1)).findAll();
  }

  @Test
  void getAllScrolls_shouldReturnEmptyList_whenNoScrollsExist() {
    // Arrange
    when(scrollRepository.findAll()).thenReturn(Collections.emptyList());

    // Act
    List<scroll.dto.Scroll> result = scrollService.getAllScrolls();

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.isEmpty()).isTrue();
    verify(scrollMapper, never()).scrollEntityToScrollDto(any());
  }

  // Tests for createScroll

  @Test
  void createScroll_shouldCreateAndReturnScroll_whenIdIsAvailable() throws IOException {
    // Arrange
    when(scrollRepository.existsByScrollId(SCROLL_ID)).thenReturn(false);
    when(scrollMapper.newScrollDtoToScrollEntity(newScrollDto)).thenReturn(scroll);
    when(scrollRepository.save(scroll)).thenReturn(scroll);
    when(scrollMapper.scrollEntityToScrollDto(scroll)).thenReturn(scrollDto);

    // We need to mock static methods in `java.nio.file.Files`
    try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {
      // Act
      scroll.dto.Scroll result = scrollService.createScroll(newScrollDto, mockImageFile);

      // Assert
      assertThat(result).isEqualTo(scrollDto);
      verify(scrollRepository, times(1)).save(scroll);

      // Verify that the file copy operation was attempted
      mockedFiles.verify(() -> Files.copy(any(InputStream.class), any(Path.class), any()));
    }
  }

  @Test
  void createScroll_shouldThrowResourceAlreadyExistsException_whenIdIsTaken() {
    // Arrange
    when(scrollRepository.existsByScrollId(SCROLL_ID)).thenReturn(true);

    // Act & Assert
    assertThrows(ResourceAlreadyExistsException.class,
            () -> scrollService.createScroll(newScrollDto, mockImageFile));

    verify(scrollRepository, never()).save(any());
  }

}
