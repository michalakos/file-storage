package com.mvasilakos.filestorage.repository;

import com.mvasilakos.filestorage.model.FilePermission;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


/**
 * File permission repository.
 */
@Repository
public interface FilePermissionRepository extends JpaRepository<FilePermission, UUID> {}
