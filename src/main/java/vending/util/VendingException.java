package vending.util;

public class VendingException extends Exception {

    public VendingException(String message) {
        super(message);
    }

    public VendingException(String message, Throwable cause) {
        super(message, cause);
    }
}
