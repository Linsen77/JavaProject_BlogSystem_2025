package com.blog.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class AvatarStorage {
    private final Path directory;

    public AvatarStorage(@Value("${blog.avatar.upload-dir:./data/avatars}") String directory) {
        this.directory = Path.of(directory).toAbsolutePath().normalize();
    }

    public Path directory() { return directory; }

    public String save(MultipartFile avatar) throws IOException {
        Files.createDirectories(directory);
        Path target = directory.resolve(UUID.randomUUID() + ".png");
        try {
            avatar.transferTo(target);
        } catch (IOException | RuntimeException failure) {
            try { Files.deleteIfExists(target); } catch (IOException cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
        return "/avatar/" + target.getFileName();
    }
}
