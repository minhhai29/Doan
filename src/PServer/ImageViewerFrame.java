package PServer;

import javax.swing.*;

import database.JDBCUtil;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ImageViewerFrame extends JFrame {
	private static Socket socket;
    private JLabel imageLabel;
    private JButton nextButton;
    private int currentImageId;
    private ButtonGroup buttonGroup;
    private JRadioButton[] answerButtons;
    private int score;
    private String playerName; 
    public ImageViewerFrame(String playerName,Socket socket) {
    	this.playerName = playerName;
    	this.socket = socket;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(660, 620);

        imageLabel = new JLabel();
        add(imageLabel, BorderLayout.CENTER);

        nextButton = new JButton("Next Image");
        nextButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	checkAnswer();
                showNextImage();
            }
        });
        add(nextButton, BorderLayout.SOUTH);
        buttonGroup = new ButtonGroup();
        answerButtons = new JRadioButton[6];
        for (int i = 0; i < 6; i++) {
            answerButtons[i] = new JRadioButton("Answer " + (i + 1));
            buttonGroup.add(answerButtons[i]);
        }
        JPanel radioButtonPanel = new JPanel();
        for (JRadioButton button : answerButtons) {
            radioButtonPanel.add(button);
        }

        // Add panel to the frame
        add(radioButtonPanel, BorderLayout.NORTH);
        // Initialize the connection and show the first image
        showNextImage();
    }



    private void showNextImage() {
    	Connection connection = JDBCUtil.getConnection();
    	try {
            clearRadioButtons();
            String query = "SELECT image_data, correctanswer FROM images WHERE id = ?";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, currentImageId + 1);

            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                byte[] imageData = resultSet.getBytes("image_data");
                ImageIcon imageIcon = new ImageIcon(imageData);
                imageLabel.setIcon(imageIcon);

                int correctAnswer = resultSet.getInt("correctanswer");
                answerButtons[correctAnswer - 1].setSelected(true); // Select the correct answer

                currentImageId++;
            } else {
            	int result = JOptionPane.showConfirmDialog(this, "Quiz completed! Your score is: " + score ,"End of Quiz", JOptionPane.OK_CANCEL_OPTION);
                
                if (result == JOptionPane.OK_OPTION) {
                    // Close current frame
                    dispose();
                    
                    updateScoreInDatabase();
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error retrieving image from the database", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    private void checkAnswer() {
        for (int i = 0; i < 6; i++) {
            if (answerButtons[i].isSelected()) {
                int selectedAnswer = i + 1;
                if (selectedAnswer == getCorrectAnswer()) {
                    score += 10;
                } 
                return;
            }
        }

        JOptionPane.showMessageDialog(this, "Please select an answer", "Warning", JOptionPane.WARNING_MESSAGE);
    }
    private void updateScoreInDatabase() {
        Connection connection = JDBCUtil.getConnection();
        try {
            String updateQuery = "UPDATE nameid SET pointiq = ? WHERE username = ?";
            PreparedStatement updateStatement = connection.prepareStatement(updateQuery);
            updateStatement.setInt(1, score);
            updateStatement.setString(2, playerName);
            updateStatement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error updating score in the database", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    private int getCorrectAnswer() {
    	Connection connection = JDBCUtil.getConnection();
        try {
            String query = "SELECT correctanswer FROM images WHERE id = ?";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, currentImageId);

            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getInt("correctanswer");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error retrieving correct answer from the database", "Error", JOptionPane.ERROR_MESSAGE);
        }
        
        return -1; // Default value if an error occurs
    }


    private void clearRadioButtons() {
        buttonGroup.clearSelection();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ImageViewerFrame("",socket).setVisible(true);
            }
        });
    }
}
