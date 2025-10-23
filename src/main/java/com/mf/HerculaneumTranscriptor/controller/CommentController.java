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
  public ResponseEntity<Comment> addCommentToRegion(String scrollId, UUID regionId, CommentContent commentContent) {
    Comment response = commentService.addCommentToRegion(scrollId, regionId, commentContent);
    return new ResponseEntity<>(response, HttpStatus.CREATED);
  }

  @Override
  public ResponseEntity<Void> deleteComment(String scrollId, UUID regionId, UUID commentId) {
    commentService.deleteComment(scrollId, regionId, commentId);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<GetRegionComments200Response> getRegionComments(String scrollId, UUID regionId, Date since) {
    GetRegionComments200Response response = commentService.getRegionComments(scrollId, regionId, since);
    return ResponseEntity.ok(response);
  }
}
