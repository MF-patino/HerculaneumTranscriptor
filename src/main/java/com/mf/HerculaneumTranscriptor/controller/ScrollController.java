package com.mf.HerculaneumTranscriptor.controller;

import lombok.AllArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import scroll.api.ScrollsApi;
import scroll.dto.NewScroll;
import scroll.dto.Scroll;

import java.util.List;

@RestController
@AllArgsConstructor
public class ScrollController implements ScrollsApi {
  @Override
  public ResponseEntity<Scroll> createScroll(NewScroll metadata, MultipartFile inkImage) {
    return null;
  }

  @Override
  public ResponseEntity<Void> deleteScroll(String scrollId) {
    return null;
  }

  @Override
  public ResponseEntity<List<Scroll>> getAllScrolls() {
    return null;
  }

  @Override
  public ResponseEntity<Resource> getScrollImage(String scrollId) {
    return null;
  }
}
