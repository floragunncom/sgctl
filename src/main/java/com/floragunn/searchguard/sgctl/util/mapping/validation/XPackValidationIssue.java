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


package com.floragunn.searchguard.sgctl.util.mapping.validation;

import java.util.Objects;

/**
 * Represents a single validation issue found in X-Pack configuration input.
 */
public record XPackValidationIssue(
        XPackValidationSeverity severity,
        String componentType,
        String identifier,
        String field,
        String message
) {

    /**
     * Creates a new validation issue.
     *
     * @param severity severity of the issue
     * @param componentType type of the affected component, for example "role", "user" or "role-mapping"
     * @param identifier logical identifier of the component, for example the role name
     * @param field affected field name or {@code null} if the issue is not tied to a single field
     * @param message human-readable description of the problem
     */
    public XPackValidationIssue {
        Objects.requireNonNull(severity, "severity must not be null");
        Objects.requireNonNull(message, "message must not be null");
    }

    /**
     * Returns a short human-readable representation of the issue.
     *
     * @return string representation of the issue
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append(severity);
        builder.append(": ");

        if (componentType != null) {
            builder.append(componentType);
            if (identifier != null) {
                builder.append(" '").append(identifier).append('\'');
            }
            builder.append(": ");
        }

        if (field != null) {
            builder.append('[').append(field).append("] ");
        }

        builder.append(message);

        return builder.toString();
    }
}
