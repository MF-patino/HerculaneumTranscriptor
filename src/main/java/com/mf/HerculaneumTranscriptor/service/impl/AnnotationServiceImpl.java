package com.mf.HerculaneumTranscriptor.service.impl;

import annotation.dto.BoxRegion;
import annotation.dto.NewBoxRegion;
import annotation.dto.RegionUpdateResponse;
import annotation.dto.Vote;
import com.mf.HerculaneumTranscriptor.domain.Annotation;
import com.mf.HerculaneumTranscriptor.domain.Scroll;
import com.mf.HerculaneumTranscriptor.domain.User;
import com.mf.HerculaneumTranscriptor.domain.mapper.AnnotationMapper;
import com.mf.HerculaneumTranscriptor.exception.ResourceNotFoundException;
import com.mf.HerculaneumTranscriptor.repository.AnnotationRepository;
import com.mf.HerculaneumTranscriptor.repository.ScrollRepository;
import com.mf.HerculaneumTranscriptor.repository.VoteRepository;
import com.mf.HerculaneumTranscriptor.security.JwtUserDetails;
import com.mf.HerculaneumTranscriptor.service.AnnotationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
  private final VoteRepository voteRepository;

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
            .orElseThrow(() -> new ResourceNotFoundException("Cannot create region: scroll not found"));

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
    // Find the annotation by its unique internal ID.
    Annotation annotation = annotationRepository.findByRegionId(regionId)
            .orElseThrow(() -> new ResourceNotFoundException("Cannot update region: region not found"));

    // Make sure the annotation belongs to the parent scroll.
    if (!annotation.getScroll().getScrollId().equals(scrollId))
      throw new ResourceNotFoundException("Cannot update region: it does not belong to specified scroll");

    // Update fields with DTO information
    annotation.setCoordinates(annotationMapper.coordinatesDtoToEntityCoordinates(updatedRegion.getCoordinates()));
    annotation.setTranscription(updatedRegion.getTranscription());

    // Save and return updated annotation
    Annotation savedAnnotation = annotationRepository.save(annotation);
    return annotationMapper.annotationEntityToBoxRegionDto(savedAnnotation);
  }

  @Override
  public void deleteRegion(String scrollId, UUID regionId) throws ResourceNotFoundException {
    // Find the annotation by its unique internal ID.
    Annotation annotation = annotationRepository.findByRegionId(regionId)
            .orElseThrow(() -> new ResourceNotFoundException("Cannot delete region: region not found"));

    // Make sure the annotation belongs to the parent scroll.
    if (!annotation.getScroll().getScrollId().equals(scrollId))
      throw new ResourceNotFoundException("Cannot delete region: it does not belong to specified scroll");

    // Delete the annotation from the database.
    annotationRepository.delete(annotation);
  }

  @Override
  @Transactional // This is a critical multi-step write operation
  public BoxRegion voteOnRegion(String scrollId, UUID regionId, Vote voteDto) throws ResourceNotFoundException {
    // Find the annotation by its unique internal ID.
    Annotation annotation = annotationRepository.findByRegionId(regionId)
            .orElseThrow(() -> new ResourceNotFoundException("Annotation not found"));

    // Make sure the annotation belongs to the parent scroll.
    if (!annotation.getScroll().getScrollId().equals(scrollId))
      throw new ResourceNotFoundException("Annotation does not belong to specified scroll");

    // Find the caster of the vote from the security context.
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    User caster;

    if (authentication.getPrincipal() instanceof JwtUserDetails)
      caster = ((JwtUserDetails) authentication.getPrincipal()).getUser();
    else throw new BadCredentialsException("User not authenticated through a JWT");

    com.mf.HerculaneumTranscriptor.domain.Vote vote  = voteRepository.findByUserAndAnnotation(caster, annotation).orElse(null);

    if (vote == null)
      vote = new com.mf.HerculaneumTranscriptor.domain.Vote(caster, annotation, voteDto.getVote());
    else
      vote.setVoteValue(voteDto.getVote());

    // Create or update the cast vote
    voteRepository.save(vote);

    // Calculate and update annotation certainty
    Float avgCertainty = voteRepository.calculateAverageVote(annotation.getId());
    annotation.setCertaintyScore(avgCertainty != null ? avgCertainty : -1.0f);
    Annotation updatedAnnotation = annotationRepository.save(annotation);

    return annotationMapper.annotationEntityToBoxRegionDto(updatedAnnotation);
  }
}
