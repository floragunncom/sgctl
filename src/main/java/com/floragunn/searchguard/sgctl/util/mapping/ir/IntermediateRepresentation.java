package com.floragunn.searchguard.sgctl.util.mapping.ir;

import com.floragunn.searchguard.sgctl.util.mapping.ir.authentication.AuthenticationIR;

public class IntermediateRepresentation {

    GlobalIR global;
    SslTlsIR sslTls;
    AuthorizationIR authoIR;
    AuthenticationIR authentIR;


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