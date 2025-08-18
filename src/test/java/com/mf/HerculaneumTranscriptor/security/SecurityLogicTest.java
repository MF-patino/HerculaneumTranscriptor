package com.mf.HerculaneumTranscriptor.security;

import com.mf.HerculaneumTranscriptor.domain.User;
import com.mf.HerculaneumTranscriptor.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import user.dto.UserInfo;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityLogicTest {

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private SecurityLogic securityLogic;

  private User rootUser;
  private User regularUser;

  // Helper method to create a mock Authentication object
  private Authentication mockAuthentication(String username, String role) {
    Authentication auth = mock(Authentication.class);

    // this stubbing is made optional for minimal stubbing
    if (username != null)
      when(auth.getName()).thenReturn(username);

    Collection authorities = List.of(new SimpleGrantedAuthority(role));
    when(auth.getAuthorities()).thenReturn(authorities);

    when(auth.isAuthenticated()).thenReturn(true);
    return auth;
  }

  @BeforeEach
  void setUp() {
    rootUser = new User();
    rootUser.setUsername("root");
    rootUser.setPermissions(UserInfo.PermissionsEnum.ROOT);

    regularUser = new User();
    regularUser.setUsername("user");
    regularUser.setPermissions(UserInfo.PermissionsEnum.READ);
  }

  @Test
  void hasAuthorityOver_shouldReturnFalse_whenAdminTriesToActOnRoot() {
    // Arrange
    Authentication adminAuth = mockAuthentication(null, "ROLE_ADMIN");
    when(userRepository.findByUsername("root")).thenReturn(Optional.of(rootUser));

    // Act
    boolean result = securityLogic.hasAuthorityOver(adminAuth, "root");

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  void hasAuthorityOver_shouldReturnTrue_whenAdminTriesToActOnRegularUser() {
    // Arrange
    Authentication adminAuth = mockAuthentication(null, "ROLE_ADMIN");
    when(userRepository.findByUsername("user")).thenReturn(Optional.of(regularUser));

    // Act
    boolean result = securityLogic.hasAuthorityOver(adminAuth, "user");

    // Assert
    assertThat(result).isTrue();
  }

  @Test
  void hasAuthorityOver_shouldReturnTrue_whenUserActsOnThemselves() {
    // Arrange
    Authentication userAuth = mockAuthentication("user", "ROLE_READ");
    when(userRepository.findByUsername("user")).thenReturn(Optional.of(regularUser));

    // Act
    boolean result = securityLogic.hasAuthorityOver(userAuth, "user");

    // Assert
    assertThat(result).isTrue();
  }

  @Test
  void hasAuthorityOver_shouldReturnFalse_whenUserActsOnNonExistingUser() {
    // Arrange
    Authentication userAuth = mockAuthentication(null, "ROLE_READ");
    when(userRepository.findByUsername("otherUser")).thenReturn(Optional.empty());

    // Act
    boolean result = securityLogic.hasAuthorityOver(userAuth, "otherUser");

    // Assert
    assertThat(result).isTrue();
  }

  @Test
  void hasAuthorityOver_shouldReturnFalse_whenUserActsOnExistingUser() {
    // Arrange
    Authentication userAuth = mockAuthentication("otherUser", "ROLE_READ");
    when(userRepository.findByUsername("user")).thenReturn(Optional.of(regularUser));

    // Act
    boolean result = securityLogic.hasAuthorityOver(userAuth, "user");

    // Assert
    assertThat(result).isFalse();
  }
}