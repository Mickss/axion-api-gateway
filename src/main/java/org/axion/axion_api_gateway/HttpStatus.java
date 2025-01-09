package org.axion.axion_api_gateway;

public enum HttpStatus {

    BAD_REQUEST(400),
    FORBIDDEN(403),
    SERVER_ERROR(500),
    UNAUTHORIZED(401);

    private final int statusCode;

    HttpStatus(int statusCode) {
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
