package com.mf.HerculaneumTranscriptor.service;

import annotation.dto.BoxRegion;
import annotation.dto.NewBoxRegion;
import annotation.dto.RegionUpdateResponse;
import com.mf.HerculaneumTranscriptor.domain.Annotation;
import com.mf.HerculaneumTranscriptor.domain.Coordinates;
import com.mf.HerculaneumTranscriptor.domain.Scroll;
import com.mf.HerculaneumTranscriptor.domain.User;
import com.mf.HerculaneumTranscriptor.domain.mapper.AnnotationMapper;
import com.mf.HerculaneumTranscriptor.exception.ResourceNotFoundException;
import com.mf.HerculaneumTranscriptor.repository.AnnotationRepository;
import com.mf.HerculaneumTranscriptor.repository.ScrollRepository;
import com.mf.HerculaneumTranscriptor.security.JwtUserDetails;
import com.mf.HerculaneumTranscriptor.service.impl.AnnotationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import user.dto.UserInfo;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AnnotationServiceImplTest {
  @InjectMocks
  private AnnotationServiceImpl annotationService;

  @Mock
  private AnnotationRepository annotationRepository;
  @Mock
  private ScrollRepository scrollRepository;
  @Mock
  private AnnotationMapper annotationMapper;

  private Scroll scroll;
  private User author;
  private Annotation annotation;
  private BoxRegion boxRegionDto;
  private NewBoxRegion newBoxRegionDto;

  private static final UUID REGION_ID = UUID.randomUUID();
  private static final String SCROLL_ID = "vesuvius-scroll-1";
  private static final String USERNAME = "testUser";

  @BeforeEach
  void setUp() {
    scroll = new Scroll();
    scroll.setId(1L);
    scroll.setScrollId(SCROLL_ID);

    author = new User();
    author.setId(1L);
    author.setUsername(USERNAME);
    author.setPermissions(UserInfo.PermissionsEnum.READ);

    annotation.dto.Coordinates coordinatesDto = new annotation.dto.Coordinates();
    coordinatesDto.setX(0f);
    coordinatesDto.setY(0f);
    coordinatesDto.setWidth(100f);
    coordinatesDto.setHeight(100f);

    Coordinates coordinates = new Coordinates();
    coordinates.setX(0f);
    coordinates.setY(0f);
    coordinates.setWidth(100f);
    coordinates.setHeight(100f);

    annotation = new Annotation();
    annotation.setId(1L);
    annotation.setAuthor(author);
    annotation.setScroll(scroll);
    annotation.setCoordinates(coordinates);

    newBoxRegionDto = new NewBoxRegion();
    newBoxRegionDto.setCoordinates(coordinatesDto);
    newBoxRegionDto.setTranscription("test transcription");

    boxRegionDto = new BoxRegion();
    boxRegionDto.setBasicInfo(newBoxRegionDto);
    boxRegionDto.setAuthorUsername(USERNAME);
    boxRegionDto.setRegionId(REGION_ID);
    boxRegionDto.setCertaintyScore(0f);
    boxRegionDto.setCreatedAt(new Date());
    boxRegionDto.setUpdatedAt(new Date());
  }

  // Tests for getScrollRegions

  @Test
  void getScrollRegions_shouldReturnAllRegions_whenSinceIsNull() {
    // Arrange
    when(scrollRepository.existsByScrollId(SCROLL_ID)).thenReturn(true);
    when(annotationRepository.findByScrollScrollId(SCROLL_ID)).thenReturn(List.of(annotation));
    when(annotationMapper.annotationEntityToBoxRegionDto(annotation)).thenReturn(boxRegionDto);

    // Act
    RegionUpdateResponse response = annotationService.getScrollRegions(SCROLL_ID, null);

    // Assert
    assertThat(response).isNotNull();
    assertThat(response.getRegions()).isNotNull();
    assertThat(response.getRegions().size()).isEqualTo(1);
    assertThat(response.getRegions().getFirst()).isEqualTo(boxRegionDto);
    assertThat(response.getLastSyncTimestamp()).isNotNull();
    verify(annotationRepository, times(1)).findByScrollScrollId(SCROLL_ID);
    verify(annotationRepository, never()).findByScrollScrollIdAndUpdatedAtAfter(anyString(), any(Date.class));
  }

  @Test
  void getScrollRegions_shouldReturnDeltaRegions_whenSinceIsProvided() {
    // Arrange
    Date since = Date.from(Instant.now().minusSeconds(60));
    when(scrollRepository.existsByScrollId(SCROLL_ID)).thenReturn(true);
    when(annotationRepository.findByScrollScrollIdAndUpdatedAtAfter(SCROLL_ID, since)).thenReturn(List.of(annotation));
    when(annotationMapper.annotationEntityToBoxRegionDto(annotation)).thenReturn(boxRegionDto);

    // Act
    RegionUpdateResponse response = annotationService.getScrollRegions(SCROLL_ID, since);

    // Assert
    assertThat(response).isNotNull();
    assertThat(response.getRegions().size()).isEqualTo(1);
    verify(annotationRepository, never()).findByScrollScrollId(anyString());
    verify(annotationRepository, times(1)).findByScrollScrollIdAndUpdatedAtAfter(SCROLL_ID, since);
  }

  @Test
  void getScrollRegions_shouldThrowResourceNotFoundException_whenScrollDoesNotExist() {
    // Arrange
    when(scrollRepository.existsByScrollId(SCROLL_ID)).thenReturn(false);

    // Act & Assert
    assertThrows(ResourceNotFoundException.class,
            () -> annotationService.getScrollRegions(SCROLL_ID, null));
  }

  // Tests for createRegion

  @Test
  void createRegion_shouldCreateAndReturnRegion_whenDataIsValid() {
    // Arrange
    // Mock the security context to provide an authenticated user
    Authentication authentication = mock(Authentication.class);
    UserDetails userDetails = new JwtUserDetails(author);

    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);
    when(authentication.getPrincipal()).thenReturn(userDetails);

    // Mock repository calls
    when(scrollRepository.findByScrollId(SCROLL_ID)).thenReturn(Optional.of(scroll));
    when(annotationMapper.newBoxRegionDtoToAnnotationEntity(newBoxRegionDto)).thenReturn(new Annotation()); // Return a fresh instance
    when(annotationRepository.save(any(Annotation.class))).thenReturn(annotation); // Return the final, saved entity
    when(annotationMapper.annotationEntityToBoxRegionDto(annotation)).thenReturn(boxRegionDto);

    // Act
    BoxRegion result = annotationService.createRegion(SCROLL_ID, newBoxRegionDto);

    // Assert
    assertThat(result).isEqualTo(boxRegionDto);

    // Use an ArgumentCaptor to verify the entity was correctly populated before saving
    ArgumentCaptor<Annotation> annotationCaptor = ArgumentCaptor.forClass(Annotation.class);
    verify(annotationRepository).save(annotationCaptor.capture());
    Annotation savedAnnotation = annotationCaptor.getValue();

    assertThat(savedAnnotation.getAuthor()).isEqualTo(author);
    assertThat(savedAnnotation.getScroll()).isEqualTo(scroll);

    // Clean up the security context
    SecurityContextHolder.clearContext();
  }

  @Test
  void createRegion_shouldThrowResourceNotFoundException_whenScrollDoesNotExist() {
    // Arrange
    when(scrollRepository.findByScrollId(SCROLL_ID)).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(ResourceNotFoundException.class,
            () -> annotationService.createRegion(SCROLL_ID, newBoxRegionDto));
    verify(annotationRepository, never()).save(any());
  }

  @Test
  void createRegion_shouldThrowBadCredentialsException_whenAuthenticatedUserNotInDb() {
    // Arrange
    Authentication authentication = mock(Authentication.class);
    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);
    // Simulate the authenticated user not having been authenticated via JWT filters
    when(authentication.getPrincipal()).thenReturn(USERNAME);

    when(scrollRepository.findByScrollId(SCROLL_ID)).thenReturn(Optional.of(scroll));

    // Act & Assert
    assertThrows(BadCredentialsException.class,
            () -> annotationService.createRegion(SCROLL_ID, newBoxRegionDto));

    SecurityContextHolder.clearContext();
  }
}
