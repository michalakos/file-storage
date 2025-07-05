package com.mvasilakos.FileStorage.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

@Entity
@Table(name = "file_permissions")
@Getter
@Setter
@NoArgsConstructor
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
