package com.mf.HerculaneumTranscriptor.service;

import com.mf.HerculaneumTranscriptor.dto.AuthenticationResponse;
import com.mf.HerculaneumTranscriptor.exception.ResourceNotFoundException;
import com.mf.HerculaneumTranscriptor.exception.UserAlreadyExistsException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.AuthenticationException;
import user.dto.ChangePermissions;
import user.dto.UserInfo;
import user.dto.UserLoginInfo;
import user.dto.UserRegisterInfo;

/**
 * Service layer defining business operations related to Users.
 * This interface is independent of the web/controller layer.
 */
public interface UserService {

  /**
   * Registers a new user in the system.
   *
   * @param registrationInfo DTO containing new user details.
   * @return An AuthenticationResponse containing the JWT andUserInfo of the newly created user.
   * @throws com.mf.HerculaneumTranscriptor.exception.UserAlreadyExistsException if username or email is taken.
   */
  AuthenticationResponse registerNewUser(UserRegisterInfo registrationInfo) throws UserAlreadyExistsException;

  /**
   * Authenticates a user and generates an access token.
   *
   * @param loginInfo DTO containing login credentials.
   * @return An AuthenticationResponse containing the JWT and user info.
   * @throws org.springframework.security.core.AuthenticationException for failed login attempts.
   */
  AuthenticationResponse login(UserLoginInfo loginInfo) throws AuthenticationException;

  /**
   * Finds a user by their username.
   *
   * @param username The username to search for.
   * @return The public information of the found user.
   * @throws com.mf.HerculaneumTranscriptor.exception.ResourceNotFoundException if no user is found.
   */
  UserInfo findUserByUsername(String username) throws ResourceNotFoundException;

  /**
   * Deletes a user from the system.
   * Regular users can delete their own account.
   * Only ROOT or ADMIN users can delete other users, but the ROOT user cannot be deleted.
   *
   * @param username The username of the user to delete.
   * @throws com.mf.HerculaneumTranscriptor.exception.ResourceNotFoundException if no user is found.
   */
  @PreAuthorize("@securityLogic.hasAuthorityOver(authentication, #username)")
  void deleteUserByUsername(String username) throws ResourceNotFoundException;

  /**
   * Updates a user's profile information.
   * Regular users can update their own profile.
   * Only ROOT or ADMIN users can update other users' profiles, but admins cannot update the ROOT user.
   *
   * @param username The username of the user to update.
   * @param updateInfo DTO with the new profile information.
   * @throws com.mf.HerculaneumTranscriptor.exception.ResourceNotFoundException if no user is found.
   */
  @PreAuthorize("hasRole('ROOT') or @securityLogic.hasAuthorityOver(authentication, #username")
  void updateUserProfile(String username, UserRegisterInfo updateInfo) throws ResourceNotFoundException;

  /**
   * Changes the permission level for a given user.
   * Only ROOT or ADMIN users can perform this operation but the target user can never be the ROOT user.
   *
   * @param username The username of the user to update.
   * @param newPermissions DTO containing the new permission level.
   * @throws com.mf.HerculaneumTranscriptor.exception.ResourceNotFoundException if no user is found.
   */
  @PreAuthorize("(hasRole('ROOT') or hasRole('ADMIN')) and @securityLogic.hasAuthorityOver(authentication, #username)")
  void changeUserPermissions(String username, ChangePermissions newPermissions);
}