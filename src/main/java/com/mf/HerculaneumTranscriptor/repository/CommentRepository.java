package com.mf.HerculaneumTranscriptor.repository;

import com.mf.HerculaneumTranscriptor.domain.Comment;
import org.springframework.data.repository.CrudRepository;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommentRepository extends CrudRepository<Comment, Long> {
  Optional<Comment> findByCommentId(UUID commentId);

  // The following methods look into the field annotation.regionId
  List<Comment> findByAnnotationRegionId(UUID regionId);
  List<Comment> findByAnnotationRegionIdAndUpdatedAtAfter(UUID regionId, Date timestamp);
}
