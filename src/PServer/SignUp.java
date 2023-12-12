package PServer;

import java.awt.EventQueue;
import javax.swing.*;

import java.util.Random;

import javax.swing.border.EmptyBorder;

import database.JDBCUtil;

import java.awt.Font;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.awt.event.ActionEvent;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
public class SignUp extends JFrame {
	private static Socket socket;
 	private JPanel contentPane;
    private JTextField textField;
    private JTextField textField_2;
    private JPasswordField passwordField;
    private final JComboBox<String> comboBoxGender = new JComboBox<>(new String[]{"Nam", "Nữ", "Other"});
    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    SignUp frame = new SignUp(socket);
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
    public SignUp(Socket socket) {
    	this.socket = socket;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 450, 300);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        contentPane.setLayout(null);
	        
		JLabel lblNewLabel = new JLabel("Đăng ký");
		lblNewLabel.setHorizontalAlignment(SwingConstants.CENTER);
		lblNewLabel.setFont(new Font("Tahoma", Font.BOLD, 16));
		lblNewLabel.setBounds(166, 23, 103, 33);
		contentPane.add(lblNewLabel);
		
		JLabel lblNewLabel_1 = new JLabel("Gmail đăng nhập");
		lblNewLabel_1.setBounds(45, 75, 121, 14);
		contentPane.add(lblNewLabel_1);
		
		JLabel lblNewLabel_2 = new JLabel("Mật khẩu");
		lblNewLabel_2.setBounds(45, 107, 121, 14);
		contentPane.add(lblNewLabel_2);
		
		JLabel lblNewLabel_3 = new JLabel("Tên người dùng");
		lblNewLabel_3.setBounds(45, 140, 121, 14);
		contentPane.add(lblNewLabel_3);
		
		JLabel lblNewLabel_4 = new JLabel("Giới tính");
		lblNewLabel_4.setBounds(45, 174, 46, 14);
		contentPane.add(lblNewLabel_4);
		
		textField = new JTextField();
		textField.setBounds(176, 72, 182, 20);
		contentPane.add(textField);
		textField.setColumns(10);
		
		textField_2 = new JTextField();
		textField_2.setBounds(176, 137, 182, 20);
		contentPane.add(textField_2);
		textField_2.setColumns(10);
		
		comboBoxGender.setBounds(176, 170, 100, 20);
	    contentPane.add(comboBoxGender);
		JButton btnNewButton = new JButton("Đăng ký");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(isInputValid()) {
				
						String username = textField_2.getText();
						String password = passwordField.getText();
						String hashedPassword = hashPassword(password);
	
						String email = textField.getText();
						String gender = (String) comboBoxGender.getSelectedItem();
						
						
						try {
							BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
				            out.write("register\n");
				            out.write(email + "\n");
				            out.write(hashedPassword + "\n");
				            out.write(username + "\n");
				            out.write(gender + "\n");
				            out.flush();

		                } catch (Exception ex) {
		                    ex.printStackTrace();
		                }
		        }
		    }
		});
        btnNewButton.setBounds(285, 212, 89, 23);
        contentPane.add(btnNewButton);
		btnNewButton.setBounds(285, 212, 89, 23);
		contentPane.add(btnNewButton);
		
        
		passwordField = new JPasswordField();
		passwordField.setBounds(176, 104, 182, 20);
		contentPane.add(passwordField);
		
		
		JButton btnNewButton_1 = new JButton("Trở lại");
		btnNewButton_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Login loginFrame = new Login(socket); // Tạo thể hiện của JFrame Signup
                loginFrame.setVisible(true); // Hiển thị JFrame Signup
                dispose();
			}
		});
		btnNewButton_1.setBounds(180, 212, 89, 23);
		contentPane.add(btnNewButton_1);
	}
	    private boolean isInputValid() {
		    String email = textField.getText();
		    String password = new String(passwordField.getPassword());
		    String username = textField_2.getText();

		    // Kiểm tra xem có bất kỳ trường nào bị trống không
		    if (email.isEmpty() || password.isEmpty() || username.isEmpty()) {
		        JOptionPane.showMessageDialog(null, "Vui lòng nhập đầy đủ thông tin.", "Lỗi", JOptionPane.ERROR_MESSAGE);
		        return false;
		    }
		    if (!email.matches(".+@gmail\\.com")) {
		        JOptionPane.showMessageDialog(null, "Email phải có định dạng @gmail.com.", "Lỗi", JOptionPane.ERROR_MESSAGE);
		        return false;
		    }

		    // Kiểm tra độ dài mật khẩu (tối thiểu 6 kí tự)
		    if (password.length() < 6) {
		        JOptionPane.showMessageDialog(null, "Mật khẩu phải có ít nhất 6 kí tự.", "Lỗi", JOptionPane.ERROR_MESSAGE);
		        return false;
		    }

		    // Kiểm tra xem mật khẩu có ít nhất một chữ cái và một số không
		    if (!password.matches(".*[a-zA-Z].*") || !password.matches(".*\\d.*")) {
		        JOptionPane.showMessageDialog(null, "Mật khẩu phải chứa ít nhất một chữ cái và một số.", "Lỗi", JOptionPane.ERROR_MESSAGE);
		        return false;
		    }

		    // Kiểm tra định dạng tên người dùng (viết hoa chữ cái đầu)
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
	    // Các đặt lại thông tin khác nếu cần
	    
}
