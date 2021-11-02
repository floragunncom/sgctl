package com.floragunn.searchguard.sgctl;

import java.security.Security;

import com.floragunn.searchguard.sgctl.commands.user.UpdateUser;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import com.floragunn.searchguard.sgctl.commands.ComponentState;
import com.floragunn.searchguard.sgctl.commands.Connect;
import com.floragunn.searchguard.sgctl.commands.GetConfig;
import com.floragunn.searchguard.sgctl.commands.MigrateConfig;
import com.floragunn.searchguard.sgctl.commands.ShowLicenses;
import com.floragunn.searchguard.sgctl.commands.ShowVersion;
import com.floragunn.searchguard.sgctl.commands.UpdateConfig;
import com.floragunn.searchguard.sgctl.commands.user.AddUser;
import com.floragunn.searchguard.sgctl.commands.user.DeleteUser;
import com.floragunn.searchguard.sgctl.commands.user.GetUser;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "sgctl", subcommands = { Connect.class, GetConfig.class, UpdateConfig.class, MigrateConfig.class, ComponentState.class,
        ShowLicenses.class, ShowVersion.class, AddUser.class, GetUser.class, UpdateUser.class, DeleteUser.class}, description = "Remote control tool for Search Guard")
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
