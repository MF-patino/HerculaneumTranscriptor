package com.mf.HerculaneumTranscriptor.domain.mapper;

import com.mf.HerculaneumTranscriptor.domain.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import user.dto.UserInfo;
import user.dto.UserRegisterInfo;

@Mapper(componentModel = "spring")
public interface UserMapper {
  // Maps the registration DTO to the internal User entity
  @Mapping(target = "passwordHash", ignore = true) // Don't map password directly, it will be stored as a hash
  @Mapping(source = "basicInfo.username", target = "username")
  @Mapping(source = "basicInfo.firstName", target = "firstName")
  @Mapping(source = "basicInfo.lastName", target = "lastName")
  @Mapping(source = "basicInfo.contact", target = "contact")
  User userRegisterInfoToUser(UserRegisterInfo dto);

  // Maps User entity to the public-facing UserInfo DTO
  @Mapping(source = "username", target = "basicInfo.username")
  @Mapping(source = "firstName", target = "basicInfo.firstName")
  @Mapping(source = "lastName", target = "basicInfo.lastName")
  @Mapping(source = "contact", target = "basicInfo.contact")
  @Mapping(source = "permissions", target = "permissions")
  UserInfo userToUserInfo(User entity);
}