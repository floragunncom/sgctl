package com.floragunn.searchguard.sgctl.util.mapping.ir;


public class IntermediateRepresentation {

    GlobalIR global;
    SslTlsIR sslTls;
    AuthorizationIR authoIR;
    AuthenticationIR authentIR;

    // before setting an option, check that its type matches
    public static boolean assertType(Object object, Class<?> type) {
        return type.isInstance(object);
    }

    // classify into severity: 0 -> info, 1 -> needs manual rework, 2 -> critical
    public static void errorLog(String message, int severity) {
        switch (severity) {
            case 0:
                System.out.println(message);
                break;
            case 1:
                System.out.println("Needs Manual rework: " + message);
                break;
            case 2:
                System.out.println("Critical issue!: " + message);
                break;
        }
    }

}

/*

IR:
global ...
ssl/tls ...
    - transport
    - http
authorization ...
authentication ...
    - Map<String, Object> users
    for (String user : users)


in searchguard:

elasticsearch.yml:
    - searchguard.nodes_dn: enumerate





* */