package org.axion.axion_api_gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.axion.axion_api_gateway.config.AppConfig;
import org.axion.axion_api_gateway.config.ConfigNotFoundException;
import org.axion.axion_api_gateway.config.ServiceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

public class UserSession {

    private final AppConfig appConfig;

    public UserSession(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    private static final Logger log = LoggerFactory.getLogger(UserSession.class);

    public UserDTO getLoggedInUser(HttpServletRequest request) {
        String cookieHeader = request.getHeader("Cookie");
        if (cookieHeader == null) {
            throw new RequestValidationException("No cookie header", HttpStatus.UNAUTHORIZED);
        }
        String[] cookiesArray = cookieHeader.split(";");
        HashMap<String, String> cookieMap = new HashMap<>();
        Arrays.stream(cookiesArray).forEach(cookie -> {
            String[] cookiePair = cookie.split("=");
            cookieMap.put(cookiePair[0], cookiePair[1]);
        });
        UserDTO user = fetchUserFromAuthService(cookieMap.get("token"));
        if (user == null) {
            throw new RequestValidationException("Invalid user ID", HttpStatus.UNAUTHORIZED);
        }
        return user;
    }

    private UserDTO fetchUserFromAuthService(String token) {
        OkHttpClient client = new OkHttpClient();
        ServiceConfig authServiceConfig;
        try {
            authServiceConfig = appConfig.getServiceConfig("axion-auth-service");
        } catch (ConfigNotFoundException e) {
            throw new RuntimeException("Cannot load auth config", e);
        }
        Request request = new Request.Builder()
                .url("http://localhost:" + authServiceConfig.port() + "/users/logged-in")
                .get()
                .header("Authorization", "Bearer " + token)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                log.info("Validation successful for user ID: {}", token);
                return parseUserDTO(response.body().string());
            }
            if (response.code() == 401) {
                throw new RequestValidationException("Unauthorized: Invalid token", HttpStatus.UNAUTHORIZED);
            }
            if (response.code() == 403) {
                throw new RequestValidationException("Forbidden: Access denied", HttpStatus.FORBIDDEN);
            }
            log.error("Unexpected response from auth service: {}. Message: {}", response.code(), response.message());
            throw new IllegalStateException("Unexpected response code: " + response.code());
        } catch (IOException e) {
            log.error("Error while sending request to auth-service", e);
            throw new RequestValidationException("Internal Server Error", HttpStatus.SERVER_ERROR);
        }
    }

    private UserDTO parseUserDTO(String responseBody) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(responseBody, UserDTO.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse UserDTO from response body: {}", responseBody, e);
            throw new RequestValidationException("Failed to parse user data", HttpStatus.SERVER_ERROR);
        }
    }
}
