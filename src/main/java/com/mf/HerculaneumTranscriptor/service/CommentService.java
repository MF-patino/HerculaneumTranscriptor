package com.mf.HerculaneumTranscriptor.service;

import com.mf.HerculaneumTranscriptor.exception.ResourceNotFoundException;
import comment.dto.CommentContent;
import comment.dto.GetRegionComments200Response;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Date;
import java.util.UUID;

/**
 * Service layer defining business operations related to comments on annotation regions.
 */
public interface CommentService {

  /**
   * Retrieves all comments for a specific annotation region, optionally filtered by a timestamp.
   * This is a public operation for any authenticated user.
   *
   * @param scrollId The ID of the scroll containing the region.
   * @param regionId The UUID of the annotation region.
   * @param since Optional timestamp to fetch only comments created or updated since that time. Can be null.
   * @return A response object containing the list of comments and a new sync timestamp.
   * @throws com.mf.HerculaneumTranscriptor.exception.ResourceNotFoundException if the scroll or region does not exist.
   */
  GetRegionComments200Response getRegionComments(String scrollId, UUID regionId, Date since) throws ResourceNotFoundException;

  /**
   * Adds a new comment to an annotation region. The author is automatically set
   * to the currently authenticated user.
   * Requires the user to have 'WRITE' permissions.
   *
   * @param scrollId The ID of the scroll containing the region.
   * @param regionId The UUID of the annotation region.
   * @param commentContent The DTO containing the comment's text.
   * @return The newly created Comment DTO, including server-generated fields.
   * @throws com.mf.HerculaneumTranscriptor.exception.ResourceNotFoundException if the scroll or region does not exist.
   */
  @PreAuthorize("hasRole('WRITE') or hasRole('ROOT') or hasRole('ADMIN')")
  comment.dto.Comment addCommentToRegion(String scrollId, UUID regionId, CommentContent commentContent) throws ResourceNotFoundException;

  /**
   * Deletes a comment.
   * This can only be performed by the author of the comment or by an ADMIN/ROOT user.
   *
   * @param scrollId The ID of the scroll containing the region.
   * @param regionId The UUID of the annotation region.
   * @param commentId The UUID of the comment to be deleted.
   * @throws com.mf.HerculaneumTranscriptor.exception.ResourceNotFoundException if the scroll, region, or comment does not exist.
   */
  @PreAuthorize("@securityLogic.canModifyRegion(authentication, #regionId)")
  void deleteComment(String scrollId, UUID regionId, UUID commentId) throws ResourceNotFoundException;

}



