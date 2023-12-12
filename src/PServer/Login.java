package PServer;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import database.JDBCUtil;
import encryptions.AESEncryption;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import java.awt.Font;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.awt.event.ActionEvent;
import javax.swing.JPasswordField;

public class Login extends JFrame {

	private JPanel contentPane;
	private JTextField textField;
	private JPasswordField passwordField;
	private static Socket socket;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Login frame = new Login(socket);
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public Login(Socket socket){
		this.socket = socket;
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));

		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		JLabel lblNewLabel = new JLabel("Tên đăng nhập");
		lblNewLabel.setBounds(34, 77, 87, 26);
		contentPane.add(lblNewLabel);
		
		textField = new JTextField();
		textField.setBounds(156, 80, 184, 20);
		contentPane.add(textField);
		textField.setColumns(10);
		
		JLabel lblNewLabel_1 = new JLabel("Mật khẩu");
		lblNewLabel_1.setBounds(34, 114, 74, 20);
		contentPane.add(lblNewLabel_1);
		
		JLabel lblNewLabel_2 = new JLabel("Học viện IQ");
		lblNewLabel_2.setFont(new Font("Tahoma", Font.BOLD, 16));
		lblNewLabel_2.setHorizontalAlignment(SwingConstants.CENTER);
		lblNewLabel_2.setBounds(132, 11, 184, 46);
		contentPane.add(lblNewLabel_2);
		
		JButton btnNewButton = new JButton("Đăng nhập");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (isInputValid()) {
			        String email = textField.getText();
			        String password = passwordField.getText();
			        String hashedPassword = hashPassword(password);
					try {
						BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			            out.write("login\n");
			            out.write(email + "\n");
			            out.write(hashedPassword + "\n");
			            out.flush();

			        } catch (Exception ex) {
			            ex.printStackTrace();
			        }
			}

		}
			});
		btnNewButton.setBounds(103, 169, 109, 23);
		contentPane.add(btnNewButton);
		
		JButton btnNewButton_1 = new JButton("Đăng ký");
		btnNewButton_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(() -> {
					SignUp signupFrame = new SignUp(socket); // Tạo thể hiện của JFrame Signup
	                signupFrame.setVisible(true); // Hiển thị JFrame Signup
	                dispose(); // Đóng cửa sổ của JFrame Login sau khi chuyển qua JFrame Signup
                });
				
			}
		});
		btnNewButton_1.setBounds(240, 169, 100, 23);
		contentPane.add(btnNewButton_1);
		
		passwordField = new JPasswordField();
		passwordField.setBounds(156, 114, 184, 20);
		contentPane.add(passwordField);
	}
	private boolean isInputValid() {
	    String username = textField.getText();
	    String password = new String(passwordField.getPassword());

	    // Kiểm tra xem có bất kỳ trường nào bị trống không
	    if (password.isEmpty() || username.isEmpty()) {
	        JOptionPane.showMessageDialog(null, "Vui lòng nhập đầy đủ thông tin.", "Lỗi", JOptionPane.ERROR_MESSAGE);
	        return false;
	    }

	    return true;
	}
	
	
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
	public String getEmail() {
	    return textField.getText(); // Giả sử textField là JTextField chứa email
	}
	public void resetLoginForm() {
        // Đặt lại thông tin đăng nhập trên giao diện
		textField.setText("");
		passwordField.setText("");
        // Các đặt lại thông tin khác nếu cần
    }
}
