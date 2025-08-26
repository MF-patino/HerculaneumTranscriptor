package com.mf.HerculaneumTranscriptor.controller;

import com.mf.HerculaneumTranscriptor.service.ScrollService;
import lombok.AllArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import scroll.api.ScrollsApi;
import scroll.dto.NewScroll;
import scroll.dto.Scroll;

import java.io.IOException;
import java.util.List;

@RestController
@AllArgsConstructor
public class ScrollController implements ScrollsApi {
  private final ScrollService scrollService;

  @Override
  public ResponseEntity<Scroll> createScroll(NewScroll metadata, MultipartFile inkImage) throws IOException {
    Scroll scroll = scrollService.createScroll(metadata, inkImage);
    return ResponseEntity.ok(scroll);
  }

  @Override
  public ResponseEntity<Void> deleteScroll(String scrollId) {
    return null;
  }

  @Override
  public ResponseEntity<List<Scroll>> getAllScrolls() {
    List<Scroll> scrolls = scrollService.getAllScrolls();
    return ResponseEntity.ok(scrolls);
  }

  @Override
  public ResponseEntity<Resource> getScrollImage(String scrollId) {
    return null;
  }
}
