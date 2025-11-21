package com.floragunn.searchguard.sgctl.migratetest;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;

public abstract class TestBase {

    protected Path resolveResourcePath(String resourceName) {
        var classLoader = getClass().getClassLoader();
        var resourceUrl = classLoader.getResource(resourceName);

        if (resourceUrl == null) {
            throw new IllegalArgumentException("Test resource not found: " + resourceName);
        }

        try {
            return Path.of(resourceUrl.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Failed to resolve resource path: " + resourceName, e);
        }
    }

    protected String readResourceAsString(String resourceName) {
        Path path = resolveResourcePath(resourceName);
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read test resource: " + resourceName, e);
        }
    }

    @BeforeEach
    void beforeEachTest() {
        // Platz für gemeinsame Setup-Logik (Logging, Mocks, ...)
        // absichtlich leer gelassen, um SonarQube-Warnungen zu vermeiden
    }
}