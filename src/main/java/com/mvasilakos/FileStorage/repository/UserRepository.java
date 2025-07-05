package com.mvasilakos.FileStorage.repository;

import com.mvasilakos.FileStorage.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    /**
     * Performs a fuzzy search for users where the given keyword is contained
     * (case-insensitively) in their username or email.
     *
     * @param keyword The single keyword to search for.
     * @return A list of users matching the fuzzy search criteria.
     */
    @Query("SELECT u FROM User u WHERE " +
        "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
        "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<User> searchUser(@Param("keyword") String keyword);

}
