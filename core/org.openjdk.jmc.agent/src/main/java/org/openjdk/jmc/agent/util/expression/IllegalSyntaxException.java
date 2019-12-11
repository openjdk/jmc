package org.openjdk.jmc.agent.util.expression;

public class IllegalSyntaxException extends Exception {
    public IllegalSyntaxException() {
        super();
    }

    public IllegalSyntaxException(String message) {
        super(message);
    }

    public IllegalSyntaxException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalSyntaxException(Throwable cause) {
        super(cause);
    }
}
