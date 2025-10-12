package com.mf.HerculaneumTranscriptor.service.impl;

import com.mf.HerculaneumTranscriptor.exception.ResourceNotFoundException;
import com.mf.HerculaneumTranscriptor.service.CommentService;
import comment.dto.Comment;
import comment.dto.CommentContent;
import comment.dto.GetRegionComments200Response;

import java.util.Date;
import java.util.UUID;

public class CommentServiceImpl implements CommentService {
  @Override
  public GetRegionComments200Response getRegionComments(String scrollId, UUID regionId, Date since) throws ResourceNotFoundException {
    return null;
  }

  @Override
  public Comment addCommentToRegion(String scrollId, UUID regionId, CommentContent commentContent) throws ResourceNotFoundException {
    return null;
  }

  @Override
  public void deleteComment(String scrollId, UUID regionId, UUID commentId) throws ResourceNotFoundException {

  }
}
