package com.mf.HerculaneumTranscriptor;

import com.mf.HerculaneumTranscriptor.domain.User;
import com.mf.HerculaneumTranscriptor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import user.dto.UserInfo;

import java.util.Optional;

@Profile("!test") // This component will NOT be loaded when the "test" profile is active
@RequiredArgsConstructor
@Component
public class EventListeners {
  @Value( "${security.rootProfile.username}" )
  private String rootUsername;
  @Value( "${security.rootProfile.password}" )
  private String rootPassword;

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  // Event listener to create a root profile on startup if it doesn't exist
  // In case the root password or username is changed in the configuration file, the root profile will be updated
  @EventListener(ApplicationReadyEvent.class)
  public void createRootProfileOnStartUp(ApplicationReadyEvent event) {
    Optional<User> possibleRoot = userRepository.findByPermissions(UserInfo.PermissionsEnum.ROOT);
    User root;
    String rootPasswordHash = passwordEncoder.encode(rootPassword);

    if (possibleRoot.isEmpty()) {
      System.out.println("Root profile not found, generating one.");

      root = new User();
      root.setUsername(rootUsername);
      root.setFirstName("Root");
      root.setPasswordHash(rootPasswordHash);
      root.setPermissions(UserInfo.PermissionsEnum.ROOT);

      userRepository.save(root);
    } else {
      root = possibleRoot.get();

      if (!passwordEncoder.matches(rootPassword, root.getPasswordHash()) ||
              !root.getUsername().equals(rootUsername)) {
        System.out.println("Root profile information not aligned with configuration, updating.");

        root.setPasswordHash(rootPasswordHash);
        root.setUsername(rootUsername);
        userRepository.save(root);
      }
    }
  }
}
