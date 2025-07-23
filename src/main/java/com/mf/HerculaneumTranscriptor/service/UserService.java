package com.mf.HerculaneumTranscriptor.service;

import com.mf.HerculaneumTranscriptor.dto.AuthenticationResponse;
import com.mf.HerculaneumTranscriptor.exception.ResourceNotFoundException;
import com.mf.HerculaneumTranscriptor.exception.UserAlreadyExistsException;
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
   *
   * @param username The username of the user to delete.
   * @throws com.mf.HerculaneumTranscriptor.exception.ResourceNotFoundException if no user is found.
   */
  void deleteUserByUsername(String username) throws ResourceNotFoundException;

  /**
   * Updates a user's profile information.
   *
   * @param username The username of the user to update.
   * @param updateInfo DTO with the new profile information.
   * @throws com.mf.HerculaneumTranscriptor.exception.ResourceNotFoundException if no user is found.
   */
  void updateUserProfile(String username, UserRegisterInfo updateInfo) throws ResourceNotFoundException;

  /**
   * Changes the permission level for a given user.
   *
   * @param username The username of the user to update.
   * @param newPermissions DTO containing the new permission level.
   * @throws com.mf.HerculaneumTranscriptor.exception.ResourceNotFoundException if no user is found.
   */
  void changeUserPermissions(String username, ChangePermissions newPermissions);
}