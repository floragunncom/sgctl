import java.util.HashMap;
import java.util.Map;

/*
 * This class implements an Intermediate Representation of the read XPack configuration.
 * It does this by capturing all relevant X-Pack concepts.
 * */
public class IR {

    // here are some core fields of the elasticsearch.yml in X-Pack that we need to abstract
    // - feel free to change; Should the Maps be of type Map<String, String>?

    /*
     * Security enabling flags such as
     * xpack.security.enabled
     * */
    private Map<Object, Object> ENABLE_OPTIONS = new HashMap<>();

    /*
     * Cryptographic settings (encryption, certificates, keystores, ...) such as:
     * xpack.security.transport.*, xpack.security.http.*
     * this section could be nested
     * */
    private Map<Object, Object> SSL_TLS_OPTIONS = new HashMap<>();

    /*
     * The authentication settings (users, passwords, realms, ...) such as:
     * xpack.security.authc.*
     * */
    private Map<Object, Object> AUTHENT_OPTIONS = new HashMap<>();

}