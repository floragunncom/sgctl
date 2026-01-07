package com.floragunn.searchguard.sgctl.testsupport;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Base test class that suppresses stdout/stderr during each test to reduce noise.
 */
public abstract class QuietTestBase extends TestBase {
    private PrintStream originalOut;
    private PrintStream originalErr;
    private ByteArrayOutputStream outBuffer;
    private ByteArrayOutputStream errBuffer;

    /**
     * Redirects stdout and stderr to in-memory buffers.
     */
    @BeforeEach
    void suppressOutput() {
        originalOut = System.out;
        originalErr = System.err;
        outBuffer = new ByteArrayOutputStream();
        errBuffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outBuffer));
        System.setErr(new PrintStream(errBuffer));
    }

    /**
     * Restores stdout and stderr after each test.
     */
    @AfterEach
    void restoreOutput() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }
}
