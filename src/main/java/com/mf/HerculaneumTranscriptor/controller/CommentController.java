package com.mf.HerculaneumTranscriptor.controller;

import com.mf.HerculaneumTranscriptor.service.CommentService;
import comment.api.CommentsApi;
import comment.dto.Comment;
import comment.dto.CommentContent;
import comment.dto.GetRegionComments200Response;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.UUID;

@RestController
@AllArgsConstructor
public class CommentController implements CommentsApi {
  private final CommentService commentService;

  @Override
  public ResponseEntity<Void> addCommentToRegion(String scrollId, UUID regionId, CommentContent commentContent) {
    return null;
  }

  @Override
  public ResponseEntity<Void> deleteComment(String scrollId, UUID regionId, UUID commentId) {
    return null;
  }

  @Override
  public ResponseEntity<GetRegionComments200Response> getRegionComments(String scrollId, UUID regionId, Date since) {
    GetRegionComments200Response response = commentService.getRegionComments(scrollId, regionId, since);
    return ResponseEntity.ok(response);
  }
}
