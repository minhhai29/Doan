package PClient;

import javax.swing.*;


import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.List;

public class inGame extends JFrame {
    private int currentQuestionIndex;
    private int score;
    private List<Question> questionList;

    private JTextArea questionTextArea;
    private JButton[] answerButtons;
    private JLabel scoreLabel;
    public static Socket socket;

    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    ObjectOutputStream objectOut = new ObjectOutputStream(socket.getOutputStream());
                    ObjectInputStream objectIn = new ObjectInputStream(socket.getInputStream());
                    Object receivedObject = objectIn.readObject();
                    if (receivedObject instanceof List) {
                        List<Question> questionList = (List<Question>) receivedObject;
                        inGame frame = new inGame(socket);
                        frame.setVisible(true);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public inGame(Socket socket) {
        this.socket = socket;
        this.currentQuestionIndex = 0;
        this.score = 0;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        questionTextArea = new JTextArea();
        questionTextArea.setEditable(false);
        add(questionTextArea, BorderLayout.CENTER);

        answerButtons = new JButton[4];
        JPanel buttonPanel = new JPanel(new GridLayout(2, 2));
        for (int i = 0; i < 4; i++) {
            answerButtons[i] = new JButton();
            answerButtons[i].addActionListener(new AnswerButtonListener());
            buttonPanel.add(answerButtons[i]);
        }
        add(buttonPanel, BorderLayout.SOUTH);

        scoreLabel = new JLabel("Điểm: 0");
        add(scoreLabel, BorderLayout.NORTH);

        displayQuestion();
        startTimer(); // Bạn cần triển khai một bộ đếm thời gian cho 20 giây
    }

    private void displayQuestion() {
        if (currentQuestionIndex < questionList.size()) {
            Question currentQuestion = questionList.get(currentQuestionIndex);
            questionTextArea.setText(currentQuestion.getText());

            List<Answer> answers = currentQuestion.getAnswers();
            for (int i = 0; i < Math.min(4, answers.size()); i++) {
                answerButtons[i].setText(answers.get(i).getText());
            }
        } else {
            // Hết câu hỏi, hiển thị điểm cuối cùng hoặc thực hiện hành động phù hợp
            JOptionPane.showMessageDialog(this, "Hết trò chơi! Điểm cuối cùng của bạn là: " + score);
            // Bạn có thể đóng cửa sổ hoặc thực hiện hành động khác
            dispose();
        }
    }

    private class AnswerButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            JButton clickedButton = (JButton) e.getSource();
            int selectedAnswerIndex = -1;

            for (int i = 0; i < 4; i++) {
                if (clickedButton == answerButtons[i]) {
                    selectedAnswerIndex = i;
                    break;
                }
            }

            if (selectedAnswerIndex != -1) {
                Answer selectedAnswer = questionList.get(currentQuestionIndex).getAnswers().get(selectedAnswerIndex);
                if (selectedAnswer.isCorrect()) {
                    score += 10;
                }

                currentQuestionIndex++;
                displayQuestion();
            }
        }
    }

    private void startTimer() {
        // Triển khai logic đếm thời gian cho 20 giây
    }

    public class Question implements Serializable {
        private String text;
        private List<Answer> answers;

        public Question(String text, List<Answer> answers) {
            this.text = text;
            this.answers = answers;
        }

        public String getText() {
            return text;
        }

        public List<Answer> getAnswers() {
            return answers;
        }
    }

    public class Answer implements Serializable {
        private String text;
        private boolean isCorrect;

        public Answer(String text, boolean isCorrect) {
            this.text = text;
            this.isCorrect = isCorrect;
        }

        public String getText() {
            return text;
        }

        public boolean isCorrect() {
            return isCorrect;
        }
    }
}
