package com.mf.HerculaneumTranscriptor.service;

import com.mf.HerculaneumTranscriptor.exception.ResourceAlreadyExistsException;
import com.mf.HerculaneumTranscriptor.exception.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import annotation.dto.BoxRegion;
import annotation.dto.NewBoxRegion;
import annotation.dto.RegionUpdateResponse;
import annotation.dto.Vote;

import java.util.Date;
import java.util.UUID;

/**
 * Service layer defining business operations related to scroll annotations (regions).
 * This interface is independent of the web/controller layer.
 */
public interface AnnotationService {

  /**
   * Retrieves all box regions for a given scroll, optionally filtered by a timestamp.
   * This is used for initial client data loads and subsequent delta synchronization.
   * Any authenticated user can perform this action.
   *
   * @param scrollId The unique identifier of the scroll.
   * @param since Optional timestamp to fetch only regions created or updated since that time.
   * @return A RegionUpdateResponse containing the list of regions and a new sync timestamp.
   * @throws com.mf.HerculaneumTranscriptor.exception.ResourceNotFoundException if the scroll does not exist.
   */
  RegionUpdateResponse getScrollRegions(String scrollId, Date since) throws ResourceNotFoundException;

  /**
   * Creates a new annotation box region on a scroll. The author is automatically set
   * to the currently authenticated user.
   * Requires the user to have at least 'WRITE' permissions.
   *
   * @param scrollId The identifier of the scroll to add the region to.
   * @param newRegion The DTO containing the new region's coordinates and transcription.
   * @return The newly created BoxRegion DTO, including server-generated fields like regionId and author.
   * @throws com.mf.HerculaneumTranscriptor.exception.ResourceNotFoundException if the scroll does not exist.
   */
  @PreAuthorize("hasRole('WRITE') or hasRole('ROOT') or hasRole('ADMIN')")
  BoxRegion createRegion(String scrollId, NewBoxRegion newRegion) throws ResourceNotFoundException;

  /**
   * Updates an existing annotation box region.
   * Requires the user to have at least 'WRITE' permissions, and they must either be the
   * original author of the region or have 'ADMIN'/'ROOT' permissions.
   *
   * @param scrollId The identifier of the scroll containing the region.
   * @param regionId The unique identifier of the region to update.
   * @param updatedRegion The DTO with the new information for the region.
   * @return The updated BoxRegion DTO.
   * @throws com.mf.HerculaneumTranscriptor.exception.ResourceNotFoundException if the scroll or region does not exist.
   */
  @PreAuthorize("(hasRole('ROOT') or hasRole('ADMIN')) or (hasRole('WRITE') and @securityLogic.canModifyRegion(authentication, #regionId))")
  BoxRegion updateRegion(String scrollId, UUID regionId, NewBoxRegion updatedRegion) throws ResourceNotFoundException;

  /**
   * Deletes an annotation box region.
   * Requires the user to have at least 'WRITE' permissions, and they must either be the
   * original author of the region or have 'ADMIN'/'ROOT' permissions.
   *
   * @param scrollId The identifier of the scroll containing the region.
   * @param regionId The unique identifier of the region to delete.
   * @throws com.mf.HerculaneumTranscriptor.exception.ResourceNotFoundException if the scroll or region does not exist.
   */
  @PreAuthorize("(hasRole('ROOT') or hasRole('ADMIN')) or (hasRole('WRITE') and @securityLogic.canModifyRegion(authentication, #regionId))")
  void deleteRegion(String scrollId, UUID regionId) throws ResourceNotFoundException;

  /**
   * Casts a vote on the certainty of a region's transcription.
   * Requires the user to have at least 'WRITE' permissions. A user can only vote once per region.
   *
   * @param scrollId The identifier of the scroll containing the region.
   * @param regionId The unique identifier of the region to vote on.
   * @param vote The user's vote DTO.
   * @return The updated BoxRegion DTO, reflecting the new certainty score.
   * @throws com.mf.HerculaneumTranscriptor.exception.ResourceNotFoundException if the scroll or region does not exist.
   * @throws com.mf.HerculaneumTranscriptor.exception.ResourceAlreadyExistsException if the user has already voted on this region.
   *
   */
  @PreAuthorize("hasRole('WRITE') or hasRole('ROOT') or hasRole('ADMIN')")
  BoxRegion voteOnRegion(String scrollId, UUID regionId, Vote vote) throws ResourceNotFoundException, ResourceAlreadyExistsException;

}
