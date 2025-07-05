package com.mvasilakos.FileStorage.repository;


import com.mvasilakos.FileStorage.model.FileMetadata;
import com.mvasilakos.FileStorage.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, UUID> {

    @Query("SELECT DISTINCT f FROM FileMetadata f " +
        "LEFT JOIN f.sharedWith p " +
        "WHERE f.owner = :user OR " +
        "(p.user = :user AND p.accessLevel = com.mvasilakos.FileStorage.model.FileAccessLevel.OWNER)")
    List<FileMetadata> findByOwner(@Param("user") User owner);

    @Query("SELECT DISTINCT f FROM FileMetadata f " +
        "LEFT JOIN f.sharedWith p " +
        "WHERE f.id = :id AND " +
        "(f.owner = :user OR " +
        "(p.user = :user AND p.accessLevel = com.mvasilakos.FileStorage.model.FileAccessLevel.OWNER))")
    Optional<FileMetadata> findByIdAndOwner(@Param("id") UUID id,
                                            @Param("user") User owner);

    @Query("SELECT DISTINCT f FROM FileMetadata f " +
        "LEFT JOIN f.sharedWith p " +
        "WHERE f.owner = :user OR p.user = :user")
    List<FileMetadata> findByOwnerOrSharedWith(@Param("user") User owner);

    @Query("SELECT DISTINCT f FROM FileMetadata f " +
        "LEFT JOIN f.sharedWith p " +
        "WHERE f.id = :id AND (f.owner = :user OR p.user = :user)")
    Optional<FileMetadata> findByIdAndOwnerOrSharedWith(@Param("id") UUID id, @Param("user") User user);

    @Query("SELECT DISTINCT f FROM FileMetadata f WHERE f.size > :size")
    List<FileMetadata> findLargerThan(@Param("size") Long size);

}