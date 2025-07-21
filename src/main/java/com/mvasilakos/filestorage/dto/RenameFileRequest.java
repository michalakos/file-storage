package com.mvasilakos.filestorage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * Request for renaming files.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RenameFileRequest {
  @NotBlank(message = "File name cannot be blank")
  @Size(max = 255, message = "File name cannot exceed 255 characters")
  private String newFileName;
}