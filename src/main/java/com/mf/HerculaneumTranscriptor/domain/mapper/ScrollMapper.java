package com.mf.HerculaneumTranscriptor.domain.mapper;

import com.mf.HerculaneumTranscriptor.domain.Scroll;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import scroll.dto.NewScroll;

@Mapper(componentModel = "spring")
public interface ScrollMapper {
  @Mapping(target = "imagePath", ignore = true) // This will be set manually in the service.
  @Mapping(target = "createdAt", ignore = true) // The database generates the timestamp.
  Scroll newScrollDtoToScrollEntity(NewScroll newScroll);

  scroll.dto.Scroll scrollEntityToScrollDto(Scroll scroll);
}
