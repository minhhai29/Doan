package PServer;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import database.JDBCUtil;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.awt.event.ActionEvent;
import java.sql.ResultSet;
import javax.swing.JCheckBox;
public class addquestion extends JDialog {

	private final JPanel contentPanel = new JPanel();
	private JTextField textField_1;
	private JTextField textField_2;
	private JTextField textField_3;
	private JTextField textField_4;
	private JTextField textField_5;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			addquestion dialog = new addquestion();
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create the dialog.
	 */
	public addquestion() {
		setBounds(100, 100, 450, 300);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(null);
		
		JLabel lblNewLabel_1 = new JLabel("Câu hỏi");
		lblNewLabel_1.setBounds(40, 52, 87, 23);
		contentPanel.add(lblNewLabel_1);
		
		JLabel lblNewLabel_2 = new JLabel("Đáp án");
		lblNewLabel_2.setBounds(40, 142, 50, 23);
		contentPanel.add(lblNewLabel_2);
		
		textField_1 = new JTextField();
		textField_1.setBounds(137, 52, 247, 80);
		contentPanel.add(textField_1);
		textField_1.setColumns(10);
		
		JLabel lblNewLabel_3 = new JLabel("A");
		lblNewLabel_3.setBounds(116, 146, 18, 14);
		contentPanel.add(lblNewLabel_3);
		
		textField_2 = new JTextField();
		textField_2.setBounds(137, 143, 86, 20);
		contentPanel.add(textField_2);
		textField_2.setColumns(10);
		
		JLabel lblNewLabel_4 = new JLabel("C");
		lblNewLabel_4.setBounds(116, 185, 18, 14);
		contentPanel.add(lblNewLabel_4);
		
		textField_3 = new JTextField();
		textField_3.setBounds(137, 182, 86, 20);
		contentPanel.add(textField_3);
		textField_3.setColumns(10);
		
		JLabel lblNewLabel_5 = new JLabel("B");
		lblNewLabel_5.setBounds(258, 146, 18, 14);
		contentPanel.add(lblNewLabel_5);
		
		textField_4 = new JTextField();
		textField_4.setBounds(275, 143, 86, 20);
		contentPanel.add(textField_4);
		textField_4.setColumns(10);
		
		JLabel lblNewLabel_6 = new JLabel("D");
		lblNewLabel_6.setBounds(258, 185, 18, 14);
		contentPanel.add(lblNewLabel_6);
		
		textField_5 = new JTextField();
		textField_5.setBounds(275, 182, 86, 20);
		contentPanel.add(textField_5);
		textField_5.setColumns(10);
		
		JCheckBox checkBoxIsCorrecta = new JCheckBox("");
		checkBoxIsCorrecta.setBounds(226, 142, 26, 23);
		contentPanel.add(checkBoxIsCorrecta);
		
		JCheckBox checkBoxIsCorrectc = new JCheckBox("");
		checkBoxIsCorrectc.setBounds(226, 181, 26, 23);
		contentPanel.add(checkBoxIsCorrectc);
		
		JCheckBox checkBoxIsCorrectb = new JCheckBox("");
		checkBoxIsCorrectb.setBounds(367, 142, 26, 23);
		contentPanel.add(checkBoxIsCorrectb);
		
		JCheckBox checkBoxIsCorrectd = new JCheckBox("");
		checkBoxIsCorrectd.setBounds(367, 181, 26, 23);
		contentPanel.add(checkBoxIsCorrectd);
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton("OK");
				okButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						if (isInputValid()) {
							Connection connection = JDBCUtil.getConnection();
						    PreparedStatement preparedStatementCauHoi = null;
						    PreparedStatement preparedStatementDapAn = null;
						    try {
						        // Chuẩn bị câu truy vấn SQL cho bảng câu hỏi
						        String sqlCauHoi = "INSERT INTO question (qttext) VALUES (?)";
	
						        // Tạo một PreparedStatement cho bảng câu hỏi
						        preparedStatementCauHoi = connection.prepareStatement(sqlCauHoi, PreparedStatement.RETURN_GENERATED_KEYS);
	
						        // Lấy giá trị từ form và thiết lập cho các tham số trong câu truy vấn câu hỏi
						        String questionText = textField_1.getText(); // Giả sử textFieldQuestion là JTextField cho nội dung câu hỏi
						        preparedStatementCauHoi.setString(1, questionText);
						        // Thực hiện câu truy vấn câu hỏi
						        int rowsAffectedCauHoi = preparedStatementCauHoi.executeUpdate();
	
						        // Kiểm tra xem có bao nhiêu dòng đã được ảnh hưởng
						        if (rowsAffectedCauHoi > 0) {
						            System.out.println("Câu hỏi đã được thêm vào CSDL.");
	
						            // Lấy ID của câu hỏi vừa thêm vào
						            ResultSet generatedKeys = preparedStatementCauHoi.getGeneratedKeys();
						            if (generatedKeys.next()) {
						                int questionId = generatedKeys.getInt(1);
	
						                // Chuẩn bị câu truy vấn SQL cho bảng đáp án
						                String sqlDapAn = "INSERT INTO answer (text, qtid, iscorrect) VALUES (?, ?, ?)";
	
						                // Lấy giá trị từ form và thiết lập cho các tham số trong câu truy vấn đáp án
						                String[] answerTexts = {textField_2.getText(), textField_4.getText(), textField_3.getText(), textField_5.getText()};
						                JCheckBox[] checkBoxes = {checkBoxIsCorrecta, checkBoxIsCorrectb, checkBoxIsCorrectc, checkBoxIsCorrectd};
	
						                for (int i = 0; i < answerTexts.length; i++) {
						                    // Tạo một PreparedStatement mới cho mỗi câu truy vấn đáp án
						                    preparedStatementDapAn = connection.prepareStatement(sqlDapAn);
						                    
						                    preparedStatementDapAn.setString(1, answerTexts[i]);
						                    preparedStatementDapAn.setInt(2, questionId);
						                    preparedStatementDapAn.setBoolean(3, checkBoxes[i].isSelected());
	
						                    // Thực hiện câu truy vấn đáp án
						                    int rowsAffectedDapAn = preparedStatementDapAn.executeUpdate();
	
						                    // Kiểm tra xem có bao nhiêu dòng đã được ảnh hưởng
						                    if (rowsAffectedDapAn > 0) {
						                        System.out.println("Đáp án đã được thêm vào CSDL.");
												/*
												 * GUIServer Sframe = new GUIServer(); Sframe.setVisible(true); dispose();
												 */
						                    } else {
						                        System.out.println("Không thể thêm đáp án vào CSDL.");
						                    }
						            }
						        } else {
						            System.out.println("Không thể thêm câu hỏi vào CSDL.");
						        }
						        }
						    } catch (SQLException ex) {
						        ex.printStackTrace();
						    } finally {
						        // Đóng kết nối và các PreparedStatement
						        JDBCUtil.closeConnection(connection);
						    }
						
						}}
				});
				okButton.setActionCommand("OK");
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
			{
				JButton cancelButton = new JButton("Cancel");
				cancelButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						dispose();
					}
				});
				cancelButton.setActionCommand("Cancel");
				buttonPane.add(cancelButton);
			}
		}
	}
	private boolean isInputValid() {
	    String cauhoi = textField_1.getText();
	    String dapan1 = textField_2.getText();
	    String dapan2 = textField_3.getText();
	    String dapan3 = textField_4.getText();
	    String dapan4 = textField_5.getText();
	    // Kiểm tra xem có bất kỳ trường nào bị trống không
	    if (cauhoi.isEmpty() || dapan1.isEmpty()|| dapan2.isEmpty()|| dapan3.isEmpty()|| dapan4.isEmpty()) {
	        JOptionPane.showMessageDialog(null, "Vui lòng nhập đầy đủ thông tin.", "Lỗi", JOptionPane.ERROR_MESSAGE);
	        return false;
	    }

	    return true;
	}
}
