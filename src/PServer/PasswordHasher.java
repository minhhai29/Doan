package PServer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
public class PasswordHasher {

	public static String hashPassword(String password) {
	    try {
	        MessageDigest digest = MessageDigest.getInstance("SHA-256");
	        byte[] encodedHash = digest.digest(password.getBytes());

	        StringBuilder hexString = new StringBuilder();
	        for (byte b : encodedHash) {
	            String hex = Integer.toHexString(0xff & b);
	            if (hex.length() == 1) hexString.append('0');
	            hexString.append(hex);
	        }
	        return hexString.toString();
	    } catch (NoSuchAlgorithmException e) {
	        throw new RuntimeException("Thuật toán SHA-256 không khả dụng", e);
	        // Hoặc xử lý ngoại lệ theo cách khác phù hợp với ứng dụng của bạn
	    }
	}

}
