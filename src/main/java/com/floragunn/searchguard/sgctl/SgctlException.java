package com.floragunn.searchguard.sgctl;

public class SgctlException extends Exception {

    private static final long serialVersionUID = 6181282719824113444L;

    private String debugDetail;

    public SgctlException() {
        super();
    }

    public SgctlException(String message, Throwable cause) {
        super(message, cause);
    }

    public SgctlException(String message) {
        super(message);
    }

    public SgctlException(Throwable cause) {
        super(cause);
    }

    public SgctlException debugDetail(String debugDetail) {
        this.debugDetail = debugDetail;
        return this;
    }

    public String getDebugDetail() {
        return debugDetail;
    }

}
