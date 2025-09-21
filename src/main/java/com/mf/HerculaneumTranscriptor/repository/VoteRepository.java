package com.mf.HerculaneumTranscriptor.repository;

import com.mf.HerculaneumTranscriptor.domain.Annotation;
import com.mf.HerculaneumTranscriptor.domain.User;
import com.mf.HerculaneumTranscriptor.domain.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface VoteRepository extends JpaRepository<Vote, Vote.VoteId> {
  Optional<Vote> findByUserAndAnnotation(User user, Annotation annotation);

  // JPQL query to calculate the average vote value for a specific annotation.
  // More efficient than fetching vote entities and averaging in Java.
  @Query("SELECT AVG(v.voteValue) FROM Vote v WHERE v.annotation.id = :annotationId")
  Float calculateAverageVote(Long annotationId);
}
