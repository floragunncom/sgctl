/*
 * Copyright 2025-2026 floragunn GmbH
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


package com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml;

import java.util.logging.Logger;

/**
 * Intermediate representation for elasticsearch.yml security settings.
 */
public class IntermediateRepresentationElasticSearchYml {

    private static final Logger LOG = Logger.getLogger(IntermediateRepresentationElasticSearchYml.class.getName());

    private final GlobalIR global;
    private final SslTlsIR sslTls;
    private final AuthenticationIR authent;

    public GlobalIR getGlobal() { return global; }
    public SslTlsIR getSslTls() { return sslTls; }
    public AuthenticationIR getAuthent() { return authent; }

    public IntermediateRepresentationElasticSearchYml() {
        global = new GlobalIR();
        sslTls = new SslTlsIR();
        authent = new AuthenticationIR();
    }

    /**
     * Helper to express intent when validating parsed config types.
     */
    public static boolean isType(Object object, Class<?> type) {
        return type.isInstance(object);
    }

    /**
     * Backwards-compatible alias for older code/tests.
     *
     * @deprecated use {@link #isType(Object, Class)} instead.
     */
    @Deprecated(since = "1.0", forRemoval = false)
    public static boolean assertType(Object object, Class<?> type) {
        return isType(object, type);
    }

    /**
     * Legacy logger kept for compatibility with existing tests.
     *
     * @deprecated prefer structured reporting via {@link com.floragunn.searchguard.sgctl.util.mapping.MigrationReport}.
     */
    @Deprecated(since = "1.0", forRemoval = false)
    public static void errorLog(String message, int severity) {
        String prefix = switch (severity) {
            case 1 -> "Needs Manual rework: ";
            case 2 -> "Critical issue!: ";
            default -> "";
        };
        LOG.info(prefix + message);
    }

}