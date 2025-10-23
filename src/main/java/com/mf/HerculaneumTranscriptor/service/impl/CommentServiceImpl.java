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
  public Comment addCommentToRegion(String scrollId, UUID regionId, CommentContent commentContent) throws ResourceNotFoundException {
    return null;
  }

  @Override
  public void deleteComment(String scrollId, UUID regionId, UUID commentId) throws ResourceNotFoundException {

  }
}
