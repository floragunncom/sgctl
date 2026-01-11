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
