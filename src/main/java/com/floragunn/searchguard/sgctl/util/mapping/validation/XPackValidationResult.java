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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Collects validation issues for X-Pack configuration input.
 */
public final class XPackValidationResult {

    private final List<XPackValidationIssue> issues = new ArrayList<>();

    /**
     * Adds a new validation issue.
     *
     * @param issue issue to add
     * @return this instance for chaining
     */
    public XPackValidationResult addIssue(XPackValidationIssue issue) {
        if (issue != null) {
            issues.add(issue);
        }
        return this;
    }

    /**
     * Returns an unmodifiable view of all collected issues.
     *
     * @return list of collected issues
     */
    public List<XPackValidationIssue> getIssues() {
        return Collections.unmodifiableList(issues);
    }

    /**
     * Returns whether at least one error level issue has been collected.
     *
     * @return {@code true} if at least one error exists
     */
    public boolean hasErrors() {
        return issues.stream()
                .anyMatch(issue -> issue.severity() == XPackValidationSeverity.ERROR);
    }

    /**
     * Returns whether at least one warning level issue has been collected.
     *
     * @return {@code true} if at least one warning exists
     */
    public boolean hasWarnings() {
        return issues.stream()
                .anyMatch(issue -> issue.severity() == XPackValidationSeverity.WARNING);
    }

    /**
     * Returns whether no issues have been collected.
     *
     * @return {@code true} if no issues have been added
     */
    public boolean isEmpty() {
        return issues.isEmpty();
    }
}
