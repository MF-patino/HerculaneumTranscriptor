package com.mf.HerculaneumTranscriptor.service;

import com.mf.HerculaneumTranscriptor.domain.Scroll;
import com.mf.HerculaneumTranscriptor.domain.mapper.ScrollMapper;
import com.mf.HerculaneumTranscriptor.exception.ResourceAlreadyExistsException;
import com.mf.HerculaneumTranscriptor.exception.ResourceNotFoundException;
import com.mf.HerculaneumTranscriptor.repository.ScrollRepository;
import com.mf.HerculaneumTranscriptor.service.impl.ScrollServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
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
import java.util.Optional;

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
    ReflectionTestUtils.setField(scrollService, "useCloudStorage", false);

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

  // Tests for deleteScroll

  @Test
  void deleteScroll_shouldDeleteScrollAndFile_whenScrollExists() throws IOException {
    // Arrange
    when(scrollRepository.findByScrollId(SCROLL_ID)).thenReturn(Optional.of(scroll));
    doNothing().when(scrollRepository).delete(scroll);

    // Mock static Files.delete method
    try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {
      mockedFiles.when(() -> Files.delete(any(Path.class))).thenAnswer(invocation -> null);
      mockedFiles.when(() -> Files.exists(any(Path.class))).thenAnswer(invocation -> true);

      // Act
      scrollService.deleteScroll(SCROLL_ID);

      // Assert
      verify(scrollRepository, times(1)).delete(scroll);
      Path expectedPath = TEST_STORAGE_LOCATION.resolve(scroll.getImagePath()).normalize();
      mockedFiles.verify(() -> Files.delete(eq(expectedPath)));
    }
  }

  @Test
  void deleteScroll_shouldThrowResourceNotFoundException_whenScrollDoesNotExist() {
    // Arrange
    when(scrollRepository.findByScrollId(SCROLL_ID)).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(ResourceNotFoundException.class, () -> scrollService.deleteScroll(SCROLL_ID));
    verify(scrollRepository, never()).delete(any());
  }

  // Tests for getScrollImage

  @Test
  void getScrollImage_shouldReturnResource_whenScrollAndFileExist() throws IOException {
    // Arrange
    when(scrollRepository.findByScrollId(SCROLL_ID)).thenReturn(Optional.of(scroll));

    // We mock the static getResourceAsStream method
    try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {
      InputStream mockInputStream = mock(InputStream.class);
      mockedFiles.when(() -> Files.newInputStream(any(Path.class))).thenReturn(mockInputStream);

      // Act
      Resource result = scrollService.getScrollImage(SCROLL_ID);

      // Assert
      assertThat(result).isNotNull();
      assertThat(result.getInputStream()).isEqualTo(mockInputStream);
    }
  }

  @Test
  void getScrollImage_shouldThrowResourceNotFoundException_whenScrollDoesNotExist() {
    // Arrange
    when(scrollRepository.findByScrollId(SCROLL_ID)).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(ResourceNotFoundException.class, () -> scrollService.getScrollImage(SCROLL_ID));
  }

  // Tests for updateScroll

  @Test
  void updateScroll_shouldUpdateAndReturnScroll_whenScrollExistsAndNewIdIsAvailable() {
    // Arrange
    NewScroll updateDto = new NewScroll();
    String newScrollId = "vesuvius-scroll-1-updated";
    String newDisplayName = "Updated Scroll Name";
    updateDto.setScrollId(newScrollId);
    updateDto.setDisplayName(newDisplayName);

    when(scrollRepository.findByScrollId(SCROLL_ID)).thenReturn(Optional.of(scroll));
    when(scrollRepository.existsByScrollId(newScrollId)).thenReturn(false);
    when(scrollRepository.save(any(Scroll.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // Mock the final DTO mapping
    scroll.dto.Scroll updatedDto = new scroll.dto.Scroll();
    updatedDto.setScrollId(newScrollId);
    updatedDto.setDisplayName(newDisplayName);
    when(scrollMapper.scrollEntityToScrollDto(any(Scroll.class))).thenReturn(updatedDto);

    // Act
    scroll.dto.Scroll result = scrollService.updateScroll(SCROLL_ID, updateDto);

    // Assert
    // Verify the result is what the mapper returned.
    assertThat(result).isEqualTo(updatedDto);

    // Use an ArgumentCaptor to inspect what was saved to the database.
    ArgumentCaptor<Scroll> scrollCaptor = ArgumentCaptor.forClass(Scroll.class);
    verify(scrollRepository).save(scrollCaptor.capture());
    Scroll savedScroll = scrollCaptor.getValue();

    // Check that the fields were correctly updated on the entity.
    assertThat(savedScroll.getScrollId()).isEqualTo(newScrollId);
    assertThat(savedScroll.getDisplayName()).isEqualTo(newDisplayName);
  }

  @Test
  void updateScroll_shouldThrowResourceNotFoundException_whenScrollDoesNotExist() {
    // Arrange
    // Simulate the repository not finding the scroll to update.
    when(scrollRepository.findByScrollId(SCROLL_ID)).thenReturn(Optional.empty());
    NewScroll updateDto = new NewScroll(); // The content doesn't matter for this test
    updateDto.setScrollId("some-id");

    // Act & Assert
    assertThrows(ResourceNotFoundException.class,
            () -> scrollService.updateScroll(SCROLL_ID, updateDto));

    // Verify that no save operation was ever attempted.
    verify(scrollRepository, never()).save(any());
  }

  @Test
  void updateScroll_shouldThrowResourceAlreadyExistsException_whenNewScrollIdIsTaken() {
    // Arrange
    NewScroll updateDto = new NewScroll();
    String conflictingId = "already-taken-scroll-id";
    updateDto.setScrollId(conflictingId);
    updateDto.setDisplayName("Some Name");

    // Mock the repository finding the original scroll.
    when(scrollRepository.findByScrollId(SCROLL_ID)).thenReturn(Optional.of(scroll));

    // Mock the existence check for the NEW ID to return true.
    when(scrollRepository.existsByScrollId(conflictingId)).thenReturn(true);

    // Act & Assert
    assertThrows(ResourceAlreadyExistsException.class,
            () -> scrollService.updateScroll(SCROLL_ID, updateDto));

    // Verify that no save operation was ever attempted.
    verify(scrollRepository, never()).save(any());
  }

  @Test
  void updateScroll_shouldSucceed_whenScrollIdIsNotChanged() {
    // Arrange
    // The user is updating the display name but keeping the same scrollId.
    NewScroll updateDto = new NewScroll();
    updateDto.setScrollId(SCROLL_ID); // Same ID as the original
    updateDto.setDisplayName("A New Display Name");

    when(scrollRepository.findByScrollId(SCROLL_ID)).thenReturn(Optional.of(scroll));
    when(scrollRepository.save(any(Scroll.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(scrollMapper.scrollEntityToScrollDto(any(Scroll.class))).thenReturn(new scroll.dto.Scroll());

    // Act
    scrollService.updateScroll(SCROLL_ID, updateDto);

    // Assert
    verify(scrollRepository, times(1)).save(any(Scroll.class));
  }
}
