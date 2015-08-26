package edu.nyu.classes.groupersync;

class UserWithRole {
    private String username;
    private String role;

    public UserWithRole(String username, String role) {
        this.username = username;
        this.role = normalizeRole(role);
    }

    public String getUsername() {
        return username;
    }

    public String toString() {
        return String.format("#<%s [%s]>", username, role);
    }

    public String hashKey() {
        return username + "_" + role;
    }

    public int hashCode() {
        return hashKey().hashCode();
    }

    public boolean equals(Object other) {
        if (!(other instanceof UserWithRole)) {
            return false;
        }

        return ((UserWithRole) other).hashKey().equals(hashKey());
    }

    private String normalizeRole(String role) {
        if (role != null && role.toLowerCase().startsWith("i")) {
            return "manager";
        } else {
            return "viewer";
        }
    }
}
