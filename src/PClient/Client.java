package PClient;

import java.net.*;
import java.awt.EventQueue;
import java.io.*;

import javax.swing.JOptionPane;

import PServer.HomePage;
import PServer.Login;
import PServer.SignUp;

public class Client {
    private static Socket socket;
    private static String host;

    public static void main(String[] args) {
        try {
            host = "192.168.1.3";
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
            out.write(messageToSend + "\n");
            out.flush();

            StringBuilder serverResponseBuilder = new StringBuilder();
            int character;
            while ((character = in.read()) != -1) {
                if (character == '\n') {
                    // Khi đọc ký tự xuống dòng, kiểm tra và xử lý dữ liệu từ server
                    String serverResponse = serverResponseBuilder.toString().trim();
                    System.out.println("Server response: " + serverResponse);
                    if ("Login successful".equals(serverResponse)) {
                        // Xử lý khi đăng nhập thành công
                        String email = loginFrame.getEmail();
                        updateOnlineStatus(email, 1);
                        homePageFrame.setEmail(email);
                        String username = in.readLine();
                        String playerDataString = in.readLine();
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
		                // Gửi mã OTP nhập vào từ người dùng đến server
		                out.write(enteredOTP + "\n");
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
	                } else if ("Incorrect OTP. Registration failed.".equals(serverResponse.trim())) {
	                    JOptionPane.showMessageDialog(null, "Mã OTP không đúng.", "Lỗi", JOptionPane.ERROR_MESSAGE);
	                    
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

    public static void updateOnlineStatus(String email, int status) {
        try {
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            out.write("update_status\n");
            out.write(email + "\n");
            out.write(status + "\n");
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}