package com.mf.HerculaneumTranscriptor.service.impl;

import com.mf.HerculaneumTranscriptor.domain.mapper.ScrollMapper;
import com.mf.HerculaneumTranscriptor.exception.ResourceAlreadyExistsException;
import com.mf.HerculaneumTranscriptor.exception.ResourceNotFoundException;
import com.mf.HerculaneumTranscriptor.repository.ScrollRepository;
import com.mf.HerculaneumTranscriptor.service.ScrollService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import scroll.dto.NewScroll;
import scroll.dto.Scroll;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class ScrollServiceImpl implements ScrollService {
  private final ScrollRepository scrollRepository;
  private final ScrollMapper scrollMapper;

  @Value("${api.scrolls.storageDirectory}")
  private Path storageLocation;

  @Override
  public List<Scroll> getAllScrolls() {
    return StreamSupport.stream(scrollRepository.findAll().spliterator(), false).map(scrollMapper::scrollEntityToScrollDto).toList();
  }

  @Override
  public Scroll createScroll(NewScroll metadata, MultipartFile inkImage) throws ResourceAlreadyExistsException, IOException {
    if (scrollRepository.existsByScrollId(metadata.getScrollId())) {
      throw new ResourceAlreadyExistsException("Scroll with ID '" + metadata.getScrollId() + "' already exists.");
    }

    com.mf.HerculaneumTranscriptor.domain.Scroll newScroll = scrollMapper.newScrollDtoToScrollEntity(metadata);

    if (!Files.exists(storageLocation))
      Files.createDirectories(storageLocation);

    String fileExtension = StringUtils.getFilenameExtension(inkImage.getOriginalFilename());
    String filename = metadata.getScrollId() + "." + fileExtension;
    Path destinationFile = storageLocation.resolve(filename).normalize();

    // Use a try-with-resources block to ensure the input stream is closed automatically
    try (InputStream inputStream = inkImage.getInputStream()) {
      // Copy the file's input stream to the target location.
      // REPLACE_EXISTING ensures that if a file with the same name somehow exists, it's overwritten.
      Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
    }

    newScroll.setImagePath(filename);

    // Important to return savedScroll as creation date is set automatically by the DB
    com.mf.HerculaneumTranscriptor.domain.Scroll savedScroll = scrollRepository.save(newScroll);
    return scrollMapper.scrollEntityToScrollDto(savedScroll);
  }

  @Override
  public void deleteScroll(String scrollId) throws ResourceNotFoundException, IOException {
    com.mf.HerculaneumTranscriptor.domain.Scroll scroll = scrollRepository.findByScrollId(scrollId)
            .orElseThrow(() -> new ResourceNotFoundException("Scroll not found"));

    scrollRepository.delete(scroll); // Deletes the metadata from the DB

    Path filePath = storageLocation.resolve(scroll.getImagePath()).normalize();

    // In the improbable case the image does not exist, do not try to delete it
    if (Files.exists(filePath))
      Files.delete(filePath);
  }

  @Override
  public Resource getScrollImage(String scrollId) throws ResourceNotFoundException, IOException {
    com.mf.HerculaneumTranscriptor.domain.Scroll scroll = scrollRepository.findByScrollId(scrollId)
            .orElseThrow(() -> new ResourceNotFoundException("Scroll not found"));

    Path filePath = storageLocation.resolve(scroll.getImagePath()).normalize();
    InputStream in = Files.newInputStream(filePath);
    return new InputStreamResource(in);
  }

  @Override
  public Scroll updateScroll(String scrollId, NewScroll metadata) throws ResourceAlreadyExistsException, ResourceNotFoundException {
    com.mf.HerculaneumTranscriptor.domain.Scroll scroll = scrollRepository.findByScrollId(scrollId)
            .orElseThrow(() -> new ResourceNotFoundException("Scroll not found"));

    if (!scrollId.equals(metadata.getScrollId()) && scrollRepository.existsByScrollId(metadata.getScrollId())) {
      throw new ResourceAlreadyExistsException("Scroll with ID '" + metadata.getScrollId() + "' already exists.");
    }

    scroll.setDescription(metadata.getDescription());
    scroll.setDisplayName(metadata.getDisplayName());
    scroll.setScrollId(metadata.getScrollId());
    scroll.setThumbnailUrl(scrollMapper.uriToString(metadata.getThumbnailUrl()));

    // Update scroll entry
    com.mf.HerculaneumTranscriptor.domain.Scroll updatedScroll = scrollRepository.save(scroll);
    return scrollMapper.scrollEntityToScrollDto(updatedScroll);
  }
}
