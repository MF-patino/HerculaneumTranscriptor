package com.mf.HerculaneumTranscriptor.domain.mapper;

import com.mf.HerculaneumTranscriptor.domain.Scroll;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import scroll.dto.NewScroll;

import java.net.URI;
import java.net.URISyntaxException;

@Mapper(componentModel = "spring")
public interface ScrollMapper {
  @Mapping(target = "imagePath", ignore = true) // This will be set manually in the service.
  @Mapping(target = "createdAt", ignore = true) // The database generates the timestamp.
  Scroll newScrollDtoToScrollEntity(NewScroll newScroll);

  scroll.dto.Scroll scrollEntityToScrollDto(Scroll scroll);

  default String uriToString(URI uri) {
    return (uri == null) ? null : uri.toString();
  }

  default URI stringToUri(String uriString) {
    if (uriString == null || uriString.isBlank()) {
      return null;
    }
    try {
      return new URI(uriString);
    } catch (URISyntaxException e) {
      System.err.println("Failed to parse URI string from database: " + uriString);
      return null;
    }
  }
}
