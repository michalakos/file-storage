package com.mvasilakos.filestorage.repository;


import com.mvasilakos.filestorage.model.FileMetadata;
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
 * File metadata repository.
 */
@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, UUID> {

  /**
   * Find file with given id for which the given user is an owner.
   *
   * @param id    file id
   * @param owner user
   * @return file metadata
   */
  @Query("SELECT DISTINCT f FROM FileMetadata f "
      + "LEFT JOIN f.sharedWith p "
      + "WHERE f.id = :id AND "
      + "(f.owner = :user OR "
      + "(p.user = :user "
      + "AND p.accessLevel = com.mvasilakos.filestorage.model.FileAccessLevel.OWNER))")
  Optional<FileMetadata> findByIdAndOwner(@Param("id") UUID id, @Param("user") User owner);

  /**
   * Find all files that the given user has access to.
   *
   * @param owner user
   * @return list of file metadata
   */
  @Query("SELECT DISTINCT f FROM FileMetadata f "
      + "LEFT JOIN f.sharedWith p "
      + "WHERE f.owner = :user OR p.user = :user")
  List<FileMetadata> findByOwnerOrSharedWith(@Param("user") User owner);

  /**
   * Find all files that the given user has access to, with pagination.
   *
   * @param user     the user who owns or has access to the files
   * @param pageable pagination information (page number, size, sort)
   * @return a page of file metadata
   */
  @Query("SELECT DISTINCT f FROM FileMetadata f "
      + "LEFT JOIN f.sharedWith p "
      + "WHERE f.owner = :user OR p.user = :user")
  Page<FileMetadata> findByOwnerOrSharedWithPaginated(@Param("user") User user, Pageable pageable);

  /**
   * Search all files that the given user has read only access to, but is not their owner, matching
   * a keyword to their filename, with pagination.
   *
   * @param user     the user who owns or has access to the files
   * @param keyword  the keyword to search for in filenames (should include wildcards)
   * @param pageable pagination information (page number, size, sort)
   * @return a page of file metadata
   */
  @Query("SELECT DISTINCT f FROM FileMetadata f "
      + "LEFT JOIN f.sharedWith p "
      + "WHERE p.user = :user "
      + "AND p.accessLevel = com.mvasilakos.filestorage.model.FileAccessLevel.VIEW "
      + "AND LOWER(f.filename) LIKE LOWER(:keyword)")
  Page<FileMetadata> searchSharedWithPaginated(@Param("user") User user,
      @Param("keyword") String keyword, Pageable pageable);

  /**
   * Search all files that the given user has owner access to, matching a keyword to their filename,
   * with pagination.
   *
   * @param user     the user who owns or has access to the files
   * @param keyword  the keyword to search for in filenames (should include wildcards)
   * @param pageable pagination information (page number, size, sort)
   * @return a page of file metadata
   */
  @Query("SELECT DISTINCT f FROM FileMetadata f "
      + "LEFT JOIN f.sharedWith p "
      + "WHERE (f.owner = :user "
      + "OR (p.user = :user "
      + "AND p.accessLevel = com.mvasilakos.filestorage.model.FileAccessLevel.OWNER)) "
      + "AND LOWER(f.filename) LIKE LOWER(:keyword)")
  Page<FileMetadata> searchOwnedPaginated(@Param("user") User user,
      @Param("keyword") String keyword, Pageable pageable);

  /**
   * Find file with given id which the given user has access to.
   *
   * @param id   file id
   * @param user user
   * @return file metadata
   */
  @Query("SELECT DISTINCT f FROM FileMetadata f "
      + "LEFT JOIN f.sharedWith p "
      + "WHERE f.id = :id "
      + "AND (f.owner = :user OR p.user = :user)")
  Optional<FileMetadata> findByIdAndOwnerOrSharedWith(@Param("id") UUID id,
      @Param("user") User user);

  /**
   * Find the most recently uploaded files that the given user has access to (owns or shared with).
   * Uses limit parameter instead of Pageable.
   *
   * @param user  user to check access for
   * @param limit maximum number of files to return
   * @return list of recent file metadata ordered by upload date (newest first)
   */
  @Query("SELECT DISTINCT f FROM FileMetadata f "
      + "LEFT JOIN f.sharedWith p "
      + "WHERE f.owner = :user OR p.user = :user "
      + "ORDER BY f.uploadDate DESC "
      + "LIMIT :limit")
  List<FileMetadata> findRecentFilesByOwnerOrSharedWithLimit(@Param("user") User user,
      @Param("limit") int limit);

  /**
   * Calculate total size of all files owned by the user.
   *
   * @param owner the user
   * @return total size in bytes, or 0 if no files found
   */
  @Query("SELECT COALESCE(SUM(f.size), 0) FROM FileMetadata f "
      + "WHERE f.owner = :owner")
  Long sumSizeByOwner(@Param("owner") User owner);

  /**
   * Calculate total storage usage across all files.
   *
   * @return total size in bytes, or 0 if no files found
   */
  @Query("SELECT COALESCE(SUM(f.size), 0) FROM FileMetadata f")
  Long calculateTotalStorageUsage();

  /**
   * Find all files with a size that exceeds the given size in bytes.
   *
   * @param size size in bytes
   * @return list of file metadata
   */
  @Query("SELECT DISTINCT f FROM FileMetadata f WHERE f.size > :size")
  List<FileMetadata> findLargerThan(@Param("size") Long size);

}