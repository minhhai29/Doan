package PServer;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import PClient.Client;

import javax.swing.JLabel;

import java.awt.Font;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.awt.event.ActionEvent;
import javax.swing.JDialog;

public class HomePage extends JFrame {
	private static Socket socket;
	private JPanel contentPane;
	private int secondsPassed = 0;
	private Timer timer;
	private JLabel lblNewLabel_5;
	// private MatchmakingServer matchmakingServer;
	private String username; // Biến instance để lưu trữ playerName
	private int playerRank;
	private JLabel lblNewLabel_1;
	private JLabel lblNewLabel_2;
	private JLabel lblNewLabel_3;
	private String email; 
	private int pointIQ;
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					HomePage frame = new HomePage(socket);
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

		JButton btnNewButton = new JButton("Bài test IQ");
		btnNewButton.setBounds(60, 236, 115, 45);
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
					BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		            out.write("testiq\n");
		            out.write(email);
		            out.flush();
		            
		        } catch (Exception ex) {
		            ex.printStackTrace();
		        }
			}
		});

		btnNewButton.setFont(new Font("Tahoma", Font.PLAIN, 12));
		contentPane.add(btnNewButton);

		JButton btnNewButton_1 = new JButton("Tìm trận");
		btnNewButton_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				startTimer();
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
		            out.write("logout\n");
		            out.flush();
		            
		        } catch (Exception ex) {
		            ex.printStackTrace();
		        }

			}
		});
		contentPane.add(btnNewButton_2);

		JLabel lblNewLabel_4 = new JLabel("Nhấn");
		lblNewLabel_4.setBounds(265, 211, 46, 14);
		contentPane.add(lblNewLabel_4);

	}

	private void startTimer() {
		if (lblNewLabel_5 == null) {

			lblNewLabel_5 = new JLabel("Thời gian bắt đầu 60 giây");
			lblNewLabel_5.setFont(new Font("Tahoma", Font.BOLD, 12));
			lblNewLabel_5.setBounds(265, 292, 171, 14);
			contentPane.add(lblNewLabel_5);
		}
		final int totalTime = 60;
		timer = new Timer(1000, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				secondsPassed++;
				int remainingTime = totalTime - secondsPassed;
				lblNewLabel_5.setText("Thời gian bắt đầu " + remainingTime + " giây");
				// sendMatchNotification(playerName, opponent);
				// matchmakingServer.startMatchmaking(playerName);
				if (remainingTime <= 0) {
					timer.stop();
					resetTimer();
				}
			}
		});
		if (!timer.isRunning()) {
			timer.start();
		}
	}

	private void resetTimer() {
		if (timer != null && timer.isRunning()) {
			timer.stop();
		}
		secondsPassed = 0;
		lblNewLabel_5.setText("Thời gian bắt đầu 60 giây");
	}

	public void setUsername(String username) {
		
        this.username = username;
        lblNewLabel_1.setText("Tên người chơi: " + username);
    }
	public void setPlayerRank(int playerRank) {
        this.playerRank = playerRank;
        lblNewLabel_2.setText("Xếp hạng: " + playerRank);
    }
	public void setEmail(String email) {
        this.email = email;
    }
	public String getEmail() {
	    return email; // Giả sử textField là JTextField chứa email
	}
	public void setIQ(int pointIQ) {
		
        this.pointIQ = pointIQ;
        lblNewLabel_3.setText("Điểm IQ: " + pointIQ);
    }
}
