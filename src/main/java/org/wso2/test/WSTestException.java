package org.wso2.test;

public class WSTestException extends RuntimeException {
    private static final long serialVersionUID = -7244032125641596311L;

    public WSTestException(String string) {
        super(string);
    }

    public WSTestException(String msg, Throwable e) {
        super(msg, e);
    }

    public WSTestException(Throwable t) {
        super(t);
    }

}
