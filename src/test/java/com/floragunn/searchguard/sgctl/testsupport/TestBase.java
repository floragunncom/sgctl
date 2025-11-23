package com.floragunn.searchguard.sgctl.testsupport;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Base class for JUnit tests with resource helper methods.
 */
public abstract class TestBase {

    /**
     * Resolves a classpath resource name to a file system path.
     *
     * @param resourceName the relative resource name, e.g. "testbase/example.txt"
     * @return the resolved path
     */
    protected Path resolveResourcePath(String resourceName) {
        if (resourceName == null || resourceName.isBlank()) {
            throw new IllegalArgumentException("resourceName must not be null or blank");
        }

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = getClass().getClassLoader();
        }

        URL resourceUrl = classLoader.getResource(resourceName);
        if (resourceUrl == null) {
            throw new IllegalArgumentException("Test resource not found: " + resourceName);
        }

        try {
            return Path.of(resourceUrl.toURI());
        } catch (URISyntaxException exception) {
            throw new IllegalStateException("Failed to resolve resource path: " + resourceName, exception);
        }
    }

    /**
     * Reads a classpath resource as a UTF-8 string.
     *
     * @param resourceName the relative resource name, e.g. "testbase/example.txt"
     * @return the resource content as string
     */
    protected String readResourceAsString(String resourceName) {
        Path path = resolveResourcePath(resourceName);
        try {
            byte[] bytes = Files.readAllBytes(path);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read test resource: " + resourceName, exception);
        }
    }

    /**
     * Normalizes line endings to '\n'.
     *
     * @param text the input text
     * @return the text with normalized line endings
     */
    protected String normalizeLineEndings(String text) {
        if (text == null) {
            return "";
        }
        String withUnix = text.replace("\r\n", "\n");
        return withUnix.replace("\r", "\n");
    }
}
