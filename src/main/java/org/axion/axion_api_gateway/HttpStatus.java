package org.axion.axion_api_gateway;

public enum HttpStatus {

    UNAUTHENTICATED(401),
    BAD_REQUEST(400),
    SERVER_ERROR(500);

    private final int statusCode;

    HttpStatus(int statusCode) {
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
