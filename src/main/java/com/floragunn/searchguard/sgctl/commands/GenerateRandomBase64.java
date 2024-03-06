package com.floragunn.searchguard.sgctl.commands;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.Callable;

@Command(name = "generateRandomBase64", mixinStandardHelpOptions = true, version = "1.0",
        description = "Generates a random base 64 encoded integer.")
public class GenerateRandomBase64 implements Callable<Integer> {

    @Option(names = {"-b", "--bits"}, description = "Number of bits for the random number (must be divisible by 8). Defaults to 256 bits")
    private int bits = 256;

    @Option(names = {"--base64url"}, description = "If the encoding type should be of type 'url' instead of 'standard'")
    private Boolean base64url = false;

    @Override
    public Integer call() throws Exception {
        if (bits % 8 != 0) {
            System.err.println("The number of bits must be divisible by 8.");
            return 1;
        }

        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[bits / 8];
        random.nextBytes(bytes);
        String encoded;

        if (base64url) {
            encoded = Base64.getUrlEncoder().encodeToString(bytes);
        } else {
            encoded = Base64.getEncoder().encodeToString(bytes);
        }

        System.out.println(encoded);
        return 0;
    }
}