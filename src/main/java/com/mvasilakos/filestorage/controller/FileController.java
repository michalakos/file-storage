package com.mvasilakos.filestorage.controller;

import com.mvasilakos.filestorage.dto.FileMetadataDto;
import com.mvasilakos.filestorage.dto.RenameFileRequest;
import com.mvasilakos.filestorage.dto.ShareFileRequest;
import com.mvasilakos.filestorage.model.User;
import com.mvasilakos.filestorage.service.FileService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
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
 * File controller for managing file operations.
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Validated
@Slf4j
public class FileController {

  private final FileService fileService;

  /**
   * Upload a file.
   *
   * @param file  the file to be uploaded
   * @param owner the user uploading the file
   * @return uploaded file's metadata
   */
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<FileMetadataDto> uploadFile(@RequestParam("file") @Valid MultipartFile file,
      @AuthenticationPrincipal User owner) {

    log.debug("Uploading file: \"{}\" for user: {}",
        file.getOriginalFilename(), owner.getUsername());
    FileMetadataDto metadata = fileService.uploadFile(file, owner);
    return ResponseEntity.status(HttpStatus.CREATED).body(metadata);
  }

  /**
   * List all files that the given user can access.
   *
   * @param user the authenticated user
   * @return list of file metadata
   */
  @GetMapping
  public ResponseEntity<List<FileMetadataDto>> listFiles(@AuthenticationPrincipal User user) {
    log.debug("Listing files for user: {}", user.getUsername());
    List<FileMetadataDto> metadata = fileService.listUserFiles(user);
    return ResponseEntity.ok(metadata);
  }

