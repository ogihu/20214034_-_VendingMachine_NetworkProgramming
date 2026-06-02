package vending.util;

/**
 * 자판기 프로그램 공통 예외.
 */
public class VendingException extends Exception {

    public VendingException(String message) {
        super(message);
    }

    public VendingException(String message, Throwable cause) {
        super(message, cause);
    }
}
