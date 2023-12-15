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
import PClient.QuizClientFrame;
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
            host = "192.168.32.100";
            socket = new Socket(host, 2911);
            
            System.out.println("Connected to server");
            Login loginFrame = new Login(socket);
            HomePage homePageFrame = new HomePage(socket);
            
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
           
            EventQueue.invokeLater(() -> {
                loginFrame.setVisible(true);
            });
            String messageToSend = "Hello from client!";
            
            out.write(messageToSend  + "\n");
            out.flush();

            StringBuilder serverResponseBuilder = new StringBuilder();
            int character;
            while ((character = in.read()) != -1) {
                if (character == '\n') {
                	String on = "on";
                	String off = "off";
                    // Khi đọc ký tự xuống dòng, kiểm tra và xử lý dữ liệu từ server
                    String serverResponse = serverResponseBuilder.toString().trim();
                    System.out.println("Server response: " + serverResponse);
                    if ("Login successful".equals(serverResponse)) {
                        // Xử lý khi đăng nhập thành công
                        String email = loginFrame.getEmail();
                        updateOnlineStatus(email, on);
                        homePageFrame.setEmail(email);
                        String username = in.readLine();
                        String playerDataString = in.readLine();
                        String[] playerDataArray = playerDataString.split(",");
                        String playerRank = playerDataArray[0];
                        String pointIQ = playerDataArray[1];
                        
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
		                out.write(enteredOTP + "\n");
		                out.flush();  
		            } else if ("Logout successful".equals(serverResponse.trim())) {
		            	String email = homePageFrame.getEmail();
		            	updateOnlineStatus(email, off);
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
	                	homePageFrame.resetStatusLabel();
	                }
	                else if("Cho phép test IQ".equals(serverResponse.trim())) {
	                	String Jsonlist = in.readLine();
	                	System.out.print(Jsonlist);
	                	QuizClientFrame quiz = new QuizClientFrame(socket);
	                	EventQueue.invokeLater(() -> {
	                		quiz.setJson(Jsonlist);
	                		quiz.setVisible(true);
	                    });
	                }else if("Đã test IQ".equals(serverResponse.trim())) {
	                	JOptionPane.showMessageDialog(null, "Bạn đã test IQ trước đó!");
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
    
    public static void updateOnlineStatus(String email, String status) throws Exception {
        try {
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            out.write("updatestatus" + "\n");
            out.write(email + "\n");
            out.write(status + "\n");
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static PublicKey convertBytesToPublicKey(byte[] keyBytes) throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA"); // Thay "RSA" bằng thuật toán sử dụng
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        return keyFactory.generatePublic(keySpec);
    }
	 static class ImageData {
	        private byte[] imageData;
	        private String correctAnswer;

	        public ImageData( byte[] imageData, String correctAnswer)  {
	            this.imageData = imageData;
	            this.correctAnswer = correctAnswer;
	        }
	        public byte[] getImageData() {
	            return imageData;
	        }

	        public String getCorrectAnswer() {
	            return correctAnswer;
	        }
	    }
}