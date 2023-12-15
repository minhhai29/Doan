package PServer;

import javax.swing.*;

import database.JDBCUtil;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ImageDataViewer extends JFrame {

    private JLabel imageLabel;
    private int currentImageIndex;

    public ImageDataViewer(List<ImageData> imageDataList) {
        setTitle("Image Viewer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Tạo JLabel để hiển thị ảnh
        imageLabel = new JLabel();
        add(imageLabel, BorderLayout.CENTER);

        // Tạo JButton để chuyển đến ảnh tiếp theo
        JButton nextButton = new JButton("Next");
        nextButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showNextImage();
            }
        });

        // Thêm JButton vào frame
        add(nextButton, BorderLayout.SOUTH);

        // Thiết lập kích thước của frame
        setSize(650, 660);
        setLocationRelativeTo(null); // Đưa frame vào giữa màn hình

        // Hiển thị ảnh đầu tiên khi mở ứng dụng
        showImageAtIndex(0);
    }

    // Phương thức để hiển thị ảnh tại một vị trí cụ thể trong danh sách
    private void showImageAtIndex(int index) {
        if (index >= 0 && index < getImageDataFromDatabase().size()) {
            ImageData imageData =getImageDataFromDatabase().get(index);
            imageLabel.setIcon(new ImageIcon(imageData.getImageData()));
            currentImageIndex = index;
        }
    }

    // Phương thức để hiển thị ảnh tiếp theo
    private void showNextImage() {
        currentImageIndex++;
        if (currentImageIndex >=getImageDataFromDatabase().size()) {
            currentImageIndex = 0; // Quay lại ảnh đầu tiên nếu đã qua hết danh sách
        }
        showImageAtIndex(currentImageIndex);
    }

    public static void main(String[] args) {
        // Tạo và hiển thị frame với danh sách ảnh
        SwingUtilities.invokeLater(() -> {
            List<ImageData> imageDataList =getImageDataFromDatabase();
            ImageDataViewer imageFrame = new ImageDataViewer(imageDataList);
            imageFrame.setVisible(true);
        });
    }

    // Class ImageData như đã đề cập trong câu hỏi
    private static class ImageData {
        private byte[] imageData;
        private String correctAnswer;

        public ImageData(byte[] imageData, String correctAnswer) {
            this.imageData = imageData;
            this.correctAnswer = correctAnswer;
        }

        public byte[] getImageData() {
            return imageData;
        }

        public String getCorrectAnswer() {
            return correctAnswer;
        }

        @Override
        public String toString() {
            return correctAnswer;
        }
    }

    // Class truy vấn cơ sở dữ liệu
	private static List<ImageData> getImageDataFromDatabase() {
        List<ImageData> imageDataList = new ArrayList<>();

        try (Connection connection = JDBCUtil.getConnection()) {
            String query = "SELECT image_data, correctanswer FROM images";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        byte[] imageData = resultSet.getBytes("image_data");
                        String correctAnswer = resultSet.getString("correctanswer");
                        // Tạo đối tượng ImageData và thêm vào danh sách
                        ImageData image = new ImageData( imageData, correctAnswer);
                        imageDataList.add(image);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return imageDataList;
    }
}
