package com.floragunn.searchguard.sgctl.util.mapping.ir;

public class SslTlsIR {

    public Tls transport;
    public Tls http;

    public SslTlsIR() {
        transport = new Tls();
        http = new Tls();
    }
}
