package com.mf.HerculaneumTranscriptor.service;

import com.mf.HerculaneumTranscriptor.domain.Annotation;
import com.mf.HerculaneumTranscriptor.domain.Comment;
import com.mf.HerculaneumTranscriptor.domain.Scroll;
import com.mf.HerculaneumTranscriptor.domain.User;
import com.mf.HerculaneumTranscriptor.domain.mapper.CommentMapper;
import com.mf.HerculaneumTranscriptor.exception.ResourceNotFoundException;
import com.mf.HerculaneumTranscriptor.repository.AnnotationRepository;
import com.mf.HerculaneumTranscriptor.repository.CommentRepository;
import com.mf.HerculaneumTranscriptor.security.JwtUserDetails;
import com.mf.HerculaneumTranscriptor.service.impl.CommentServiceImpl;
import comment.dto.CommentContent;
import comment.dto.GetRegionComments200Response;
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
public class CommentServiceImplTest {
  @InjectMocks
  private CommentServiceImpl commentService;

  @Mock
  private AnnotationRepository annotationRepository;
  @Mock
  private CommentRepository commentRepository;
  @Mock
  private CommentMapper commentMapper;

  private Annotation annotation;
  private User author;
  private Comment comment;
  private comment.dto.Comment commentDto;

  private static final String SCROLL_ID = "vesuvius-scroll-1";
  private static final UUID REGION_ID = UUID.randomUUID();
  private static final UUID COMMENT_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    Scroll scroll = new Scroll();
    scroll.setScrollId(SCROLL_ID);

    author = new User();
    author.setUsername("testuser");
    author.setPermissions(UserInfo.PermissionsEnum.READ);

    annotation = new Annotation();
    annotation.setRegionId(REGION_ID);
    annotation.setScroll(scroll);

    comment = new Comment();
    comment.setCommentId(COMMENT_ID);
    comment.setAnnotation(annotation);
    comment.setAuthor(author);
    comment.setText("Test comment");

