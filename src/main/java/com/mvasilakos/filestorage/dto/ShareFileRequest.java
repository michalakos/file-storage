package com.mvasilakos.filestorage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * Request object for sharing files.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShareFileRequest {

  @NotBlank(message = "Username cannot be blank")
  @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
  @Pattern(regexp = "^[a-zA-Z0-9._-]+$",
      message = "Username can only contain letters, numbers, dots, hyphens, and underscores")
  private String username;

  @NotNull(message = "ReadOnly flag must be specified")
  private Boolean readOnly;

  public boolean isReadOnly() {
    return readOnly != null && readOnly;
  }
}