package database;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ImageUploader {

    public static void main(String[] args) {
    	Connection connection = JDBCUtil.getConnection();

        try {
            // Tạo bảng nếu chưa tồn tại
            createTable(connection);
            String imageFolderPath = "C:\\Users\\admin\\eclipse-workspace\\DoAn\\src\\iq";
            // Thêm 20 ảnh vào cơ sở dữ liệu
            for (int i = 1; i <= 20; i++) {
                String imageName =i + ".jpg";
                File imageFile = new File(imageFolderPath, imageName);

                if (imageFile.exists()) {
                    byte[] imageData = readImage(imageFile);
                    saveImageToDatabase(connection, imageData, imageName);
                    System.out.println(imageName + " uploaded successfully.");
                } else {
                    System.out.println(imageName + " not found.");
                }
            }
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }

    private static void createTable(Connection connection) throws SQLException {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS images ("
                + "id INT PRIMARY KEY AUTO_INCREMENT,"
                + "image_data LONGBLOB,"
                + "image_name VARCHAR(255))";

        try (PreparedStatement preparedStatement = connection.prepareStatement(createTableSQL)) {
            preparedStatement.executeUpdate();
        }
    }

    private static byte[] readImage(File file) throws IOException {
        byte[] imageData = new byte[(int) file.length()];
        try (FileInputStream fis = new FileInputStream(file)) {
            fis.read(imageData);
        }
        return imageData;
    }

    private static void saveImageToDatabase(Connection connection, byte[] imageData, String imageName) throws SQLException {
        String insertSQL = "INSERT INTO images (image_data, image_name) VALUES (?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(insertSQL)) {
            preparedStatement.setBytes(1, imageData);
            preparedStatement.setString(2, imageName);
            preparedStatement.executeUpdate();
        }
    }
}
