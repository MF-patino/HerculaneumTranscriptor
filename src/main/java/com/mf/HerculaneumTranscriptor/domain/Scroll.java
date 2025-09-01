package com.mf.HerculaneumTranscriptor.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity @Table(name="SCROLLS")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class Scroll {
  @Id
  @GeneratedValue(strategy= GenerationType.AUTO)
  private Long id;

  private String scrollId;
  private String displayName;
  private String description;
  private String imagePath;
  private String thumbnailUrl;

  @CreationTimestamp
  private Instant createdAt;
}
