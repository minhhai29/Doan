package PServer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import database.JDBCUtil;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class Online extends JDialog {
    private final JPanel contentPanel = new JPanel();
    private List<String> allUsers;
    private List<String> onlineUsers;

    public Online(List<String> allUsers, List<String> onlineUsers) {
        this.allUsers = allUsers;
        this.onlineUsers = onlineUsers;
        initComponents();
    }

    private void initComponents() {
        setBounds(100, 100, 450, 300);
        getContentPane().setLayout(new BorderLayout());
        contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        getContentPane().add(contentPanel, BorderLayout.CENTER);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane();
        contentPanel.add(scrollPane);

        JPanel panel = new JPanel();
        scrollPane.setViewportView(panel);
        panel.setLayout(new GridLayout(0, 2, 0, 0));

        for (String username : allUsers) {
            JPanel userPanel = new JPanel();
            userPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            JLabel usernameLabel = new JLabel(username);
            JButton blockButton = new JButton("Block");
            blockButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    blockUser(username);
                    // Tự động cập nhật giao diện người dùng nếu cần
                }
            });

            if (onlineUsers.contains(username)) {
                // Hiển thị chấm xanh cho người chơi đang online
                usernameLabel.setForeground(Color.BLUE);
            }

            userPanel.add(usernameLabel);
            userPanel.add(blockButton);
            panel.add(userPanel);
        }

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
        getContentPane().add(buttonPane, BorderLayout.SOUTH);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setActionCommand("Cancel");
        buttonPane.add(cancelButton);
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Xử lý sự kiện Cancel (nếu cần)
                dispose();
            }
        });
    }
    private void blockUser(String username) {
        // Xử lý logic để block người chơi (xóa khỏi cơ sở dữ liệu, cập nhật trạng thái, v.v.)
        // Ở đây, bạn có thể thực hiện một số công việc như xóa khỏi cơ sở dữ liệu
        // Chú ý: Đây chỉ là ví dụ, bạn cần thay đổi dựa trên cấu trúc cơ sở dữ liệu và yêu cầu của ứng dụng của bạn
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = JDBCUtil.getConnection();
            String sql = "DELETE FROM nameid WHERE username = ?";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, username);
            preparedStatement.executeUpdate();
            // Cập nhật lại giao diện hoặc thông báo nếu cần
            System.out.println("User blocked: " + username);
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            JDBCUtil.closeStatement(preparedStatement);
            JDBCUtil.closeConnection(connection);
        }
        // Tự động cập nhật giao diện người dùng nếu cần
        dispose();
    }
    
}
