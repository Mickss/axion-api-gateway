package org.axion.axion_api_gateway;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UserDTO {

    private String userId;
    private String username;
    private String role;

    public UserDTO() {
    }

    @JsonProperty("userId")
    public String getUserId() {
        return userId;
    }

    @JsonProperty("username")
    public String getUsername() {
        return username;
    }

    @JsonProperty("role")
    public String getRole() {
        return role;
    }
}
