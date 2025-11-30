package com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml;

import java.util.HashMap;
import java.util.Map;

 public abstract class RealmIR {
    String type; // ldap, saml, oidc, ...
    String name;
    int order;

    public RealmIR(String type, String name) {
        this.type = type;
        this.name = name;
    }

    // each realm type implements its own handler, attribute is suffix after xpack.security.authc.realms.<type>.<name>.
    public abstract void handleAttribute(String attribute, Object value);

    public static RealmIR create(String type, String name) {
        switch(type) {
            case "ldap": return new LdapRealmIR(type, name);
            case "file": return new FileRealmIR(type, name);
            case "native": return new NativeRealmIR(type, name);
            case "saml": return new SamlRealmIR(type, name);
            case "pki": return new PkiRealmIR(type, name);
            case "oidc": return new OidcRealmIR(type, name);
            case "kerberos": return new KerberosRealmIR(type, name);
            default:
                System.out.println("Unknown realm type: " + type);
                return new UnknownRealmIR(type, name);
        }
    }

}
