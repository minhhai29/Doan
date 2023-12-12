package PServer;

import java.awt.EventQueue;


import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;


import javax.swing.JLabel;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.util.List;
import java.util.Map;
public class GUIServer extends JFrame {
	private JPanel contentPane;
	private Worker worker;
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    GUIServer frame = new GUIServer();
                    frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
	public GUIServer() {
		worker = new Worker();
		new Thread(worker).start();
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 358, 327);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));

		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		JLabel lblNewLabel = new JLabel("Xin chào Server");
		lblNewLabel.setBounds(25, 11, 126, 30);
		contentPane.add(lblNewLabel);
		
		JButton btnNewButton = new JButton("Thêm câu hỏi vào CSDL");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				addquestion addQtDialog = new addquestion(); // Tạo thể hiện của JFrame Signup
				addQtDialog.setVisible(true); // Hiển thị JFrame Signup
			}
		});
		btnNewButton.setBounds(21, 52, 187, 41);
		contentPane.add(btnNewButton);
		
		JButton btnNewButton_1 = new JButton("Tổng số người chơi");
		btnNewButton_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showOnlineUsers();
			}
		});
		btnNewButton_1.setBounds(21, 104, 187, 38);
		contentPane.add(btnNewButton_1);
		
		JButton btnNewButton_2 = new JButton("Cài đặt trò chơi");
		btnNewButton_2.setBounds(21, 153, 149, 23);
		contentPane.add(btnNewButton_2);
		
		for (Map.Entry<String, Integer> entry : worker.highestTotalMatchPlayer.entrySet()) {
		    String username = entry.getKey();
		    int totalMatch = entry.getValue();

		    JLabel lblNewLabel_1 = new JLabel("Tham gia nhiều nhất: " + username + " số trận đã chơi: " + totalMatch );
			lblNewLabel_1.setBounds(25, 188, 300, 14);
			contentPane.add(lblNewLabel_1);
		}
		
		
		for (Map.Entry<String, Integer> entry : worker.highestPointPlayer.entrySet()) {
			String username = entry.getKey();
		    int point = entry.getValue();
			JLabel lblNewLabel_2 = new JLabel("Người điểm cao nhất:   " + username + " với " + point + " điểm");
			lblNewLabel_2.setBounds(25, 220, 300, 14);
			contentPane.add(lblNewLabel_2);
		}
		
		for (Map.Entry<String, Integer> entry : worker.highestWinStreakPlayer.entrySet()) {
			String username = entry.getKey();
		    int winstreak = entry.getValue();
			JLabel lblNewLabel_3 = new JLabel("Chuỗi thắng dài nhất:   " + username + " với chuỗi "+ winstreak);
			lblNewLabel_3.setBounds(25, 256, 300, 14);
			contentPane.add(lblNewLabel_3);
		}
		
		
		// Khởi động máy chủ trong một luồng riêng biệt
		
    }
	private void showOnlineUsers() {
		worker.updateAllUsers();
		worker.updateOnlineUsers();
        List<String> allUsers = worker.getAllUsers();
        List<String> onlineUsers = worker.getOnlineUsers();

        // Tạo một đối tượng Online và truyền danh sách tất cả người dùng và người dùng online
        Online onlineDialog = new Online(allUsers, onlineUsers);
        onlineDialog.setVisible(true);
    }
    
	    }
	




	