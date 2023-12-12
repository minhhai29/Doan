package encryptions;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

public class Hash {
    // Hàm chuyển đổi mảng byte thành chuỗi hex
    private static String bytesToHex(byte[] bytes) {
        Formatter formatter = new Formatter();
        for (byte b : bytes) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

    public static String hashSHA512(String message, String key) throws NoSuchAlgorithmException, InvalidKeyException {
        // Chuyển key và message thành mảng byte
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);

        // Tạo đối tượng SecretKeySpec từ key
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "HmacSHA512");

        // Tạo đối tượng Mac với thuật toán HmacSHA512
        Mac mac = Mac.getInstance("HmacSHA512");

        // Khởi tạo Mac với key bí mật
        mac.init(secretKeySpec);

        // Tính toán giá trị HMAC-SHA-512
        byte[] hashBytes = mac.doFinal(messageBytes);

        // Chuyển giá trị băm thành chuỗi hex
        String hashHex = bytesToHex(hashBytes);

        return hashHex;
    }

    public static String hashSHA256(String input) throws NoSuchAlgorithmException {
        // Chuyển chuỗi thành mảng byte
        byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);

        // Tạo đối tượng MessageDigest với thuật toán SHA-256
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");

        // Tính toán giá trị băm
        byte[] hashBytes = sha256.digest(inputBytes);

        // Chuyển giá trị băm thành chuỗi hex
        String hashHex = bytesToHex(hashBytes);

        return hashHex;
    }
}