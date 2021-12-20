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

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.ParseException;
import org.apache.http.message.BasicHeader;

public interface ConditionalRequestHeader extends Header {
    public static class IfNoneMatch implements ConditionalRequestHeader {
        private final String value;

        public IfNoneMatch(String value) {
            this.value = value;
        }

        public Header toHeader() {
            return new BasicHeader("If-None-Match", value);
        }
    }

    public static class IfMatch implements ConditionalRequestHeader {
        private final String value;

        public IfMatch(String value) {
            this.value = value;
        }

        public Header toHeader() {
            return new BasicHeader("If-Match", value);
        }
    }

    Header toHeader();

    default HeaderElement[] getElements() throws ParseException {
        return toHeader().getElements();
    }

    default String getName() {
        return toHeader().getName();
    }

    default String getValue() {
        return toHeader().getValue();
    }
}
