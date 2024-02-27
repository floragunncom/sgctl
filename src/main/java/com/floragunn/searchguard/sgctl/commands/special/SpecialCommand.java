/*
 * Copyright 2022 floragunn GmbH
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

package com.floragunn.searchguard.sgctl.commands.special;

import com.floragunn.searchguard.sgctl.commands.special.multitenancy.datamigration880.GetMultiTenancyDataMigrationState;
import com.floragunn.searchguard.sgctl.commands.special.multitenancy.datamigration880.StartMultiTenancyDataMigration;
import picocli.CommandLine.Command;

/**
 * This is just a container for other "special" commands. Special in this context means that these commands are only needed rarely, maybe even only once, or that these commands perform low-level operations.
 */
@Command(name = "special", description = "Commands for special circumstances", subcommands = {
        MoveSearchGuardIndexCommand.class, GetMultiTenancyDataMigrationState.class, StartMultiTenancyDataMigration.class }
)
public class SpecialCommand {

}
