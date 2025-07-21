package com.mvasilakos.filestorage.controller;

import com.mvasilakos.filestorage.dto.FileMetadataDto;
import com.mvasilakos.filestorage.dto.RenameFileRequest;
import com.mvasilakos.filestorage.model.User;
import com.mvasilakos.filestorage.service.FileService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


/**
 * File controller.
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

  private final FileService fileService;

  /**
   * Upload a file.
   *
   * @param file  the file to be uploaded
   * @param owner the user uploading the file
   * @return uploaded file's metadata
   */
  @PostMapping
  public ResponseEntity<FileMetadataDto> uploadFile(@RequestParam("file") MultipartFile file,
      @AuthenticationPrincipal User owner) {
    FileMetadataDto metadata = fileService.storeFile(file, owner);
    return ResponseEntity.status(HttpStatus.CREATED).body(metadata);
  }

  /**
   * List all files that the given user can access.
   *
   * @param user the user that is signed in
   * @return list of file metadata
   */
  @GetMapping
  public ResponseEntity<List<FileMetadataDto>> listFiles(@AuthenticationPrincipal User user) {
    List<FileMetadataDto> metadata = fileService.listUserFiles(user);
    return ResponseEntity.ok(metadata);
  }

  /**
   * Get metadata for file with given id for the given user if they have access to it.
   *
   * @param id   file's id
   * @param user the user that is logged in
   * @return file metadata
   */
  @GetMapping("/{id}")
  public ResponseEntity<FileMetadataDto> getFileMetadata(@PathVariable UUID id,
      @AuthenticationPrincipal User user) {
    FileMetadataDto metadata = fileService.getFileMetadata(id, user);
    return ResponseEntity.ok(metadata);
  }

  /**
   * Download a file if the user has access to it.
   *
   * @param id   id of the file to download
   * @param user the user that is logged in
   * @return file data
   */
  @GetMapping("/{id}/data")
  public ResponseEntity<Resource> downloadFile(@PathVariable UUID id,
      @AuthenticationPrincipal User user) {
    Resource resource = fileService.loadFileAsResource(id, user);
    FileMetadataDto metadata = fileService.getFileMetadata(id, user);

    return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + metadata.filename() + "\"")
        .header(HttpHeaders.CONTENT_TYPE, metadata.contentType()).body(resource);
  }

  /**
   * Rename a file if the user is an owner of it.
   *
   * @param id            file id
   * @param renameRequest request object containing new file name
   * @param user          user that is logged in
   * @return file data
   */
  @PatchMapping("/{id}/rename")
  public ResponseEntity<FileMetadataDto> renameFile(@PathVariable UUID id,
      @RequestBody @Valid RenameFileRequest renameRequest, @AuthenticationPrincipal User user) {

    FileMetadataDto fileMetadataDto = fileService.renameFile(id, renameRequest.getNewFileName(),
        user);
    return ResponseEntity.ok(fileMetadataDto);
  }

  /**
   * Delete the given file if the user has permission to do it.
   *
   * @param id    the id of the file to be deleted.
   * @param owner the user that is logged in.
   * @return nothing
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteFile(@PathVariable UUID id,
      @AuthenticationPrincipal User owner) {
    fileService.deleteFile(id, owner);
    return ResponseEntity.noContent().build();
  }

  /**
   * Share a file with another user. The user sharing the file must have access to it.
   *
   * @param fileId   file id
   * @param username the username of the user we want to share the file with
   * @param readOnly the user should only have access to view/download the file
   * @param owner    the user that is logged in
   * @return nothing
   */
  @PostMapping("/share/{fileId}/{username}/{readOnly}")
  public ResponseEntity<Void> shareFile(@PathVariable UUID fileId, @PathVariable String username,
      @PathVariable boolean readOnly, @AuthenticationPrincipal User owner) {
    fileService.shareFile(fileId, username, readOnly, owner);
    return ResponseEntity.noContent().build();
  }
}