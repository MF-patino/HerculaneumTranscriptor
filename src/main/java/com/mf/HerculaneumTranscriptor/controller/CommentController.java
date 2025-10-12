package com.mf.HerculaneumTranscriptor.controller;

import comment.api.CommentsApi;
import comment.dto.CommentContent;
import comment.dto.GetRegionComments200Response;
import org.springframework.http.ResponseEntity;

import java.util.Date;
import java.util.UUID;

public class CommentController implements CommentsApi {
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
    return null;
  }
}
