/*
 * Copyright 2021-2022 floragunn GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.floragunn.searchguard.sgctl;

import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import com.floragunn.searchguard.sgctl.commands.ComponentState;
import com.floragunn.searchguard.sgctl.commands.Connect;
import com.floragunn.searchguard.sgctl.commands.GetConfig;
import com.floragunn.searchguard.sgctl.commands.MigrateConfig;
import com.floragunn.searchguard.sgctl.commands.SetCommand;
import com.floragunn.searchguard.sgctl.commands.ShowLicenses;
import com.floragunn.searchguard.sgctl.commands.ShowVersion;
import com.floragunn.searchguard.sgctl.commands.UpdateConfig;
import com.floragunn.searchguard.sgctl.commands.user.AddUser;
import com.floragunn.searchguard.sgctl.commands.user.AddUserLocal;
import com.floragunn.searchguard.sgctl.commands.user.DeleteUser;
import com.floragunn.searchguard.sgctl.commands.user.UpdateUser;
import com.floragunn.searchguard.sgctl.commands.vars.AddConfigVar;
import com.floragunn.searchguard.sgctl.commands.vars.DeleteConfigVar;
import com.floragunn.searchguard.sgctl.commands.vars.UpdateConfigVar;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "sgctl", subcommands = { Connect.class, GetConfig.class, UpdateConfig.class, MigrateConfig.class, ComponentState.class,
        ShowLicenses.class, ShowVersion.class, AddUserLocal.class, AddUser.class, UpdateUser.class, DeleteUser.class, AddConfigVar.class,
        UpdateConfigVar.class, DeleteConfigVar.class, SetCommand.class }, description = "Remote control tool for Search Guard")
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
        return new CommandLine(new SgctlTool()).setCaseInsensitiveEnumValuesAllowed(true).execute(args);
    }

}
