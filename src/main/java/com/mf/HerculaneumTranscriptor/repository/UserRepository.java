package com.mf.HerculaneumTranscriptor.repository;

import com.mf.HerculaneumTranscriptor.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import user.dto.UserInfo;

import java.util.Optional;

public interface UserRepository extends CrudRepository<User, Long> {
  Boolean existsByUsername(String username);
  Optional<User> findByUsername(String username);
  Optional<User> findByPermissions(UserInfo.PermissionsEnum permissions);

  /**
   * Finds a set of users at an index and returns them in a paginated format.
   * @param pageable contains page number, page size, and sorting info.
   * @return a Page of Users, which includes the list for the current page
   *         and total number of pages/elements.
   */
  Page<User> findAll(Pageable pageable);
}
