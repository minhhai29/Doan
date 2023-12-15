package PClient;

import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;
public class QuizClientFrame extends JFrame {

    private int currentQuestionIndex = 0;
    private int score = 0;
    private String Jsonlist;
    private JLabel imageLabel;
    private ButtonGroup buttonGroup;
    private JButton submitButton;
    private JLabel scoreLabel;
    public static Socket socket;
    private int currentImageIndex;
    private static List<ImageData> convertJsonToList(String jsonString) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            // Chuyển đổi từ chuỗi JSON thành List<ImageData>
            List<ImageData> imageDataList = objectMapper.readValue(jsonString, new TypeReference<List<ImageData>>() {});

            return imageDataList;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }
    List<ImageData> imageDataList  =convertJsonToList(Jsonlist); 
    
    public QuizClientFrame(Socket socket) throws IOException, ClassNotFoundException {
    	
		
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
        setSize(400, 300);
        setLocationRelativeTo(null); // Đưa frame vào giữa màn hình

        // Hiển thị ảnh đầu tiên khi mở ứng dụng
        showImageAtIndex(0);
    }

    // Phương thức để hiển thị ảnh tại một vị trí cụ thể trong danh sách
    private void showImageAtIndex(int index) {
        if (index >= 0 && index <imageDataList.size()) {
            ImageData imageData =imageDataList.get(index);
            imageLabel.setIcon(new ImageIcon(imageData.getImageData()));
            currentImageIndex = index;
        }
    }

    // Phương thức để hiển thị ảnh tiếp theo
    private void showNextImage() {
        currentImageIndex++;
        if (currentImageIndex >=imageDataList.size()) {
            currentImageIndex = 0; // Quay lại ảnh đầu tiên nếu đã qua hết danh sách
        }
        showImageAtIndex(currentImageIndex);
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
    public void setJson(String Jsonlist) {
        this.Jsonlist = Jsonlist;
    }

}