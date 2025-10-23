package com.mf.HerculaneumTranscriptor.service.impl;

import com.mf.HerculaneumTranscriptor.domain.Annotation;
import com.mf.HerculaneumTranscriptor.domain.User;
import com.mf.HerculaneumTranscriptor.domain.mapper.CommentMapper;
import com.mf.HerculaneumTranscriptor.exception.ResourceNotFoundException;
import com.mf.HerculaneumTranscriptor.repository.AnnotationRepository;
import com.mf.HerculaneumTranscriptor.repository.CommentRepository;
import com.mf.HerculaneumTranscriptor.security.JwtUserDetails;
import com.mf.HerculaneumTranscriptor.service.CommentService;
import com.mf.HerculaneumTranscriptor.domain.Comment;
import comment.dto.CommentContent;
import comment.dto.GetRegionComments200Response;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {
  private final AnnotationRepository annotationRepository;
  private final CommentRepository commentRepository;
  private final CommentMapper commentMapper;

  @Override
  public GetRegionComments200Response getRegionComments(String scrollId, UUID regionId, Date since) throws ResourceNotFoundException {
    // Verify that the parent annotation exists and belongs to the scroll.
    Annotation parentAnnotation = annotationRepository.findByRegionId(regionId)
            .orElseThrow(() -> new ResourceNotFoundException("Annotation not found with ID: " + regionId));
    if (!parentAnnotation.getScroll().getScrollId().equals(scrollId)) {
      throw new ResourceNotFoundException("Conflict: The specified region does not belong to the specified scroll.");
    }

    // Decide which repository method to call based on the 'since' parameter.
    List<Comment> comments;
    if (since == null) {
      comments = commentRepository.findByAnnotationRegionIdOrderByCreatedAtDesc(regionId);
    } else {
      comments = commentRepository.findByAnnotationRegionIdAndUpdatedAtAfterOrderByCreatedAtDesc(regionId, since);
    }

    // Map the entities to DTOs.
    List<comment.dto.Comment> commentDtos = comments.stream()
            .map(commentMapper::commentEntityToCommentDto)
            .collect(Collectors.toList());

    // Build the final response object.
    GetRegionComments200Response response = new GetRegionComments200Response();
    response.setComments(commentDtos);
    response.setLastSyncTimestamp(Date.from(Instant.now()));

    return response;
  }

  @Override
  public comment.dto.Comment addCommentToRegion(String scrollId, UUID regionId, CommentContent commentContent) throws ResourceNotFoundException {
    // Find the parent annotation and verify it belongs to the scroll.
    Annotation parentAnnotation = annotationRepository.findByRegionId(regionId)
            .orElseThrow(() -> new ResourceNotFoundException("Cannot add comment: Annotation not found with ID: " + regionId));
    if (!parentAnnotation.getScroll().getScrollId().equals(scrollId)) {
      throw new ResourceNotFoundException("Conflict: The specified region does not belong to the specified scroll.");
    }

    // Find the author of the comment from the security context.
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    User author;

    if (authentication.getPrincipal() instanceof JwtUserDetails)
      author = ((JwtUserDetails) authentication.getPrincipal()).getUser();
    else throw new BadCredentialsException("User not authenticated through a JWT");

    // Map DTO to entity and enrich with server-side data.
    Comment newComment = commentMapper.commentContentDtoToCommentEntity(commentContent);
    newComment.setCommentId(UUID.randomUUID()); // Generate a new, unique public ID
    newComment.setAuthor(author);
    newComment.setAnnotation(parentAnnotation);

    // Save the new entity.
    Comment savedComment = commentRepository.save(newComment);

    // Map the saved entity back to a DTO for the response.
    return commentMapper.commentEntityToCommentDto(savedComment);
  }

  @Override
  public void deleteComment(String scrollId, UUID regionId, UUID commentId) throws ResourceNotFoundException {
    Comment commentToDelete = commentRepository.findByCommentId(commentId)
            .orElseThrow(() -> new ResourceNotFoundException("Comment not found with ID: " + commentId));

    // Verify the comment belongs to the correct annotation and scroll.
    if (!commentToDelete.getAnnotation().getRegionId().equals(regionId) ||
            !commentToDelete.getAnnotation().getScroll().getScrollId().equals(scrollId)) {
      throw new ResourceNotFoundException("The comment does not belong to the specified region/scroll path.");
    }

    // Delete the comment.
    commentRepository.delete(commentToDelete);
  }
}
