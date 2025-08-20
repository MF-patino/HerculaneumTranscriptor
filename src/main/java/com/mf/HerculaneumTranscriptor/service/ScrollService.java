package com.mf.HerculaneumTranscriptor.service;

import com.mf.HerculaneumTranscriptor.exception.ResourceAlreadyExistsException;
import com.mf.HerculaneumTranscriptor.exception.ResourceNotFoundException;
import org.springframework.core.io.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;
import scroll.dto.NewScroll;
import scroll.dto.Scroll;

import java.io.IOException;
import java.util.List;

/**
 * Service layer defining business operations related to Scrolls.
 * This interface is independent of the web/controller layer.
 */
public interface ScrollService {

  /**
   * Retrieves a list of all scrolls available in the system.
   * This is a public operation for any authenticated user.
   *
   * @return A list of Scroll DTOs.
   */
  List<Scroll> getAllScrolls();

  /**
   * Creates a new scroll and stores its associated ink prediction image.
   * This operation is restricted to ROOT or ADMIN users.
   *
   * @param metadata The DTO containing the new scroll's details.
   * @param inkImage The ink prediction image file in PNG format.
   * @return The newly created Scroll DTO.
   * @throws com.mf.HerculaneumTranscriptor.exception.ResourceAlreadyExistsException if a scroll with the same ID already exists.
   * @throws java.io.IOException if there is an error saving the image file.
   */
  @PreAuthorize("hasRole('ROOT') or hasRole('ADMIN')")
  Scroll createScroll(NewScroll metadata, MultipartFile inkImage) throws ResourceAlreadyExistsException, IOException;

  /**
   * Deletes a scroll and its associated image file from the system.
   * This is a destructive operation restricted to ROOT or ADMIN users.
   *
   * @param scrollId The unique identifier of the scroll to delete.
   * @throws com.mf.HerculaneumTranscriptor.exception.ResourceNotFoundException if the scroll does not exist.
   * @throws java.io.IOException if there is an error deleting the image file.
   */
  @PreAuthorize("hasRole('ROOT') or hasRole('ADMIN')")
  void deleteScroll(String scrollId) throws ResourceNotFoundException, IOException;

  /**
   * Retrieves the ink prediction image for a specific scroll as a loadable resource.
   * Any authenticated user can download the image.
   *
   * @param scrollId The unique identifier of the scroll.
   * @return A Spring Resource object pointing to the image file.
   * @throws com.mf.HerculaneumTranscriptor.exception.ResourceNotFoundException if the scroll or its image does not exist.
   */
  Resource getScrollImage(String scrollId) throws ResourceNotFoundException;

}