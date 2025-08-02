package com.mf.HerculaneumTranscriptor.security;

import com.mf.HerculaneumTranscriptor.domain.User;
import com.mf.HerculaneumTranscriptor.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import user.dto.UserInfo;

@AllArgsConstructor
@Component("securityLogic")
public class SecurityLogic {
  private final UserRepository userRepository;

  /**
   * Checks if the current user is permitted to perform an operation on a target user.
   * Rules:
   * - A ROOT or ADMIN user can operate on anyone (except the ROOT user, e.g ROOT cannot delete itself).
   * - A regular user can operate on themselves.
   *
   * @param authentication The current user's authentication object.
   * @param targetUsername The username of the user to be acted upon.
   * @return true if the current user has authority over the target user, false otherwise.
   */
  public boolean hasAuthorityOver(Authentication authentication, String targetUsername) {
    if (authentication == null || !authentication.isAuthenticated()) {
      return false;
    }

    boolean isAdminRoot = authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().matches("ROLE_ROOT|ROLE_ADMIN"));

    // Admins and root user can operate on a user, but we must first check if the target is the root user.
    if (isAdminRoot) {
      User targetUser = userRepository.findByUsername(targetUsername).orElse(null);

      // If the target doesn't exist, let the service method handle the ResourceNotFoundException.
      // This will inform the frontend that the user doesn't exist.
      if (targetUser == null) {
        return true;
      }

      // Allow if the target is NOT root.
      return targetUser.getPermissions() != UserInfo.PermissionsEnum.ROOT;
    } else return authentication.getName().equals(targetUsername); // A user can operate on themselves.
  }
}
