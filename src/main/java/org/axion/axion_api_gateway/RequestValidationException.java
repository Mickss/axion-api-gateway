package org.axion.axion_api_gateway;

public class RequestValidationException extends RuntimeException {

    private final HttpStatus httpStatus;

    public RequestValidationException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
