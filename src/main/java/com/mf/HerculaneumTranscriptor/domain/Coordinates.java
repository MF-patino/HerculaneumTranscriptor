package com.mf.HerculaneumTranscriptor.domain;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable // Marks this class as one that can be embedded in another entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Coordinates {
  private float x;
  private float y;
  private float width;
  private float height;
}
