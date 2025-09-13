package com.mvasilakos.filestorage.repository;

import com.mvasilakos.filestorage.model.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


/**
 * User repository.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

  /**
   * Find user by username.
   *
   * @param username username
   * @return user
   */
  Optional<User> findByUsername(String username);

  /**
   * Check if a user with the given username exists.
   *
   * @param username username
   * @return true if user exists
   */
  boolean existsByUsername(String username);

  /**
   * Check if a user with the given email address exists.
   *
   * @param email email address
   * @return true if user exists
   */
  boolean existsByEmail(String email);

  /**
   * Performs a fuzzy search for users where the given keyword is contained (case-insensitively) in
   * their username or email.
   *
   * @param keyword The single keyword to search for.
   * @return A list of users matching the fuzzy search criteria.
   */
  @Query("SELECT u FROM User u WHERE "
      + "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
      + "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))")
  List<User> searchUser(@Param("keyword") String keyword);

  /**
   * Performs a fuzzy search for users where the given keyword is contained (case-insensitively) in
   * their username or email.
   *
   * @param keyword  The single keyword to search for.
   * @param pageable pagination information (page number, size, sort)
   * @return A list of users matching the fuzzy search criteria.
   */
  @Query("SELECT u FROM User u WHERE "
      + "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
      + "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))")
  Page<User> searchUserPaginated(@Param("keyword") String keyword, Pageable pageable);

}
