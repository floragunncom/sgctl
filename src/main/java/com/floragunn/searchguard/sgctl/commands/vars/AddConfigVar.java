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

package com.floragunn.searchguard.sgctl.commands.vars;

import java.util.concurrent.Callable;

import org.apache.http.Header;

import com.floragunn.searchguard.sgctl.client.ConditionalRequestHeader.IfNoneMatch;

import picocli.CommandLine.Command;

@Command(name = "add-var", description = "Adds a new configuration variable")
public class AddConfigVar extends AddOrUpdateConfigVar implements Callable<Integer> {

    @Override
    protected Header[] getHeaders() {
        return new Header[] { new IfNoneMatch("*") };
    }

}
