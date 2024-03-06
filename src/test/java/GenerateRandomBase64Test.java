import com.floragunn.searchguard.sgctl.commands.GenerateRandomBase64;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

class GenerateRandomBase64Test {

    final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();

    @Test
    void testStandardEncoding256Bits() {
        // Execute the command with 256 bits and standard encoding
        CommandLine commandLine = new CommandLine(new GenerateRandomBase64());
        int exitCode = commandLine.execute("-b", "256", "--base64url");
        assertEquals(0, exitCode, "The command should execute successfully.");

        String output = outputStreamCaptor.toString();

        assertTrue(isValidBase64(output), "The output should be valid Base64.");
    }

    @Test
    void testUrlEncoding512Bits() {
        CommandLine commandLine = new CommandLine(new GenerateRandomBase64());
        int exitCode = commandLine.execute("-b", "512", "--base64url");
        assertEquals(0, exitCode, "The command should execute successfully.");

        String output = outputStreamCaptor.toString();
        assertTrue(isValidBase64Url(output), "The output should be valid Base64 URL.");
    }

    @Test
    void testInvalidBits() {
        CommandLine commandLine = new CommandLine(new GenerateRandomBase64());
        int exitCode = commandLine.execute("-b", "123");

        assertEquals(1, exitCode, "The command should fail with invalid bits value.");
    }

    private boolean isValidBase64(String base64) {
        try {
            Base64.getDecoder().decode(base64);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isValidBase64Url(String base64) {
        try {
            Base64.getUrlDecoder().decode(base64);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
