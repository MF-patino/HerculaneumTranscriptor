package com.mf.HerculaneumTranscriptor.service;

import annotation.dto.BoxRegion;
import annotation.dto.NewBoxRegion;
import annotation.dto.RegionUpdateResponse;
import com.mf.HerculaneumTranscriptor.domain.*;
import com.mf.HerculaneumTranscriptor.domain.mapper.AnnotationMapper;
import com.mf.HerculaneumTranscriptor.exception.ResourceNotFoundException;
import com.mf.HerculaneumTranscriptor.repository.AnnotationRepository;
import com.mf.HerculaneumTranscriptor.repository.ScrollRepository;
import com.mf.HerculaneumTranscriptor.repository.VoteRepository;
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
  @Mock
  private VoteRepository voteRepository;

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
    annotation.setTranscription("test transcription");

    newBoxRegionDto = new NewBoxRegion();
    newBoxRegionDto.setCoordinates(coordinatesDto);
    newBoxRegionDto.setTranscription(annotation.getTranscription());

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
  void createRegion_shouldThrowBadCredentialsException_whenUserNotAuthenticatedThroughJWT() {
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

  // Tests for deleteRegion

  @Test
  void deleteRegion_shouldDeleteAnnotation_whenRegionExistsAndBelongsToScroll() {
    // Arrange
    when(annotationRepository.findByRegionId(annotation.getRegionId())).thenReturn(Optional.of(annotation));
    doNothing().when(annotationRepository).delete(any(Annotation.class));

    // Act
    annotationService.deleteRegion(SCROLL_ID, annotation.getRegionId());

    // Assert
    // Verify that the repository's delete method was called exactly once.
    verify(annotationRepository, times(1)).delete(annotation);
  }

  @Test
  void deleteRegion_shouldThrowResourceNotFoundException_whenRegionDoesNotExist() {
    // Arrange
    UUID nonExistentRegionId = UUID.randomUUID();
    when(annotationRepository.findByRegionId(nonExistentRegionId)).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(ResourceNotFoundException.class,
            () -> annotationService.deleteRegion(SCROLL_ID, nonExistentRegionId));

    // Verify that the delete method was never called.
    verify(annotationRepository, never()).delete(any());
  }

  @Test
  void deleteRegion_shouldThrowResourceNotFoundException_whenRegionDoesNotBelongToScroll() {
    // Arrange
    String wrongScrollId = SCROLL_ID + "-wrong";

    when(annotationRepository.findByRegionId(annotation.getRegionId())).thenReturn(Optional.of(annotation));

    // Act & Assert
    // Call the service with the WRONG scrollId and assert the exception.
    assertThrows(ResourceNotFoundException.class,
            () -> annotationService.deleteRegion(wrongScrollId, annotation.getRegionId()));

    // Verify that the delete method was never called because the check failed.
    verify(annotationRepository, never()).delete(any());
  }

  // Tests for updateRegion

  @Test
  void updateRegion_shouldUpdateAndReturnRegion_whenDataIsValid() {
    // Arrange
    Coordinates mappedCoords = new Coordinates(10f, 20f, 100f, 200f);
    String updatedTranscription = "updated " + newBoxRegionDto.getTranscription();
    newBoxRegionDto.setTranscription(updatedTranscription);

    // Mock the repository to find the existing annotation.
    when(annotationRepository.findByRegionId(annotation.getRegionId())).thenReturn(Optional.of(annotation));

    // Mock the save operation. Using thenAnswer is a robust way to return the modified object.
    when(annotationRepository.save(any(Annotation.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // Mock the mapper calls.
    when(annotationMapper.coordinatesDtoToEntityCoordinates(any(annotation.dto.Coordinates.class))).thenReturn(mappedCoords);
    when(annotationMapper.annotationEntityToBoxRegionDto(any(Annotation.class))).thenReturn(boxRegionDto);

    // Act
    BoxRegion result = annotationService.updateRegion(SCROLL_ID, annotation.getRegionId(), newBoxRegionDto);

    // Assert
    assertThat(result).isEqualTo(boxRegionDto);

    // Use an ArgumentCaptor to inspect the entity that was saved.
    ArgumentCaptor<Annotation> annotationCaptor = ArgumentCaptor.forClass(Annotation.class);
    verify(annotationRepository).save(annotationCaptor.capture());
    Annotation savedAnnotation = annotationCaptor.getValue();

    // Verify that the entity's fields were correctly updated.
    assertThat(savedAnnotation.getTranscription()).isEqualTo(updatedTranscription);
    assertThat(savedAnnotation.getCoordinates()).isEqualTo(mappedCoords);
  }

  @Test
  void updateRegion_shouldThrowResourceNotFoundException_whenRegionDoesNotExist() {
    // Arrange
    UUID nonExistentRegionId = UUID.randomUUID();
    // Mock the repository to return empty, simulating "not found".
    when(annotationRepository.findByRegionId(nonExistentRegionId)).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(ResourceNotFoundException.class,
            () -> annotationService.updateRegion(SCROLL_ID, nonExistentRegionId, newBoxRegionDto));

    // Verify no save was ever attempted.
    verify(annotationRepository, never()).save(any());
  }

  @Test
  void updateRegion_shouldThrowResourceNotFoundException_whenRegionDoesNotBelongToScroll() {
    // Arrange
    String wrongScrollId = SCROLL_ID + "-wrong";
    // Mock the repository to successfully find the annotation.
    when(annotationRepository.findByRegionId(annotation.getRegionId())).thenReturn(Optional.of(annotation));

    // Act & Assert
    // Call the service with the wrong scroll ID.
    assertThrows(ResourceNotFoundException.class,
            () -> annotationService.updateRegion(wrongScrollId, annotation.getRegionId(), newBoxRegionDto));

    // Verify no save was ever attempted.
    verify(annotationRepository, never()).save(any());
  }

  // Tests for voteOnRegion

  @Test
  void voteOnRegion_shouldCreateNewVote_whenUserVotesForTheFirstTime() {
    // Arrange
    // Mock the security context to provide an authenticated user
    Authentication authentication = mock(Authentication.class);
    UserDetails userDetails = new JwtUserDetails(author);

    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);
    when(authentication.getPrincipal()).thenReturn(userDetails);

    annotation.dto.Vote voteDto = new annotation.dto.Vote().vote(5);

    when(annotationRepository.findByRegionId(annotation.getRegionId())).thenReturn(Optional.of(annotation));
    // Simulate that no existing vote is found
    when(voteRepository.findByUserAndAnnotation(author, annotation)).thenReturn(Optional.empty());

    // Mock the average calculation to return a new average
    when(voteRepository.calculateAverageVote(annotation.getId())).thenReturn(4.5f);

    // Mock the final save and map calls
    when(annotationRepository.save(any(Annotation.class))).thenReturn(annotation);
    when(annotationMapper.annotationEntityToBoxRegionDto(any(Annotation.class))).thenReturn(boxRegionDto);

    // Act
    BoxRegion result = annotationService.voteOnRegion(SCROLL_ID, annotation.getRegionId(), voteDto);

    // Assert
    // Check the final result
    assertThat(result).isEqualTo(boxRegionDto);

    // Capture the Vote entity to verify it was created correctly
    ArgumentCaptor<Vote> voteCaptor = ArgumentCaptor.forClass(Vote.class);
    verify(voteRepository).save(voteCaptor.capture());
    Vote savedVote = voteCaptor.getValue();
    assertThat(savedVote.getUser()).isEqualTo(author);
    assertThat(savedVote.getAnnotation()).isEqualTo(annotation);
    assertThat(savedVote.getVoteValue()).isEqualTo(5);

    // Capture the Annotation entity to verify the score was updated
    ArgumentCaptor<Annotation> annotationCaptor = ArgumentCaptor.forClass(Annotation.class);
    verify(annotationRepository, times(1)).save(annotationCaptor.capture());
    Annotation savedAnnotation = annotationCaptor.getValue();
    assertThat(savedAnnotation.getCertaintyScore()).isEqualTo(4.5f);

    // Clean up context
    SecurityContextHolder.clearContext();
  }

  @Test
  void voteOnRegion_shouldUpdateExistingVote_whenUserVotesAgain() {
    // Arrange
    // Mock the security context to provide an authenticated user
    Authentication authentication = mock(Authentication.class);
    UserDetails userDetails = new JwtUserDetails(author);

    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);
    when(authentication.getPrincipal()).thenReturn(userDetails);

    // Prepare DTOs and Mocks
    annotation.dto.Vote voteDto = new annotation.dto.Vote().vote(3); // The new vote value
    Vote existingVote = new Vote(author, annotation, 5); // The old vote

    when(annotationRepository.findByRegionId(annotation.getRegionId())).thenReturn(Optional.of(annotation));
    // Simulate finding the existing vote this time
    when(voteRepository.findByUserAndAnnotation(author, annotation)).thenReturn(Optional.of(existingVote));

    when(voteRepository.calculateAverageVote(annotation.getId())).thenReturn(3.5f);
    when(annotationRepository.save(any(Annotation.class))).thenReturn(annotation);
    when(annotationMapper.annotationEntityToBoxRegionDto(any(Annotation.class))).thenReturn(boxRegionDto);

    // Act
    annotationService.voteOnRegion(SCROLL_ID, annotation.getRegionId(), voteDto);

    // Assert
    // Capture the Vote entity and verify its value was updated before saving
    ArgumentCaptor<Vote> voteCaptor = ArgumentCaptor.forClass(Vote.class);
    verify(voteRepository).save(voteCaptor.capture());
    Vote savedVote = voteCaptor.getValue();
    assertThat(savedVote.getVoteValue()).isEqualTo(3); // Check that the value was changed

    // Verify the annotation's score was updated
    ArgumentCaptor<Annotation> annotationCaptor = ArgumentCaptor.forClass(Annotation.class);
    verify(annotationRepository).save(annotationCaptor.capture());
    Annotation savedAnnotation = annotationCaptor.getValue();
    assertThat(savedAnnotation.getCertaintyScore()).isEqualTo(3.5f);

    // Clean up context
    SecurityContextHolder.clearContext();
  }

  @Test
  void voteOnRegion_shouldThrowBadCredentialsException_whenUserNotAuthorizedThroughJWT() {
    // Arrange
    Authentication authentication = mock(Authentication.class);
    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);
    // Simulate the authenticated user not having been authenticated via JWT filters
    when(authentication.getPrincipal()).thenReturn(USERNAME);

    // Prepare DTOs and Mocks
    annotation.dto.Vote voteDto = new annotation.dto.Vote().vote(3); // The new vote value

    when(annotationRepository.findByRegionId(annotation.getRegionId())).thenReturn(Optional.of(annotation));

    // Act & Assert
    assertThrows(BadCredentialsException.class,
            () -> annotationService.voteOnRegion(SCROLL_ID, annotation.getRegionId(), voteDto));

    // Clean up context
    SecurityContextHolder.clearContext();
  }

  @Test
  void voteOnRegion_shouldThrowResourceNotFoundException_whenAnnotationDoesNotExist() {
    // Arrange
    UUID nonExistentRegionId = UUID.randomUUID();
    annotation.dto.Vote voteDto = new annotation.dto.Vote().vote(4);
    when(annotationRepository.findByRegionId(nonExistentRegionId)).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(ResourceNotFoundException.class,
            () -> annotationService.voteOnRegion(SCROLL_ID, nonExistentRegionId, voteDto));

    verify(voteRepository, never()).save(any(Vote.class));
    verify(annotationRepository, never()).save(any(Annotation.class));
  }

  @Test
  void voteOnRegion_shouldThrowResourceNotFoundException_whenAnnotationDoesNotBelongToScroll() {
    // Arrange
    String wrongScrollId = SCROLL_ID + "-wrong";
    annotation.dto.Vote voteDto = new annotation.dto.Vote().vote(4);
    when(annotationRepository.findByRegionId(annotation.getRegionId())).thenReturn(Optional.of(annotation));

    // Act & Assert
    assertThrows(ResourceNotFoundException.class,
            () -> annotationService.voteOnRegion(wrongScrollId, annotation.getRegionId(), voteDto));

    verify(voteRepository, never()).save(any(Vote.class));
    verify(annotationRepository, never()).save(any(Annotation.class));
  }
}
