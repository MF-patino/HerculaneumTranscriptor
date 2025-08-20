package com.mf.HerculaneumTranscriptor.repository;

import com.mf.HerculaneumTranscriptor.domain.Scroll;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface ScrollRepository extends CrudRepository<Scroll, Long> {
  Optional<Scroll> findByScrollId(String scrollId);
}
