package com.mf.HerculaneumTranscriptor.domain;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Table(name = "VOTES")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@IdClass(Vote.VoteId.class) // Specifies the composite primary key class
public class Vote {
  // COMPOSITE PRIMARY KEY
  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "annotation_id")
  private Annotation annotation;

  private int voteValue; // The 0-5 score

  // A static nested class to represent the composite key
  public static class VoteId implements Serializable {
    private Long user;
    private Long annotation;
  }
}
