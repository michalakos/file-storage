package com.mvasilakos.FileStorage.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "file_metadata")
@Getter
@Setter
@NoArgsConstructor
@ToString
public class FileMetadata {

    @Id
    @Column(updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @Column(nullable = false)
    private String filename;

    @Column(name="content_type", nullable = false)
    private String contentType;

    @Column(nullable = false)
    private Long size;

    @Column(name = "upload_date", nullable = false)
    private LocalDateTime uploadDate;

    @Column(name = "storage_path", nullable = false, unique = true)
    private String storagePath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    @ToString.Exclude
    private User owner;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FileMetadata that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

}