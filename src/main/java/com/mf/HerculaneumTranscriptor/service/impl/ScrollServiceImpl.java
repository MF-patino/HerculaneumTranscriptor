package com.mf.HerculaneumTranscriptor.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.mf.HerculaneumTranscriptor.domain.mapper.ScrollMapper;
import com.mf.HerculaneumTranscriptor.exception.ResourceAlreadyExistsException;
import com.mf.HerculaneumTranscriptor.exception.ResourceNotFoundException;
import com.mf.HerculaneumTranscriptor.repository.ScrollRepository;
import com.mf.HerculaneumTranscriptor.service.ScrollService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import scroll.dto.NewScroll;
import scroll.dto.Scroll;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class ScrollServiceImpl implements ScrollService {
  private final ScrollRepository scrollRepository;
  private final ScrollMapper scrollMapper;
  private final Cloudinary cloudinary;

  @Value("${api.scrolls.storageDirectory}")
  private Path storageLocation;
  @Value("${api.scrolls.useCloud}")
  private Boolean useCloudStorage;

  /**
   * Validates if a given string is a valid URL.
   *
   * @param urlString The string to validate.
   * @return true if the string is a valid URL, false otherwise.
   */
  boolean isValidURL(String urlString) {
    if (urlString == null || urlString.isBlank()) {
      return false;
    }
    try {
      URI uri = new URI(urlString);

      // Check if the URI has a scheme and host.
      if (uri.getScheme() == null || uri.getHost() == null)
        return false;

      uri.toURL();

      return true;
    } catch (URISyntaxException | MalformedURLException | IllegalArgumentException e) {
      // If any part fails, it's not a valid URL.
      return false;
    }
  }

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

    String imgPath;
    if (useCloudStorage){
      Map uploadResult = cloudinary.uploader().upload(inkImage.getBytes(),
              ObjectUtils.asMap(
                      // public id is the scroll id
                      "public_id", metadata.getScrollId(),
                      "type", "private",
                      "folder", storageLocation.toString()
              ));

      // Get the secure URL of the uploaded image from the Cloudinary response
      imgPath = uploadResult.get("secure_url").toString();

    } else {
      if (!Files.exists(storageLocation))
        Files.createDirectories(storageLocation);

      String fileExtension = StringUtils.getFilenameExtension(inkImage.getOriginalFilename());
      imgPath = metadata.getScrollId() + "." + fileExtension;
      Path destinationFile = storageLocation.resolve(imgPath).normalize();

      // Use a try-with-resources block to ensure the input stream is closed automatically
      try (InputStream inputStream = inkImage.getInputStream()) {
        // Copy the file's input stream to the target location.
        // REPLACE_EXISTING ensures that if a file with the same name somehow exists, it's overwritten.
        Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
      }
    }

    newScroll.setImagePath(imgPath);

    // Important to return savedScroll as creation date is set automatically by the DB
    com.mf.HerculaneumTranscriptor.domain.Scroll savedScroll = scrollRepository.save(newScroll);
    return scrollMapper.scrollEntityToScrollDto(savedScroll);
  }

  @Override
  public void deleteScroll(String scrollId) throws ResourceNotFoundException, IOException {
    com.mf.HerculaneumTranscriptor.domain.Scroll scroll = scrollRepository.findByScrollId(scrollId)
            .orElseThrow(() -> new ResourceNotFoundException("Scroll not found"));

    scrollRepository.delete(scroll); // Deletes the metadata from the DB

    String imagePath = scroll.getImagePath();

    // If instead of a path we have a URL, delete image from cloud storage
    if (isValidURL(imagePath)){
      // We delete the image from Cloudinary using its public_id (its folder + scrollId)
      String publicId = storageLocation.toString() + "/" + scrollId;
      cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image", "invalidate", true, "type", "private"));
      return;
    }

    Path filePath = storageLocation.resolve(imagePath).normalize();

    // In the improbable case the image does not exist, do not try to delete it
    if (Files.exists(filePath))
      Files.delete(filePath);
  }

  @Override
  public Resource getScrollImage(String scrollId) throws ResourceNotFoundException, IOException {
    com.mf.HerculaneumTranscriptor.domain.Scroll scroll = scrollRepository.findByScrollId(scrollId)
            .orElseThrow(() -> new ResourceNotFoundException("Scroll not found"));

    String imagePath = scroll.getImagePath();

    // If instead of a path we have a URL, return signed URL to image from cloud storage
    if (isValidURL(imagePath)){
      try {
        // Define the options for the URL
        Map options = ObjectUtils.asMap(
                "resource_type", "image",
                "expires_at", (System.currentTimeMillis() / 1000L) + 5*60L // Expires in 5 minutes (Unix epoch time in seconds)
        );

        // Generate the signed URL
        String signedUrl = cloudinary.privateDownload(storageLocation.toString() + "/" + scrollId, "png", options);

        return new UrlResource(signedUrl);

      } catch (Exception e) {
        // This can happen if credentials are bad or there's a network issue with Cloudinary
        throw new RuntimeException("Could not generate secure image URL.", e);
      }
    }

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
