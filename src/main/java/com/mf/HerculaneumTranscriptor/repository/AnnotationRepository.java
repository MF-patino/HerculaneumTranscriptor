package com.mf.HerculaneumTranscriptor.repository;

import com.mf.HerculaneumTranscriptor.domain.Annotation;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnnotationRepository extends CrudRepository<Annotation, Long> {
  Optional<Annotation> findByRegionId(UUID regionId);

  // The following methods look into the field scroll.scrollId
  List<Annotation> findByScrollScrollId(String scrollId);
  List<Annotation> findByScrollScrollIdAndUpdatedAtAfter(String scrollId, Instant timestamp);

  boolean existsByRegionId(UUID regionId);

  // When a scroll is deleted, this will delete all annotations associated with it
  @Transactional
  @Modifying
  long deleteByScrollScrollId(String scrollId);
}
