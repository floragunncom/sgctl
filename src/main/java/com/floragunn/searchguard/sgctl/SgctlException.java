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

package com.floragunn.searchguard.sgctl;

public class SgctlException extends Exception {

    private static final long serialVersionUID = 6181282719824113444L;

    private String debugDetail;

    public SgctlException() {
        super();
    }

    public SgctlException(String message, Throwable cause) {
        super(message, cause);
    }

    public SgctlException(String message) {
        super(message);
    }

    public SgctlException(Throwable cause) {
        super(cause);
    }

    public SgctlException debugDetail(String debugDetail) {
        this.debugDetail = debugDetail;
        return this;
    }

    public String getDebugDetail() {
        return debugDetail;
    }

}
