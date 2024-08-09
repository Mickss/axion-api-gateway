package org.axion.axion_api_gateway;

import jakarta.servlet.http.HttpServletRequest;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestValidator {

    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private static final Logger log = LoggerFactory.getLogger(RequestValidator.class);

    public void checkRequestViolations(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || header.isEmpty()) {
            throw new RequestValidationException("Authorization header is missing or empty", HttpStatus.BAD_REQUEST);
        }

        String token = header.substring("Bearer ".length());

        boolean isTokenValid = validateTokenWithAuthService(token);
        if (!isTokenValid) {
            throw new RequestValidationException("Invalid token", HttpStatus.UNAUTHENTICATED);
        }
    }

    public String createJson(String token) {
        return "{"
                + "\"token\":\"" + token + "\""
                + "}";
    }

    private boolean validateTokenWithAuthService(String token) {
        OkHttpClient client = new OkHttpClient();


        String json = createJson(token);
        RequestBody body = RequestBody.create(json, JSON);

        Request request = new Request.Builder()
                .url("http://localhost:25003/auth/validate")
                .post(body)
                .build();
        try (Response response = client.newCall(request).execute()) {
            return response.isSuccessful();
        } catch (Exception e) {
            log.error("Error while sending post request to auth-service", e);
            throw new RequestValidationException("Internal Server Error", HttpStatus.SERVER_ERROR);
        }
    }
}
