package com.gotree.API.entities.enums;

public enum UserRole {

    ADMIN("ROLE_ADMIN"), USER("ROLE_USER");

    private final String roleName;

    UserRole(String roleName) {
        this.roleName = roleName;
    }

    public String getRoleName() {
        return roleName;
    }

}

