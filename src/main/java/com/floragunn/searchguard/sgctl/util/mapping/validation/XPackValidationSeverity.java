package com.floragunn.searchguard.sgctl.util.mapping.validation;

/**
 * Severity of a validation issue for X-Pack configuration.
 */
public enum XPackValidationSeverity {

    /**
     * A problem that prevents correct migration and must be fixed.
     */
    ERROR,

    /**
     * A non-fatal problem that should be reviewed.
     */
    WARNING
}
