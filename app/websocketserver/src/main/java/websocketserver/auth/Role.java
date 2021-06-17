package websocketserver.auth;

public enum Role {
    PLAYER;

    public String getRole() {
        return "ROLE_" + name();
    }
}
