package com.mf.HerculaneumTranscriptor.service.impl;

import annotation.dto.BoxRegion;
import annotation.dto.NewBoxRegion;
import annotation.dto.RegionUpdateResponse;
import annotation.dto.Vote;
import com.mf.HerculaneumTranscriptor.domain.mapper.AnnotationMapper;
import com.mf.HerculaneumTranscriptor.exception.ResourceAlreadyExistsException;
import com.mf.HerculaneumTranscriptor.exception.ResourceNotFoundException;
import com.mf.HerculaneumTranscriptor.repository.AnnotationRepository;
import com.mf.HerculaneumTranscriptor.service.AnnotationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnnotationServiceImpl implements AnnotationService {
  private final AnnotationRepository annotationRepository;
  private final AnnotationMapper annotationMapper;

  @Override
  public RegionUpdateResponse getScrollRegions(String scrollId, Date since) throws ResourceNotFoundException {
    return null;
  }

  @Override
  public BoxRegion createRegion(String scrollId, NewBoxRegion newRegion) throws ResourceNotFoundException {
    return null;
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
