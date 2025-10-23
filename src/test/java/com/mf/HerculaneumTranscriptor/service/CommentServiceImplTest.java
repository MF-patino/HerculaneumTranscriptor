package com.mf.HerculaneumTranscriptor.service;

import com.mf.HerculaneumTranscriptor.domain.Annotation;
import com.mf.HerculaneumTranscriptor.domain.Comment;
import com.mf.HerculaneumTranscriptor.domain.Scroll;
import com.mf.HerculaneumTranscriptor.domain.User;
import com.mf.HerculaneumTranscriptor.domain.mapper.CommentMapper;
import com.mf.HerculaneumTranscriptor.exception.ResourceNotFoundException;
import com.mf.HerculaneumTranscriptor.repository.AnnotationRepository;
import com.mf.HerculaneumTranscriptor.repository.CommentRepository;
import com.mf.HerculaneumTranscriptor.service.impl.CommentServiceImpl;
import comment.dto.GetRegionComments200Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
}
