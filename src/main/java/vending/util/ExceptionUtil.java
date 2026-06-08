package vending.util;

import java.io.IOException;

public final class ExceptionUtil {

    private ExceptionUtil() {
    }

    public static VendingException wrap(String message, IOException cause) {
        return new VendingException(message, cause);
    }

    public static VendingException wrap(String message, Exception cause) {
        if (cause instanceof VendingException) {
            return (VendingException) cause;
        }
        if (cause instanceof IOException) {
            return wrap(message, (IOException) cause);
        }
        return new VendingException(message + ": " + cause.getMessage(), cause);
    }
}
