package com.mvasilakos.filestorage.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


/**
 * File permissions entity describing what permissions users that are not the original owners of
 * the files have on them.
 */
@Entity
@Table(name = "file_permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class FilePermission {

  @Id
  @Column(updatable = false, nullable = false, columnDefinition = "UUID")
  private UUID id;

  @ManyToOne
  private FileMetadata fileMetadata;

  @ManyToOne
  private User user;

  @Enumerated(EnumType.STRING)
  private FileAccessLevel accessLevel;

}
