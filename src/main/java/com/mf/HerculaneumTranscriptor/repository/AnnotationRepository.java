package com.mf.HerculaneumTranscriptor.repository;

import com.mf.HerculaneumTranscriptor.domain.Annotation;
import org.springframework.data.repository.CrudRepository;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnnotationRepository extends CrudRepository<Annotation, Long> {
  Optional<Annotation> findByRegionId(UUID regionId);

  // The following methods look into the field scroll.scrollId
  List<Annotation> findByScrollScrollId(String scrollId);
  List<Annotation> findByScrollScrollIdAndUpdatedAtAfter(String scrollId, Date timestamp);
}
