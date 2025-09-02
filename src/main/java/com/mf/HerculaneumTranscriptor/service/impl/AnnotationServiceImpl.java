package com.mf.HerculaneumTranscriptor.service.impl;

import annotation.dto.BoxRegion;
import annotation.dto.NewBoxRegion;
import annotation.dto.RegionUpdateResponse;
import annotation.dto.Vote;
import com.mf.HerculaneumTranscriptor.domain.Annotation;
import com.mf.HerculaneumTranscriptor.domain.Scroll;
import com.mf.HerculaneumTranscriptor.domain.User;
import com.mf.HerculaneumTranscriptor.domain.mapper.AnnotationMapper;
import com.mf.HerculaneumTranscriptor.exception.ResourceAlreadyExistsException;
import com.mf.HerculaneumTranscriptor.exception.ResourceNotFoundException;
import com.mf.HerculaneumTranscriptor.repository.AnnotationRepository;
import com.mf.HerculaneumTranscriptor.repository.ScrollRepository;
import com.mf.HerculaneumTranscriptor.security.JwtUserDetails;
import com.mf.HerculaneumTranscriptor.service.AnnotationService;
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
public class AnnotationServiceImpl implements AnnotationService {
  private final AnnotationRepository annotationRepository;
  private final ScrollRepository scrollRepository;
  private final AnnotationMapper annotationMapper;

  @Override
  public RegionUpdateResponse getScrollRegions(String scrollId, Date since) throws ResourceNotFoundException {
    // Verify that the scroll exists.
    if (!scrollRepository.existsByScrollId(scrollId)) {
      throw new ResourceNotFoundException("Scroll not found with ID: " + scrollId);
    }

    // Decide which repository method to call based on the 'since' parameter.
    List<Annotation> annotations;
    if (since == null) {
      annotations = annotationRepository.findByScrollScrollId(scrollId);
    } else {
      annotations = annotationRepository.findByScrollScrollIdAndUpdatedAtAfter(scrollId, since);
    }

    List<BoxRegion> regionDtos = annotations.stream()
            .map(annotationMapper::annotationEntityToBoxRegionDto)
            .collect(Collectors.toList());

    // Build the final response object.
    RegionUpdateResponse response = new RegionUpdateResponse();
    response.setRegions(regionDtos);
    // Set the timestamp for the next client sync.
    response.setLastSyncTimestamp(Date.from(Instant.now()));

    return response;
  }

  @Override
  public BoxRegion createRegion(String scrollId, NewBoxRegion newRegion) throws ResourceNotFoundException {
    // Find the parent scroll. If it doesn't exist, this will throw a 404.
    Scroll parentScroll = scrollRepository.findByScrollId(scrollId)
            .orElseThrow(() -> new ResourceNotFoundException("Cannot create region: Scroll not found with ID: " + scrollId));

    // Find the author of the annotation from the security context.
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    User author;

    if (authentication.getPrincipal() instanceof JwtUserDetails)
      author = ((JwtUserDetails) authentication.getPrincipal()).getUser();
    else throw new BadCredentialsException("User not authenticated through a JWT");

    // Map the incoming DTO to a new entity.
    Annotation newAnnotation = annotationMapper.newBoxRegionDtoToAnnotationEntity(newRegion);

    // Complete the entity with server-side data.
    newAnnotation.setRegionId(UUID.randomUUID()); // Generate a new, unique public ID
    newAnnotation.setAuthor(author);
    newAnnotation.setScroll(parentScroll);

    // Save the annotation to the database.
    Annotation savedAnnotation = annotationRepository.save(newAnnotation);

    // Map the saved entity, which contains the auto-generated ID and timestamps.
    return annotationMapper.annotationEntityToBoxRegionDto(savedAnnotation);
  }

  @Override
  public BoxRegion updateRegion(String scrollId, UUID regionId, NewBoxRegion updatedRegion) throws ResourceNotFoundException {
    return null;
  }

  @Override
  public void deleteRegion(String scrollId, UUID regionId) throws ResourceNotFoundException {

  }

  @Override
  public BoxRegion voteOnRegion(String scrollId, UUID regionId, Vote vote) throws ResourceNotFoundException, ResourceAlreadyExistsException {
    return null;
  }
}
