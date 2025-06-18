package org.axion.axion_api_gateway;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UserDTO {

    private String userId;
    private String email;
    private String role;

    public UserDTO() {
    }

    @JsonProperty("userId")
    public String getUserId() {
        return userId;
    }

    @JsonProperty("email")
    public String getEmail() {
        return email;
    }

    @JsonProperty("role")
    public String getRole() {
        return role;
    }
}
