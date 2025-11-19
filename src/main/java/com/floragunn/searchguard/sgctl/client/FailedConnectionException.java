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

package com.floragunn.searchguard.sgctl.client;

public class FailedConnectionException extends Exception {

    private static final long serialVersionUID = -6678098546026456599L;

    public FailedConnectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public FailedConnectionException(String message) {
        super(message);
    }

    public FailedConnectionException(Throwable cause) {
        super(cause);
    }


}
