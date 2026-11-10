package streaming.controller;

import cn.hutool.crypto.digest.BCrypt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import streaming.domain.Result;
import streaming.util.JwtUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public Result login(@RequestBody LoginRequest request) {
        try {
            String username = request.getUsername();
            String password = request.getPassword();

            String sql = "SELECT id, username, password, role FROM user WHERE username = ?";
            List<Map<String, Object>> users = jdbcTemplate.queryForList(sql, username);

            if (users.isEmpty()) {
                return Result.error("用户名或密码错误");
            }

            Map<String, Object> user = users.get(0);
            String storedPassword = (String) user.get("password");

            if (!BCrypt.checkpw(password, storedPassword)) {
                return Result.error("用户名或密码错误");
            }

            Long userId = ((Number) user.get("id")).longValue();
            String role = (String) user.get("role");

            String token = jwtUtil.generateToken(userId, username, role);

            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("username", username);
            data.put("role", role);

            return Result.success(data, "登录成功");
        } catch (Exception e) {
            log.error("登录失败: {}", e.getMessage());
            return Result.error("登录失败: " + e.getMessage());
        }
    }

    @GetMapping("/verify")
    public Result verifyToken(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Result.error("未登录");
        }
        String token = authHeader.substring(7);
        if (jwtUtil.validateToken(token)) {
            Map<String, Object> data = new HashMap<>();
            data.put("valid", true);
            return Result.success(data, "Token有效");
        }
        return Result.error("Token无效");
    }

    public static class LoginRequest {
        private String username;
        private String password;
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
