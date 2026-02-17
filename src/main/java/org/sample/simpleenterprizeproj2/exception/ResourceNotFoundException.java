package org.sample.simpleenterprizeproj2.exception;

public class ResourceNotFoundException extends RuntimeException {

    private final String messageCode;
    private final Object[] messageArgs;

    public ResourceNotFoundException(String message) {
        super(message);
        this.messageCode = null;
        this.messageArgs = null;
    }

    public ResourceNotFoundException(String messageCode, Object... args) {
        super(messageCode);
        this.messageCode = messageCode;
        this.messageArgs = args;
    }

    public String getMessageCode() {
        return messageCode;
    }

    public Object[] getMessageArgs() {
        return messageArgs;
    }
}