    commentDto = new comment.dto.Comment();
    commentDto.setCommentId(COMMENT_ID);
    commentDto.setText("Test comment");
    commentDto.setAuthorUsername("testuser");
  }

  // Tests for getRegionComments

  @Test
  void getRegionComments_shouldReturnAllComments_whenSinceIsNull() {
    // Arrange
    when(annotationRepository.findByRegionId(REGION_ID)).thenReturn(Optional.of(annotation));
    when(commentRepository.findByAnnotationRegionIdOrderByCreatedAtDesc(REGION_ID)).thenReturn(List.of(comment));
    when(commentMapper.commentEntityToCommentDto(comment)).thenReturn(commentDto);

    // Act
    GetRegionComments200Response response = commentService.getRegionComments(SCROLL_ID, REGION_ID, null);

    // Assert
    assertThat(response).isNotNull();
    assertThat(response.getComments()).isNotNull();
    assertThat(response.getComments().size()).isEqualTo(1);
    assertThat(response.getComments().getFirst()).isEqualTo(commentDto);
    verify(commentRepository, times(1)).findByAnnotationRegionIdOrderByCreatedAtDesc(REGION_ID);
  }

  @Test
  void getRegionComments_shouldReturnDeltaComments_whenSinceIsProvided() {
    // Arrange
    Date since = Date.from(Instant.now().minusSeconds(60));
    when(annotationRepository.findByRegionId(REGION_ID)).thenReturn(Optional.of(annotation));
    when(commentRepository.findByAnnotationRegionIdAndUpdatedAtAfterOrderByCreatedAtDesc(REGION_ID, since)).thenReturn(List.of(comment));
    when(commentMapper.commentEntityToCommentDto(comment)).thenReturn(commentDto);

    // Act
    GetRegionComments200Response response = commentService.getRegionComments(SCROLL_ID, REGION_ID, since);

    // Assert
    assertThat(response.getComments().size()).isEqualTo(1);
    verify(commentRepository, times(1)).findByAnnotationRegionIdAndUpdatedAtAfterOrderByCreatedAtDesc(REGION_ID, since);
  }

  @Test
  void getRegionComments_shouldThrowResourceNotFound_whenAnnotationDoesNotExist() {
    // Arrange
    when(annotationRepository.findByRegionId(REGION_ID)).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(ResourceNotFoundException.class,
            () -> commentService.getRegionComments(SCROLL_ID, REGION_ID, null));
  }

  // Tests for addCommentToRegion

  @Test
  void addCommentToRegion_shouldCreateAndReturnComment_whenDataIsValid() {
    // Arrange
    CommentContent newCommentContent = new CommentContent().text("New content");

    // Mock the security context to provide an authenticated user
    Authentication authentication = mock(Authentication.class);
    UserDetails userDetails = new JwtUserDetails(author);

    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);
    when(authentication.getPrincipal()).thenReturn(userDetails);

    when(annotationRepository.findByRegionId(REGION_ID)).thenReturn(Optional.of(annotation));
    when(commentMapper.commentContentDtoToCommentEntity(newCommentContent)).thenReturn(new Comment());
    when(commentRepository.save(any(Comment.class))).thenReturn(comment);
    when(commentMapper.commentEntityToCommentDto(comment)).thenReturn(commentDto);

    // Act
    comment.dto.Comment result = commentService.addCommentToRegion(SCROLL_ID, REGION_ID, newCommentContent);

    // Assert
    assertThat(result).isEqualTo(commentDto);

    ArgumentCaptor<Comment> commentCaptor = ArgumentCaptor.forClass(Comment.class);
    verify(commentRepository).save(commentCaptor.capture());
    Comment savedComment = commentCaptor.getValue();

    assertThat(savedComment.getAuthor()).isEqualTo(author);
    assertThat(savedComment.getAnnotation()).isEqualTo(annotation);

    SecurityContextHolder.clearContext();
  }

  @Test
  void addCommentToRegion_shouldThrowResourceNotFound_whenAnnotationDoesNotExist() {
    // Arrange
    when(annotationRepository.findByRegionId(REGION_ID)).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(ResourceNotFoundException.class,
            () -> commentService.addCommentToRegion(SCROLL_ID, REGION_ID, new CommentContent().text("New content")));
  }

  @Test
  void addCommentToRegion_shouldThrowBadCredentialsException_whenUserNotAuthenticatedThroughJWT() {
    // Arrange
    Authentication authentication = mock(Authentication.class);
    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);
    // Simulate the authenticated user not having been authenticated via JWT filters
    when(authentication.getPrincipal()).thenReturn("userName");

    when(annotationRepository.findByRegionId(REGION_ID)).thenReturn(Optional.of(annotation));

    // Act & Assert
    assertThrows(BadCredentialsException.class,
            () -> commentService.addCommentToRegion(SCROLL_ID, REGION_ID, new CommentContent().text("New content")));

    SecurityContextHolder.clearContext();
  }

  // Tests for deleteComment

  @Test
  void deleteComment_shouldDeleteComment_whenDataIsValid() {
    // Arrange
    when(commentRepository.findByCommentId(COMMENT_ID)).thenReturn(Optional.of(comment));
    doNothing().when(commentRepository).delete(comment);

    // Act
    commentService.deleteComment(SCROLL_ID, REGION_ID, COMMENT_ID);

    // Assert
    verify(commentRepository, times(1)).delete(comment);
  }

  @Test
  void deleteComment_shouldThrowResourceNotFound_whenCommentDoesNotExist() {
    // Arrange
    when(commentRepository.findByCommentId(COMMENT_ID)).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(ResourceNotFoundException.class,
            () -> commentService.deleteComment(SCROLL_ID, REGION_ID, COMMENT_ID));
    verify(commentRepository, never()).delete(any());
  }

  @Test
  void deleteComment_shouldThrowResourceNotFound_whenCommentDoesNotBelongToRegion() {
    // Arrange
    UUID wrongRegionId = UUID.randomUUID();
    when(commentRepository.findByCommentId(COMMENT_ID)).thenReturn(Optional.of(comment));

    // Act & Assert
    assertThrows(ResourceNotFoundException.class,
            () -> commentService.deleteComment(SCROLL_ID, wrongRegionId, COMMENT_ID));
    verify(commentRepository, never()).delete(any());
  }
}
