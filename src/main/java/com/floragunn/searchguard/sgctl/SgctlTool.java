package com.floragunn.searchguard.sgctl;

import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import com.floragunn.searchguard.sgctl.commands.Connect;
import com.floragunn.searchguard.sgctl.commands.GetConfig;
import com.floragunn.searchguard.sgctl.commands.ComponentState;
import com.floragunn.searchguard.sgctl.commands.UpdateConfig;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "sgctl", subcommands = { Connect.class, GetConfig.class, UpdateConfig.class, ComponentState.class }, description = "Remote control tool for Search Guard")
public class SgctlTool {

    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public static void main(String... args) {
        int exitCode = exec(args);
        System.exit(exitCode);
    }

    public static int exec(String... args) {
        return new CommandLine(new SgctlTool()).execute(args);
    }

}
