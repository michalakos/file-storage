package com.mvasilakos.filestorage.repository;


import com.mvasilakos.filestorage.model.FileMetadata;
import com.mvasilakos.filestorage.model.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
   * Return all files that the user is an owner of.
   *
   * @param owner user
   * @return list of file metadata
   */
  @Query("SELECT DISTINCT f FROM FileMetadata f "
      + "LEFT JOIN f.sharedWith p "
      + "WHERE f.owner = :user OR "
      + "(p.user = :user "
      + "AND p.accessLevel = com.mvasilakos.filestorage.model.FileAccessLevel.OWNER)")
  List<FileMetadata> findByOwner(@Param("user") User owner);

  /**
   * Find file with given id for which the given user is an owner.
   *
   * @param id file id
   * @param owner user
   * @return file metadata
   */
  @Query("SELECT DISTINCT f FROM FileMetadata f "
      + "LEFT JOIN f.sharedWith p "
      + "WHERE f.id = :id AND "
      + "(f.owner = :user OR "
      + "(p.user = :user "
      + "AND p.accessLevel = com.mvasilakos.filestorage.model.FileAccessLevel.OWNER))")
  Optional<FileMetadata> findByIdAndOwner(@Param("id") UUID id,
      @Param("user") User owner);

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
   * Find file with given id which the given user has access to.
   *
   * @param id file id
   * @param user user
   * @return file metadata
   */
  @Query("SELECT DISTINCT f FROM FileMetadata f "
      + "LEFT JOIN f.sharedWith p "
      + "WHERE f.id = :id AND (f.owner = :user OR p.user = :user)")
  Optional<FileMetadata> findByIdAndOwnerOrSharedWith(
      @Param("id") UUID id,
      @Param("user") User user);

  /**
   * Find all files with a size that exceeds the given size in bytes.
   *
   * @param size size in bytes
   * @return list of file metadata
   */
  @Query("SELECT DISTINCT f FROM FileMetadata f WHERE f.size > :size")
  List<FileMetadata> findLargerThan(@Param("size") Long size);

}