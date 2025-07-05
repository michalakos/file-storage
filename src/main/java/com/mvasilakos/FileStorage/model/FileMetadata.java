package com.mvasilakos.FileStorage.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
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

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private Long size;

    @Column(nullable = false)
    private LocalDateTime uploadDate;

    @Column(nullable = false, unique = true)
    private String storagePath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    @ToString.Exclude
    private User owner;

    @OneToMany(mappedBy = "fileMetadata", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private Set<FilePermission> sharedWith = new HashSet<>();

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