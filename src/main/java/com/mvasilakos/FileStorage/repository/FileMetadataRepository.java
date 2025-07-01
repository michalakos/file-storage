package com.mvasilakos.FileStorage.repository;


import com.mvasilakos.FileStorage.model.FileMetadata;
import com.mvasilakos.FileStorage.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, UUID> {

    List<FileMetadata> findByOwner(User owner);

    Optional<FileMetadata> findByIdAndOwner(UUID id, User owner);

    Optional<FileMetadata> findById(UUID id);

}