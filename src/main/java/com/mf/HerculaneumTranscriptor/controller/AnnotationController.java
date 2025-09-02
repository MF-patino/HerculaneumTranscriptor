package com.mf.HerculaneumTranscriptor.controller;

import annotation.api.AnnotationsApi;
import annotation.dto.BoxRegion;
import annotation.dto.NewBoxRegion;
import annotation.dto.RegionUpdateResponse;
import annotation.dto.Vote;
import com.mf.HerculaneumTranscriptor.service.AnnotationService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.UUID;

@RestController
@AllArgsConstructor
public class AnnotationController implements AnnotationsApi {
  private final AnnotationService annotationService;

  @Override
  public ResponseEntity<BoxRegion> createRegion(String scrollId, NewBoxRegion newBoxRegion) {
    BoxRegion region = annotationService.createRegion(scrollId, newBoxRegion);
    return ResponseEntity.ok(region);
  }

  @Override
  public ResponseEntity<Void> deleteRegion(String scrollId, UUID regionId) {
    annotationService.deleteRegion(scrollId, regionId);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<RegionUpdateResponse> getScrollRegions(String scrollId, Date since) {
    RegionUpdateResponse response = annotationService.getScrollRegions(scrollId, since);
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<BoxRegion> updateRegion(String scrollId, UUID regionId, NewBoxRegion newBoxRegion) {
    BoxRegion region = annotationService.updateRegion(scrollId, regionId, newBoxRegion);
    return ResponseEntity.ok(region);
  }

  @Override
  public ResponseEntity<BoxRegion> voteOnRegion(String scrollId, UUID regionId, Vote vote) {
    BoxRegion region = annotationService.voteOnRegion(scrollId, regionId, vote);
    return ResponseEntity.ok(region);
  }
}
