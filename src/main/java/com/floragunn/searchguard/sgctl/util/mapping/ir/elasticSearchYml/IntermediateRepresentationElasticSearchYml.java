package com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml;


public class IntermediateRepresentationElasticSearchYml {

    GlobalIR global;
    SslTlsIR sslTls;
    AuthenticationIR authent;

    public GlobalIR getGlobal() { return global; }
    public SslTlsIR getSslTls() { return sslTls; }
    public AuthenticationIR getAuthent() { return authent; }

    public IntermediateRepresentationElasticSearchYml() {
        global = new GlobalIR();
        sslTls = new SslTlsIR();
        authent = new AuthenticationIR();
    }

    // before setting an option, check that its type matches
    public static boolean assertType(Object object, Class<?> type) {
        return type.isInstance(object);
    }
}