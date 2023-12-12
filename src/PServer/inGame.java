package PServer;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import database.JDBCUtil;

import javax.swing.ButtonGroup;

public class inGame extends JFrame {

    private JPanel contentPane;
    private ButtonGroup buttonGroup;
    private JTextArea textArea;
    private JRadioButton[] radioButtons;
    private JButton btnNewButton;
    private JLabel lblScore;
    private List<Question> questions;
    private int currentQuestionIndex;
    private int score;
    private Timer timer;
    private int timeRemaining = 20;
    // Connection details for your database
    Connection connection = JDBCUtil.getConnection();

    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    inGame frame = new inGame();
                    frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public inGame() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 450, 300);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        contentPane.setLayout(null);

        lblScore = new JLabel("Điểm của bạn: 0");
        lblScore.setBounds(200, 28, 200, 14);
        contentPane.add(lblScore);

        textArea = new JTextArea();
        textArea.setBounds(58, 59, 312, 57);
        textArea.setLineWrap(true); // Tự động xuống dòng khi văn bản quá dài
        textArea.setEditable(false);
        contentPane.add(textArea);

        buttonGroup = new ButtonGroup();
        radioButtons = new JRadioButton[4];
        for (int i = 0; i < 4; i++) {
            radioButtons[i] = new JRadioButton("Option " + (i + 1));
            radioButtons[i].setBounds(58, 131 + i * 26, 109, 23);
            contentPane.add(radioButtons[i]);
            buttonGroup.add(radioButtons[i]);
        }

        btnNewButton = new JButton("Submit");
        btnNewButton.setBounds(233, 183, 89, 49);
        contentPane.add(btnNewButton);

        btnNewButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                checkAnswer();
                loadNextQuestion();
            }
        });

        // Connect to the database and load questions
        connectAndLoadQuestions();
        // Shuffle the questions for random order
        Collections.shuffle(questions);
        // Load the first question
        loadNextQuestion();

        // Tạo và khởi chạy Timer
        timer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (timeRemaining > 0) {
                    timeRemaining--;
                    lblScore.setText("Điểm của bạn: " + score + " | Thời gian: " + timeRemaining + "s");
                } else {
                    // Xử lý khi thời gian đếm ngược kết thúc
                    timer.stop();
                    textArea.setText("Hết thời gian!");
                    for (JRadioButton radioButton : radioButtons) {
                        radioButton.setEnabled(false);
                    }
                    btnNewButton.setEnabled(false);
                }
            }
        });
        timer.start(); // Bắt đầu đếm ngược
    }

    private void connectAndLoadQuestions() {
        questions = new ArrayList<>();
        try  {
            String query = "SELECT question.id, qttext, answer.id, text, iscorrect FROM question "
                    + "JOIN answer ON question.id = answer.qtid";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    int questionId = resultSet.getInt("question.id");
                    String questionText = resultSet.getString("qttext");
                    int answerId = resultSet.getInt("answer.id");
                    String answerText = resultSet.getString("text");
                    boolean isCorrect = resultSet.getBoolean("iscorrect");
                    Question question = findOrCreateQuestion(questionId, questionText);
                    question.addAnswer(new Answer(answerId, answerText, isCorrect));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Question findOrCreateQuestion(int id, String text) {
        for (Question question : questions) {
            if (question.getId() == id) {
                return question;
            }
        }
        Question newQuestion = new Question(id, text);
        questions.add(newQuestion);
        return newQuestion;
    }

    private void loadNextQuestion() {
        if (currentQuestionIndex < questions.size()) {
            // Reset timeRemaining
            timeRemaining = 20;

            Question question = questions.get(currentQuestionIndex);
            textArea.setText(question.getText());
            List<Answer> answers = question.getAnswers();
            for (int i = 0; i < answers.size() && i < radioButtons.length; i++) {
                radioButtons[i].setText(answers.get(i).getText());
                radioButtons[i].setSelected(false);
            }
            currentQuestionIndex++;
        } else {
            // All questions have been asked
            textArea.setText("Kết thúc trò chơi!");
            for (JRadioButton radioButton : radioButtons) {
                radioButton.setEnabled(false);
            }
            btnNewButton.setEnabled(false);
        }
    }


    private void checkAnswer() {
        Question currentQuestion = questions.get(currentQuestionIndex - 1);
        List<Answer> answers = currentQuestion.getAnswers();
        for (int i = 0; i < answers.size(); i++) {
            if (radioButtons[i].isSelected() && answers.get(i).isCorrect()) {
                score += 10;
                lblScore.setText("Điểm của bạn: " + score);
                break; // Break to avoid adding score multiple times
            }
        }
    }

    private static class Question {
        private int id;
        private String text;
        private List<Answer> answers;

        public Question(int id, String text) {
            this.id = id;
            this.text = text;
            this.answers = new ArrayList<>();
        }

        public int getId() {
            return id;
        }

        public String getText() {
            return text;
        }

        public List<Answer> getAnswers() {
            return answers;
        }

        public void addAnswer(Answer answer) {
            answers.add(answer);
        }
    }

    private static class Answer {
        private int id;
        private String text;
        private boolean isCorrect;

        public Answer(int id, String text, boolean isCorrect) {
            this.id = id;
            this.text = text;
            this.isCorrect = isCorrect;
        }

        public int getId() {
            return id;
        }

        public String getText() {
            return text;
        }

        public boolean isCorrect() {
            return isCorrect;
        }
    }
    
}