  /**
   * List all files that the given user can access, with pagination.
   *
   * @param currentUser the authenticated user
   * @param page        the page number
   * @param size        the number of elements on the page
   * @return list of file metadata
   */
  @GetMapping("/paginated")
  public ResponseEntity<Page<FileMetadataDto>> getFiles(
      @AuthenticationPrincipal User currentUser,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {

    Page<FileMetadataDto> files = fileService.listUserFilesPaginated(currentUser, page, size);
    return ResponseEntity.ok(files);
  }

  /**
   * Search all files that the given user can access, with pagination.
   *
   * @param currentUser the authenticated user
   * @param page        the page number
   * @param size        the number of elements on the page
   * @return list of file metadata
   */
  @GetMapping("/paginated-search")
  public ResponseEntity<Page<FileMetadataDto>> searchFiles(
      @AuthenticationPrincipal User currentUser,
      @RequestParam(defaultValue = "") String keyword,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {

    Page<FileMetadataDto> files = fileService.searchUserFilesPaginated(
        currentUser, keyword, page, size);
    return ResponseEntity.ok(files);
  }

  /**
   * Search all files that the given user has read-only access, with pagination.
   *
   * @param currentUser the authenticated user
   * @param page        the page number
   * @param size        the number of elements on the page
   * @return list of file metadata
   */
  @GetMapping("/paginated-search-shared")
  public ResponseEntity<Page<FileMetadataDto>> searchSharedFiles(
      @AuthenticationPrincipal User currentUser,
      @RequestParam(defaultValue = "") String keyword,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {

    Page<FileMetadataDto> files = fileService.searchSharedFilesPaginated(
        currentUser, keyword, page, size);
    return ResponseEntity.ok(files);
  }

  /**
   * List most recent files that the given user can access, with an optional limit on returned
   * items.
   *
   * @param user  the authenticated user
   * @param limit optional limit on number of files
   * @return list of file metadata
   */
  @GetMapping("/recent")
  public ResponseEntity<List<FileMetadataDto>> listRecentFiles(
      @AuthenticationPrincipal User user,
      @RequestParam(defaultValue = "10") int limit) {
    log.debug("Listing {} most recent files for user: {}", limit, user.getUsername());
    List<FileMetadataDto> metadata = fileService.listRecentUserFilesWithLimit(user, limit);
    return ResponseEntity.ok(metadata);
  }


  /**
   * Get metadata for file with given id for the given user if they have access to it.
   *
   * @param id   file's id
   * @param user the authenticated user
   * @return file metadata
   */
  @GetMapping("/{id}")
  public ResponseEntity<FileMetadataDto> getFileMetadata(@PathVariable @Valid UUID id,
      @AuthenticationPrincipal User user) {

    log.debug("Getting metadata for file: {} by user: {}", id, user.getUsername());
    FileMetadataDto metadata = fileService.getFileMetadata(id, user);
    return ResponseEntity.ok(metadata);
  }

  /**
   * Download a file if the user has access to it.
   *
   * @param id   id of the file to download
   * @param user the authenticated user
   * @return file data with appropriate headers
   */
  @GetMapping("/{id}/download")
  public ResponseEntity<Resource> downloadFile(@PathVariable @Valid UUID id,
      @AuthenticationPrincipal User user) {

    log.debug("Downloading file: {} by user: {}", id, user.getUsername());
    Resource resource = fileService.downloadFile(id, user);
    FileMetadataDto metadata = fileService.getFileMetadata(id, user);

    ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + sanitizeFilename(metadata.filename()) + "\"")
        .header(HttpHeaders.CONTENT_TYPE, metadata.contentType());

    // Try to set content length, but don't fail if we can't determine it
    try {
      long contentLength = resource.contentLength();
      if (contentLength >= 0) {
        responseBuilder.header(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength));
      }
    } catch (IOException e) {
      log.warn("Could not determine content length for file: {}", id, e);
      // Continue without content-length header
    }

    return responseBuilder.body(resource);
  }

  /**
   * Rename a file if the user is an owner of it.
   *
   * @param id            file id
   * @param renameRequest request object containing new file name
   * @param user          authenticated user
   * @return updated file metadata
   */
  @PatchMapping("/{id}/rename")
  public ResponseEntity<FileMetadataDto> renameFile(@PathVariable @Valid UUID id,
      @RequestBody @Valid RenameFileRequest renameRequest, @AuthenticationPrincipal User user) {

    log.debug("Renaming file: {} to: \"{}\"", id, renameRequest.getNewFileName());

    FileMetadataDto fileMetadataDto = fileService.renameFile(id, renameRequest.getNewFileName(),
        user);
    return ResponseEntity.ok(fileMetadataDto);
  }

  /**
   * Delete a file if the user has permission to do it.
   *
   * @param id   the id of the file to be deleted
   * @param user the authenticated user
   * @return no content response
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteFile(@PathVariable @Valid UUID id,
      @AuthenticationPrincipal User user) {

    log.debug("Deleting file: {} by user: {}", id, user.getUsername());
    fileService.deleteFile(id, user);
    return ResponseEntity.noContent().build();
  }

  /**
   * Share a file with another user. The user sharing the file must have owner access to it.
   *
   * @param fileId       file id
   * @param shareRequest request object containing sharing details
   * @param owner        the authenticated user
   * @return no content response
   */
  @PostMapping("/{fileId}/share")
  public ResponseEntity<Void> shareFile(@PathVariable("fileId") @Valid UUID fileId,
      @RequestBody @Valid ShareFileRequest shareRequest, @AuthenticationPrincipal User owner) {

    log.debug("Sharing file: {} with user: {} (readOnly: {}) by owner: {}", fileId,
        shareRequest.getUsername(), shareRequest.isReadOnly(), owner.getUsername());

    fileService.shareFile(fileId, shareRequest.getUsername(), shareRequest.isReadOnly(), owner);
    return ResponseEntity.noContent().build();
  }

  /**
   * Sanitize filename for Content-Disposition header to prevent header injection.
   *
   * @param filename the original filename
   * @return sanitized filename
   */
  private String sanitizeFilename(String filename) {
    if (filename == null) {
      return "download";
    }
    return filename.replaceAll("[\"\\\\]", "_");
  }

  /**
   * Global exception handler for validation errors.
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, String>> handleValidationErrors(
      MethodArgumentNotValidException ex) {

    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult().getFieldErrors()
        .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

    return ResponseEntity.badRequest().body(errors);
  }
}