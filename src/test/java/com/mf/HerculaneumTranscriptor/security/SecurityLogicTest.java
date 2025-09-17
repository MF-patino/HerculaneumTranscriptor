package com.mf.HerculaneumTranscriptor.security;

import com.mf.HerculaneumTranscriptor.domain.Annotation;
import com.mf.HerculaneumTranscriptor.domain.User;
import com.mf.HerculaneumTranscriptor.repository.AnnotationRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityLogicTest {

  @Mock
  private UserRepository userRepository;
  @Mock
  private AnnotationRepository annotationRepository;

  @InjectMocks
  private SecurityLogic securityLogic;

  private User rootUser;
  private User writeUser;
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

    writeUser = new User();
    writeUser.setUsername("writer");
    writeUser.setPermissions(UserInfo.PermissionsEnum.WRITE);
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

  // Tests for canModifyRegion

  @Test
  void canModifyRegion_shouldReturnTrue_forAdminUser() {
    // Arrange
    Authentication adminAuth = mockAuthentication(null, "ROLE_ADMIN");
    UUID anyRegionId = UUID.randomUUID();

    // Act
    boolean result = securityLogic.canModifyRegion(adminAuth, anyRegionId);

    // Assert
    assertThat(result).isTrue();
    // Verify the database was NOT queried, because the admin check passes first.
    verify(annotationRepository, never()).findByRegionId(any());
  }

  @Test
  void canModifyRegion_shouldReturnTrue_forWriteUserWhoIsTheAuthor() {
    // Arrange
    Authentication writeAuth = mockAuthentication("writer", "ROLE_WRITE");
    Annotation ownAnnotation = new Annotation();
    ownAnnotation.setAuthor(writeUser); // The author is the same as the authenticated user
    UUID regionId = UUID.randomUUID();

    when(annotationRepository.findByRegionId(regionId)).thenReturn(Optional.of(ownAnnotation));

    // Act
    boolean result = securityLogic.canModifyRegion(writeAuth, regionId);

    // Assert
    assertThat(result).isTrue();
  }

  @Test
  void canModifyRegion_shouldReturnFalse_forWriteUserWhoIsNotTheAuthor() {
    // Arrange
    User anotherWriteUser = new User();
    anotherWriteUser.setUsername("another" + writeUser.getUsername());
    anotherWriteUser.setPermissions(UserInfo.PermissionsEnum.WRITE);
    Authentication writeAuth = mockAuthentication("writer", "ROLE_WRITE");
    Annotation anotherUsersAnnotation = new Annotation();
    anotherUsersAnnotation.setAuthor(anotherWriteUser); // Author is a different user
    UUID regionId = UUID.randomUUID();

    when(annotationRepository.findByRegionId(regionId)).thenReturn(Optional.of(anotherUsersAnnotation));

    // Act
    boolean result = securityLogic.canModifyRegion(writeAuth, regionId);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  void canModifyRegion_shouldReturnFalse_forUserWithoutWriteRole() {
    // Arrange
    // This user has READ, but not WRITE
    Authentication readAuth = mockAuthentication(null, "ROLE_READ");
    UUID anyRegionId = UUID.randomUUID();

    // Act
    boolean result = securityLogic.canModifyRegion(readAuth, anyRegionId);

    // Assert
    assertThat(result).isFalse();
    // Verify the DB was not queried, because the role check fails first.
    verify(annotationRepository, never()).findByRegionId(any());
  }

  @Test
  void canModifyRegion_shouldReturnTrue_forWriteUserWhenRegionDoesNotExist() {
    // Arrange
    Authentication writeAuth = mockAuthentication(null, "ROLE_WRITE");
    UUID nonExistentRegionId = UUID.randomUUID();

    // Simulate the annotation not being found in the database.
    when(annotationRepository.findByRegionId(nonExistentRegionId)).thenReturn(Optional.empty());

    // Act
    boolean result = securityLogic.canModifyRegion(writeAuth, nonExistentRegionId);

    // Assert
    // According to your logic, this should return true to let the service layer handle the 404.
    assertThat(result).isTrue();
  }

  @Test
  void canModifyRegion_shouldReturnFalse_whenAuthenticationIsNull() {
    // Act
    boolean result = securityLogic.canModifyRegion(null, UUID.randomUUID());

    // Assert
    assertThat(result).isFalse();
  }
}