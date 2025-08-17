package com.mf.HerculaneumTranscriptor.repository;

import com.mf.HerculaneumTranscriptor.domain.User;
import org.springframework.data.repository.CrudRepository;
import user.dto.UserInfo;

import java.util.Optional;

public interface UserRepository extends CrudRepository<User, Long> {
  Boolean existsByUsername(String username);
  Optional<User> findByUsername(String username);
  Optional<User> findByPermissions(UserInfo.PermissionsEnum permissions);
}
