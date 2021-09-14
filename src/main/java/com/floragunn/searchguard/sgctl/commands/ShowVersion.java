/*
 * Copyright 2021 floragunn GmbH
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

package com.floragunn.searchguard.sgctl.commands;

import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.Callable;

import com.floragunn.searchguard.sgctl.SgctlException;

import picocli.CommandLine.Command;

@Command(name = "sgctl-version", description = "Shows the version of this sgctl command")
public class ShowVersion extends BaseCommand implements Callable<Integer> {
    public Integer call() throws Exception {

        InputStream inputStream = getClass().getResourceAsStream("/META-INF/maven/com.floragunn/sgctl/pom.properties");
        if (inputStream == null) {
            throw new SgctlException("Could not find resource /META-INF/maven/com.floragunn/sgctl/pom.properties");
        }

        Properties pomProperties = new Properties();
        pomProperties.load(inputStream);

        System.out.println("scgtl " + pomProperties.getProperty("version"));

        return 0;
    }
}
