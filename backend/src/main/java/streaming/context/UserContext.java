package streaming.context;

public class UserContext {

    private static final ThreadLocal<UserInfo> CONTEXT = new ThreadLocal<>();

    public static void setUser(UserInfo user) {
        CONTEXT.set(user);
    }

    public static UserInfo getUser() {
        return CONTEXT.get();
    }

    public static Long getUserId() {
        UserInfo user = CONTEXT.get();
        return user != null ? user.getUserId() : null;
    }

    public static String getUsername() {
        UserInfo user = CONTEXT.get();
        return user != null ? user.getUsername() : null;
    }

    public static String getRole() {
        UserInfo user = CONTEXT.get();
        return user != null ? user.getRole() : null;
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public static class UserInfo {
        private Long userId;
        private String username;
        private String role;

        public UserInfo() {}

        public UserInfo(Long userId, String username, String role) {
            this.userId = userId;
            this.username = username;
            this.role = role;
        }

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }
}
