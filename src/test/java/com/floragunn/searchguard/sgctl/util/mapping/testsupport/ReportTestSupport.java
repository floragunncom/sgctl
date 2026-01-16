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


package com.floragunn.searchguard.sgctl.util.mapping.testsupport;

import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;

/**
 * Helper methods for creating isolated {@link MigrationReport} instances in tests.
 */
public final class ReportTestSupport {
    private ReportTestSupport() {
    }

    /**
     * Creates a new report instance without using the shared singleton.
     *
     * @return fresh migration report
     */
    public static MigrationReport newReport() {
        try {
            var ctor = MigrationReport.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create isolated MigrationReport", e);
        }
    }
}
