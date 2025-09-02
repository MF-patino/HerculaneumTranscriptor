package com.mf.HerculaneumTranscriptor.domain;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "ANNOTATIONS")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class Annotation {
  @Id
  @GeneratedValue(strategy= GenerationType.AUTO)
  private Long id;
  private UUID regionId;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "author_user_id", nullable = false)
  private User author;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "scroll_id", nullable = false)
  private Scroll scroll;

  private String transcription;

  @Embedded
  private Coordinates coordinates;

  @Column(nullable = false, columnDefinition = "float default 0")
  private float certaintyScore = 0.0f;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(nullable = false)
  private Instant updatedAt;
}
