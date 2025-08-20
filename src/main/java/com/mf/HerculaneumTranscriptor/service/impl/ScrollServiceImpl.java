package com.mf.HerculaneumTranscriptor.service.impl;

import com.mf.HerculaneumTranscriptor.domain.mapper.ScrollMapper;
import com.mf.HerculaneumTranscriptor.exception.ResourceAlreadyExistsException;
import com.mf.HerculaneumTranscriptor.exception.ResourceNotFoundException;
import com.mf.HerculaneumTranscriptor.repository.ScrollRepository;
import com.mf.HerculaneumTranscriptor.service.ScrollService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import scroll.dto.NewScroll;
import scroll.dto.Scroll;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScrollServiceImpl implements ScrollService {
  private final ScrollRepository scrollRepository;
  private final ScrollMapper scrollMapper;

  @Override
  public List<Scroll> getAllScrolls() {
    return List.of();
  }

  @Override
  public Scroll createScroll(NewScroll metadata, MultipartFile inkImage) throws ResourceAlreadyExistsException, IOException {
    return null;
  }

  @Override
  public void deleteScroll(String scrollId) throws ResourceNotFoundException, IOException {

  }

  @Override
  public Resource getScrollImage(String scrollId) throws ResourceNotFoundException {
    return null;
  }
}
