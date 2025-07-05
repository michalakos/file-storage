package com.mvasilakos.FileStorage.repository;

import com.mvasilakos.FileStorage.model.FilePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FilePermissionRepository extends JpaRepository<FilePermission, UUID> {

}
