package PClient;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import PClient.Client;
import encryptions.AESEncryption;

import javax.swing.JLabel;

import java.awt.Font;
import javax.swing.SwingConstants;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.awt.event.ActionEvent;
import javax.swing.JDialog;

public class HomePage extends JFrame {
	private static Socket socket;
	private JPanel contentPane;
	private JLabel lblNewLabel_5;
	// private MatchmakingServer matchmakingServer;
	private String username; // Biến instance để lưu trữ playerName
	private String playerRank;
	private JLabel lblNewLabel_1;
	private JLabel lblNewLabel_2;
	private JLabel lblNewLabel_3;
	private JButton btnNewButton_1;
	private String email; 
	private String pointIQ;

	/**
	 * Launch the application.
	 */


	/**
	 * Create the frame.
	 */
	public HomePage(Socket socket) {
		this.socket = socket;
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 483, 378);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));

		setContentPane(contentPane);
		contentPane.setLayout(null);

		JLabel lblNewLabel = new JLabel("Học viện IQ");
		lblNewLabel.setBounds(10, 11, 129, 33);
		lblNewLabel.setHorizontalAlignment(SwingConstants.CENTER);
		lblNewLabel.setFont(new Font("Tahoma", Font.BOLD | Font.ITALIC, 16));
		contentPane.add(lblNewLabel);

		lblNewLabel_1 = new JLabel("");
		lblNewLabel_1.setBounds(35, 68, 200, 23);
		lblNewLabel_1.setFont(new Font("Tahoma", Font.BOLD, 12));
		contentPane.add(lblNewLabel_1);

		lblNewLabel_2 = new JLabel("");
		lblNewLabel_2.setBounds(35, 102, 104, 23);
		lblNewLabel_2.setFont(new Font("Tahoma", Font.BOLD, 12));
		contentPane.add(lblNewLabel_2);

		lblNewLabel_3 = new JLabel("");
		lblNewLabel_3.setBounds(35, 136, 104, 23);
		lblNewLabel_3.setFont(new Font("Tahoma", Font.BOLD, 12));
		contentPane.add(lblNewLabel_3);

		lblNewLabel_5 = new JLabel("");
		lblNewLabel_5.setFont(new Font("Tahoma", Font.BOLD, 12));
		lblNewLabel_5.setBounds(265, 292, 171, 14);
		contentPane.add(lblNewLabel_5);
		
		JButton btnNewButton = new JButton("Bài test IQ");
		btnNewButton.setBounds(60, 236, 115, 45);
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
		            out.write("testIQ"+"\n");
		            out.write(email+"\n");
		            out.flush();
		            
		        } catch (Exception ex) {
		            ex.printStackTrace();
		        }
			}
		});

		btnNewButton.setFont(new Font("Tahoma", Font.PLAIN, 12));
		contentPane.add(btnNewButton);

		btnNewButton_1 = new JButton("Tìm trận");
		btnNewButton_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
					lblNewLabel_5.setText("Đang tìm trận...");
		            out.write("matchfind" + "\n");
		            out.write(username +"\n");
		            out.flush();
		            btnNewButton_1.setEnabled(false);
		        } catch (Exception ex) {
		            ex.printStackTrace();
		        }
			}
		});
		btnNewButton_1.setBounds(265, 236, 115, 45);
		btnNewButton_1.setFont(new Font("Tahoma", Font.PLAIN, 12));
		contentPane.add(btnNewButton_1);

		JButton btnNewButton_2 = new JButton("Đăng xuất");
		btnNewButton_2.setBounds(321, 18, 115, 59);
		btnNewButton_2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
		            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
		            out.write("logout" + "\n");
		            out.flush();
		            
		        } catch (Exception ex) {
		            ex.printStackTrace();
		        }

			}
		});
		contentPane.add(btnNewButton_2);


	}

	

	public void setUsername(String username) {
		
        this.username = username;
        lblNewLabel_1.setText("Tên người chơi: " + username);
    }
	public void setPlayerRank(String playerRank) {
        this.playerRank = playerRank;
        lblNewLabel_2.setText("Xếp hạng: " + playerRank);
    }
	public void setEmail(String email) {
        this.email = email;
    }
	public String getEmail() {
	    return email; 
	}
	public void setIQ(String pointIQ) {
		
        this.pointIQ = pointIQ;
        lblNewLabel_3.setText("Điểm IQ: " + pointIQ);
    }
	public void resetStatusLabel() {
        EventQueue.invokeLater(() -> {
            lblNewLabel_5.setText("");
        });
    }
	public void enableMatchButton(boolean enable) {
        EventQueue.invokeLater(() -> {
            btnNewButton_1.setEnabled(enable);
        });
    }
}
