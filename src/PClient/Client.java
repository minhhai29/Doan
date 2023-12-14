package PClient;

import java.net.*;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;
import java.awt.EventQueue;
import java.io.*;
import java.util.ArrayList;
import java.util.Base64;

import javax.crypto.SecretKey;
import javax.swing.JOptionPane;

import PClient.HomePage;
import PClient.ImageViewerFrame;
import PClient.Login;
import PClient.SignUp;
import PClient.inGame;
import encryptions.AESEncryption;
import encryptions.RSAEncryption;

public class Client {
    private static Socket socket;
    private static String host;
    public static SecretKey keyAES;
    private static PublicKey publicKeyFromServer;
    private static PrivateKey myPrivateKey;
    public static void main(String[] args) throws Exception {
        try {
            host = "192.168.254.120";
            socket = new Socket(host, 2911);
            
            System.out.println("Connected to server");
            Login loginFrame = new Login(socket);
            HomePage homePageFrame = new HomePage(socket);
            
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
         // khóa công khai của Server
            String publicKeyStringFromServer = in.readLine();
            publicKeyFromServer = convertBytesToPublicKey(Base64.getDecoder().decode(publicKeyStringFromServer));

            // khóa đối xứng đã được mã hóa bằng khóa công khai do Server chia sẻ
            keyAES = AESEncryption.generateKey();
            String keyAESString = Base64.getEncoder().encodeToString(keyAES.getEncoded());
            String keyAESEncode = RSAEncryption.encrypt(keyAESString, publicKeyFromServer);

            // khóa công khai của Client để cho Server nhận, sau này Server mã hóa khóa đối xứng để gửi qua mạng
            KeyPair keyPair = RSAEncryption.generateKeyPair();
            PublicKey myPublicKey = keyPair.getPublic();
            String myPublicKeyString = Base64.getEncoder().encodeToString(myPublicKey.getEncoded());
            myPrivateKey = keyPair.getPrivate();

            out.write(myPublicKeyString);
            out.newLine();
            out.write(keyAESEncode);
            out.newLine();
            out.flush();
            EventQueue.invokeLater(() -> {
                loginFrame.setVisible(true);
            });
            String messageToSend = "Hello from client!";
            String encryptedMessage = AESEncryption.encrypt(messageToSend, keyAES);
            
            out.write(encryptedMessage + "\n");
            out.flush();

            StringBuilder serverResponseBuilder = new StringBuilder();
            int character;
            while ((character = in.read()) != -1) {
                if (character == '\n') {
                    // Khi đọc ký tự xuống dòng, kiểm tra và xử lý dữ liệu từ server
                    String serverResponse = serverResponseBuilder.toString().trim();
                    String responseAES = AESEncryption.decrypt(serverResponse, keyAES);
                    System.out.println("Server response: " + responseAES);
                    if ("Login successful".equals(responseAES)) {
                        // Xử lý khi đăng nhập thành công
                        String email = loginFrame.getEmail();
                        updateOnlineStatus(email, 1);
                        homePageFrame.setEmail(email);
                        String usernameaes = in.readLine();
                        String playerDataStringaes = in.readLine();
                        String username = AESEncryption.decrypt(usernameaes, keyAES);
                        String playerDataString = AESEncryption.decrypt(playerDataStringaes, keyAES);
                        String[] playerDataArray = playerDataString.split(",");
                        int playerRank = Integer.parseInt(playerDataArray[0]);
                        int pointIQ = Integer.parseInt(playerDataArray[1]);
                        
                        EventQueue.invokeLater(() -> {
                            homePageFrame.setPlayerRank(playerRank);
                            homePageFrame.setUsername(username);
                            homePageFrame.setIQ(pointIQ);
                            homePageFrame.setVisible(true);
                            loginFrame.dispose();
                        });
                        
                    } else if ("Login failed".equals(serverResponse)) {
                        JOptionPane.showMessageDialog(null, "Tên đăng nhập hoặc mật khẩu không đúng!");
                        EventQueue.invokeLater(() -> {
                            loginFrame.setVisible(true);
                        });
                    }else if ("emailexist".equals(serverResponse.trim())) {
		                JOptionPane.showMessageDialog(null, "Email đã tồn tại.", "Lỗi", JOptionPane.ERROR_MESSAGE);
		            } else if ("usernameexist".equals(serverResponse.trim())) {
		                JOptionPane.showMessageDialog(null, "Tên người dùng đã tồn tại.", "Lỗi", JOptionPane.ERROR_MESSAGE);
		            } else if ("send_otp".equals(serverResponse)) {
		                String enteredOTP = JOptionPane.showInputDialog("Nhập mã OTP đã gửi đến email của bạn:");
		                String encryptedMes = AESEncryption.encrypt(enteredOTP, keyAES);
		                out.write(encryptedMes + "\n");
		                out.flush();  
		            } else if ("Logout successful".equals(serverResponse.trim())) {
		            	String email = homePageFrame.getEmail();
		            	updateOnlineStatus(email, 0);
		            	loginFrame.resetLoginForm();
		            	loginFrame.setVisible(true);
		            	homePageFrame.dispose();
		            }else if ("registersuccess".equals(serverResponse.trim())) {
	                    JOptionPane.showMessageDialog(null, "Đăng ký thành công!");
	                    loginFrame.resetLoginForm();
		            	loginFrame.setVisible(true);
	                }else if ("Incorrect OTP. Registration failed.".equals(serverResponse.trim())) {
	                    JOptionPane.showMessageDialog(null, "Mã OTP không đúng.", "Lỗi", JOptionPane.ERROR_MESSAGE);
	                }else if("outWaitingRoom".equals(serverResponse.trim())) {
	                	JOptionPane.showMessageDialog(null, "Hiện tại đang không có ai!!! Bạn sẽ được ra khỏi phòng chờ.");
	                	homePageFrame.resetStatusLabel();
	                	homePageFrame.enableMatchButton(true);
	                }else if("matchfound".equals(serverResponse.trim())) {
	                	JOptionPane.showMessageDialog(null, "Đã tìm thấy đối thủ!");
		                        inGame ingame = new inGame(socket);
			                	ingame.setVisible(true);
	                }

                    // Reset StringBuilder để đọc phản hồi tiếp theo
                    serverResponseBuilder.setLength(0);
                } else {
                    // Nếu không phải là ký tự xuống dòng, thêm vào StringBuilder
                    serverResponseBuilder.append((char) character);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static PublicKey convertBytesToPublicKey(byte[] keyBytes) throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA"); // Thay "RSA" bằng thuật toán sử dụng
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        return keyFactory.generatePublic(keySpec);
    }
    public static void updateOnlineStatus(String email, int status) throws Exception {
        try {
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            String updateMes = AESEncryption.encrypt("updatestatus", keyAES);
            String mailMes = AESEncryption.encrypt(email, keyAES);
            out.write(updateMes + "n");
            out.write(mailMes + "\n");
            out.write(status + "\n");
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}