package vending.admin;

import vending.config.DataPaths;
import vending.util.AppLog;
import vending.util.VendingException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

// 비밀번호기능
public class AdminPasswordStore {

    private static final Pattern RULE = Pattern.compile(
            "^(?=.*[0-9])(?=.*[~!@#$%^&*()+|=]).{8,}$");

    private final Path path = DataPaths.ADMIN.resolve("password.txt");

    public AdminPasswordStore() {
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path.getParent());
                Files.writeString(path, "Admin123!", StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            AppLog.error("ADMIN", "비밀번호 파일 초기화 실패", e);
            throw new RuntimeException("비밀번호 파일 초기화 실패", e);
        }
    }

    public boolean verify(String input) {
        try {
            String saved = Files.readString(path, StandardCharsets.UTF_8).trim();
            return saved.equals(input);
        } catch (IOException e) {
            AppLog.warn("ADMIN", "비밀번호 읽기 실패: " + e.getMessage());
            return false;
        }
    }

    public String changePassword(String newPassword) throws VendingException {
        if (!RULE.matcher(newPassword).matches()) {
            return "특수문자와 숫자를 각각 1개 이상 포함한 8자리 이상이어야 합니다.";
        }
        try {
            Files.writeString(path, newPassword, StandardCharsets.UTF_8);
            return null;
        } catch (IOException e) {
            throw new VendingException("비밀번호 저장 실패", e);
        }
    }
}
