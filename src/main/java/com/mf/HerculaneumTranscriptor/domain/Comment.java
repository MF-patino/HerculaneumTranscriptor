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

@Entity @Table(name = "COMMENTS")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class Comment {
  @Id
  @GeneratedValue(strategy= GenerationType.AUTO)
  private Long id;

  private UUID commentId;

  private String text;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "author_user_id", nullable = false, updatable = false)
  private User author;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "annotation_id_fk", nullable = false, updatable = false)
  private Annotation annotation;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(nullable = false)
  private Instant updatedAt;
}
