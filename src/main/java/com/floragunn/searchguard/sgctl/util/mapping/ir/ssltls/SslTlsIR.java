package com.floragunn.searchguard.sgctl.util.mapping.ir.ssltls;

import com.floragunn.searchguard.sgctl.util.mapping.ir.ssltls.tls.*;

public class SslTlsIR {

    boolean httpEnabled;
    boolean transportEnabled;

    Tls transport;
    Tls http;
}
