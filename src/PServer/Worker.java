package PServer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.JOptionPane;
import database.JDBCUtil;
import encryptions.AESEncryption;
import encryptions.RSAEncryption;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;

public class Worker implements Runnable {
	private static final List<ClientHandler> clients = new ArrayList<>();
	private static final List<String> waitingRoom = new ArrayList<>();
	// private List<Question> questions;
	Map<String, Integer> highestPointPlayer = getPlayerWithHighestPoint();
	Map<String, Integer> highestWinStreakPlayer = getPlayerWithHighestWinStreak();
	Map<String, Integer> highestTotalMatchPlayer = getPlayerWithHighestTotalMatch();
	private static Map<String, ClientHandler> pairedClients = new HashMap<>();
	private List<String> onlineUsers = new ArrayList<>();
	private List<String> allUsers = new ArrayList<>();
	private ScheduledExecutorService executorService;
	private static Worker instance;
	private String generatedOTP;
	private Socket clientSocket;
	private BufferedWriter out;
	private String userEmail;
	private int currentQuestionIndex;
	public SecretKey keyAES;
	private PublicKey publicKeyFromClient;
	private PrivateKey myPrivateKey;

//	private static PublicKey convertBytesToPublicKey(byte[] keyBytes) throws Exception {
//		KeyFactory keyFactory = KeyFactory.getInstance("RSA"); // Thay "RSA" bằng thuật toán sử dụng
//		X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
//		return keyFactory.generatePublic(keySpec);
//	}

	public void run() {
		try (ServerSocket serverSocket = new ServerSocket(2911)) {
			System.out.println("Server is waiting for connections...");

			while (true) {
				Socket clientSocket = serverSocket.accept();
				System.out.println("Client connected from: " + clientSocket.getInetAddress().getHostAddress());

				ClientHandler clientHandler = new ClientHandler(clientSocket);
				clients.add(clientHandler);

				new Thread(clientHandler).start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setUserEmail(String userEmail) {
		this.userEmail = userEmail;
	}

	public String getUserEmail() {
		return userEmail;
	}

	public static Worker getInstance() {
		if (instance == null) {
			instance = new Worker();
		}
		return instance;
	}

	private class ClientHandler implements Runnable {
		private Socket clientSocket;
		private BufferedReader in;
		private BufferedWriter out;

		public ClientHandler(Socket clientSocket) throws Exception {
			this.clientSocket = clientSocket;
			try {
				// Mở luồng vào/ra cho client
				in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
				// khóa công khai của Server để cho Client nhận
//				KeyPair keyPair = RSAEncryption.generateKeyPair();
//				PublicKey publicKey = keyPair.getPublic();
//				String myPublicKeyString = Base64.getEncoder().encodeToString(publicKey.getEncoded());
//				myPrivateKey = keyPair.getPrivate();

				

				// khóa công khai của Client
				//String publicKeyStringFromClient = in.readLine();
				//publicKeyFromClient = convertBytesToPublicKey(Base64.getDecoder().decode(publicKeyStringFromClient));

				// lấy khóa đối xứng bị mã hóa, giải mã khóa đối xứng dùng khóa riêng tư của
				// Server, ta được khóa đối xứng
//				String keyAESEncodeStringFromClient = in.readLine();
//				String keyAESString = RSAEncryption.decrypt(keyAESEncodeStringFromClient, myPrivateKey);
//				keyAES = new SecretKeySpec(Base64.getDecoder().decode(keyAESString), "AES");

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			try {
				while (true) {
					// Đọc dữ liệu từ client
					String requestType = in.readLine();
					System.out.println("Client gửi: " + requestType);
					if (requestType != null) {
						switch (requestType) {
						case "login":
							handleLogin();
							break;
						case "register":
							handleRegister();
							break;
						case "updatestatus":
							handleUpdateStatus();
							break;
						case "logout":
							handleLogout();
							break;
						case "testIQ":
							handletestIQ();
							break;
						case "matchfind":
							handleMatchFind();
							break;
						default:
							// Xử lý yêu cầu không được hiểu
							break;
						}
					}
				}
			} catch (SocketException e) {
				// Xử lý ngoại lệ socket (client đã ngắt kết nối)
				System.out.println("Client đã ngắt kết nối: " + clientSocket.getInetAddress().getHostAddress());
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				// Khi client thoát, loại bỏ khỏi danh sách và đóng kết nối
				clients.remove(this);
				closeConnection();
			}
		}

		private void closeConnection() {
			try {
				if (in != null) {
					in.close();
				}
				if (out != null) {
					out.close();
				}
				if (clientSocket != null && !clientSocket.isClosed()) {
					clientSocket.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				clients.remove(this);
			}
		}

		private void handleLogin() throws Exception {
			try {

				String email = in.readLine();
				String hashedPassword = in.readLine();
				setUserEmail(email);
				if (validateLogin(email, hashedPassword)) {
					out.write("Login successful" + "\n");

					String username = getUsernameByEmail(email);
					int pointIQ = getIqPoints(email);
					int playerRank = displayRanking(username);
					String playerData = playerRank + "," + pointIQ + "\n";
					out.write(username + "\n");
					out.write(playerData);
					out.flush();
				} else {
					out.write("Login failed" + "\n");
					out.flush();
					return; // Break the loop if login fails
				}
			} catch (SQLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private void handleRegister() throws Exception {
			try {
				String email = in.readLine();
				String hashedPassword = in.readLine();
				String username = in.readLine();
				String gender = in.readLine();
				boolean emailExists = isEmailExists(email);
				boolean usernameExists = isUsernameExists(username);

				if (emailExists) {
					out.write("emailexist" + "\n");
					out.flush();
				} else if (usernameExists) {
					out.write("usernameexist" + "\n");
					out.flush();
				} else {
					String otp = generateOTP();
					// Gửi mã OTP đến email
					EmailSender.sendEmail(email, otp);
					out.write("send_otp" + "\n");
					out.flush();

					// Đọc mã OTP từ client
					String enteredOTP = in.readLine();
					if (otp.equals(enteredOTP)) {
						// OTP chính xác, thực hiện đăng ký tài khoản
						signUpUser(username, hashedPassword, email, gender);

						// Gửi thông báo đăng ký thành công và ID người dùng
						out.write("registersuccess" + "\n");
						out.flush();
					} else {
						// OTP không chính xác
						out.write("Incorrect OTP. Registration failed." + "\n");
						out.flush();
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private void handleUpdateStatus() throws Exception {
			try {
				String email = in.readLine();
				String status = in.readLine();
				if ("on".equals(status)) {
					updateOnlineStatus(email, 1);
				} else if ("off".equals(status)) {
					updateOnlineStatus(email, 0);
				}

			} catch (IOException | NumberFormatException e) {
				e.printStackTrace();
			}
		}

		private static String convertListToJson(List<ImageData> imageDataList) {
			try {
				ObjectMapper objectMapper = new ObjectMapper();
				return objectMapper.writeValueAsString(imageDataList);
			} catch (JsonProcessingException e) {
				e.printStackTrace();
				return null;
			}
		}

		private void handletestIQ() throws Exception {
			try {
				String email = in.readLine();
				int pointIQ = getIqPoints(email);
				if (pointIQ == 0) {
					out.write("Cho phép test IQ" + "\n");
					List<ImageData> imageDataList = getImageDataFromDatabase();
					String jsonData = convertListToJson(imageDataList);
					out.write(jsonData + "\n");
				} else {
					out.write("Đã test IQ" + "\n");
				}
				out.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private void handleLogout() throws Exception {
			try {
				out.write("Logout successful" + "\n");
				out.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		
		  private void handleMatchFind() throws Exception { 
			  try { 
				  String username = in.readLine(); 
				  addToWaitingRoom(username);
				  out.write("Đang trong phòng đợi. Xin chờ!!!" + "\n");
		  
				  if (waitingRoom.size() >= 2) {
	                    selectPlayersAndStartMatch();
	                }else if (waitingRoom.size() == 1) {
	                	startCountdownThread(username);
	                    }
	                out.flush();
	            } catch (IOException e) {
	                e.printStackTrace();
	            } }
		  
		  
		  private void selectPlayersAndStartMatch() throws Exception {
		        List<String> selectedPlayers;
		        synchronized (waitingRoom) {
		            selectedPlayers = new ArrayList<>(waitingRoom.subList(0, 2));
		            waitingRoom.removeAll(selectedPlayers);
		        }
		        String player1 = selectedPlayers.get(0);
		        String player2 = selectedPlayers.get(1);
		        // Log the selected players to the console
		        System.out.println("Selected players for a match: " + player1 + " and " + player2);
		        notifyMatchStart(player1, player2);
		    }
			private void notifyMatchStart(String player1, String player2) throws Exception {
			    try {
			        // Lấy các thể hiện ClientHandler cho cả hai người chơi
			        ClientHandler handler1 = getClientHandlerByUsername(player1);
			        ClientHandler handler2 = getClientHandlerByUsername(player2);

			        // Thông báo cho client về trận đấu
			        String startgame = AESEncryption.encrypt("MatchStart,", keyAES);
			        handler1.out.write(startgame + player2 + "\n");
			        handler1.out.flush();

			        handler2.out.write(startgame + player1 + "\n");
			        handler2.out.flush();

			        // Bắt đầu một luồng mới để xử lý giao tiếp giữa hai client trong suốt trận đấu
			        Thread matchThread = new Thread(() -> handleMatchCommunication(handler1, handler2));
			        matchThread.start();
			    } catch (IOException e) {
			        e.printStackTrace();
			    }
			}

			private void handleMatchCommunication(ClientHandler handler1, ClientHandler handler2) {
			    try {
			    	handler1.sendQuestionsToClient();
			        handler2.sendQuestionsToClient();
			    } catch (Exception e) {
			        e.printStackTrace();
			    } 
			}

			private ClientHandler getClientHandlerByUsername(String username) {
			    // Duyệt qua danh sách client và tìm thể hiện ClientHandler cho username cụ thể
			    for (ClientHandler clientHandler : clients) {
			        if (clientHandler.getUsername().equals(username)) {
			            return clientHandler;
			        }
			    }
			    return null;
			}
			public void sendQuestionsToClient() throws Exception {
			}
		  private void startCountdownThread(String username) {
		        // Start a new thread for countdown
		        new Thread(() -> {
		            try {
		                // Wait for 1 minute
		                Thread.sleep(60000);

		                // Remove the player from the waiting room
		                waitingRoom.remove(username);

		                // Send a notification to the client
		                String outroom = "outWaitingRoom";
		                out.write(outroom + "\n");
		                out.flush();
		            } catch (InterruptedException | IOException e) {
		                e.printStackTrace();
		            } catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		        }).start();
		    }
		  private void addToWaitingRoom(String username) { 
			  synchronized (waitingRoom) {
		  waitingRoom.add(username); } }
		 


		private String getUsername() {
			String email = getUserEmail();
			String username = getUsernameByEmail(email);
		    return username;
		}
		private void sendJsonDataToClient(List<ImageData> imageDataList) throws IOException {
	        Gson gson = new GsonBuilder().setPrettyPrinting().create();
	        String jsonData = gson.toJson(imageDataList);

	        try  {
	        	ObjectOutputStream objectOut= new ObjectOutputStream(clientSocket.getOutputStream());
	        	objectOut.writeObject(jsonData + "\n");
	        } catch (IOException e) {
		        e.printStackTrace();
		    }
	    }
	}

	private List<String> getOnlineUsersFromDatabase() {
		List<String> onlineUsers = new ArrayList<>();
		Connection connection = JDBCUtil.getConnection();
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;

		try {
			String sql = "SELECT username FROM nameid WHERE isonline = 1";
			preparedStatement = connection.prepareStatement(sql);
			resultSet = preparedStatement.executeQuery();

			while (resultSet.next()) {
				String username = resultSet.getString("username");
				onlineUsers.add(username);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			JDBCUtil.closeResultSet(resultSet);
			JDBCUtil.closeStatement(preparedStatement);
			JDBCUtil.closeConnection(connection);
		}

		return onlineUsers;
	}

	private List<String> getAllUsersFromDatabase() {
		List<String> allUsers = new ArrayList<>();
		Connection connection = JDBCUtil.getConnection();
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;

		try {
			String sql = "SELECT username FROM nameid";
			preparedStatement = connection.prepareStatement(sql);
			resultSet = preparedStatement.executeQuery();

			while (resultSet.next()) {
				String username = resultSet.getString("username");
				allUsers.add(username);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			JDBCUtil.closeResultSet(resultSet);
			JDBCUtil.closeStatement(preparedStatement);
			JDBCUtil.closeConnection(connection);
		}

		return allUsers;
	}

	private Map<String, Integer> getPlayerWithHighestTotalMatch() {
		Map<String, Integer> result = new HashMap<>();
		Connection connection = JDBCUtil.getConnection();
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;

		try {
			String sql = "SELECT username, totalmatch FROM nameid ORDER BY totalmatch DESC LIMIT 1";
			preparedStatement = connection.prepareStatement(sql);
			resultSet = preparedStatement.executeQuery();

			if (resultSet.next()) {
				String username = resultSet.getString("username");
				int totalMatch = resultSet.getInt("totalmatch");
				result.put(username, totalMatch);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			JDBCUtil.closeResultSet(resultSet);
			JDBCUtil.closeStatement(preparedStatement);
			JDBCUtil.closeConnection(connection);
		}

		return result;
	}

	private Map<String, Integer> getPlayerWithHighestPoint() {
		Map<String, Integer> result = new HashMap<>();
		Connection connection = JDBCUtil.getConnection();
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;

		try {
			String sql = "SELECT username, point FROM nameid ORDER BY point DESC LIMIT 1";
			preparedStatement = connection.prepareStatement(sql);
			resultSet = preparedStatement.executeQuery();

			if (resultSet.next()) {
				String username = resultSet.getString("username");
				int totalMatch = resultSet.getInt("point");
				result.put(username, totalMatch);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			JDBCUtil.closeResultSet(resultSet);
			JDBCUtil.closeStatement(preparedStatement);
			JDBCUtil.closeConnection(connection);
		}

		return result;
	}

	private Map<String, Integer> getPlayerWithHighestWinStreak() {
		Map<String, Integer> result = new HashMap<>();
		Connection connection = JDBCUtil.getConnection();
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;

		try {
			String sql = "SELECT username, winstreak FROM nameid ORDER BY winstreak DESC LIMIT 1";
			preparedStatement = connection.prepareStatement(sql);
			resultSet = preparedStatement.executeQuery();

			if (resultSet.next()) {
				String username = resultSet.getString("username");
				int totalMatch = resultSet.getInt("winstreak");
				result.put(username, totalMatch);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			JDBCUtil.closeResultSet(resultSet);
			JDBCUtil.closeStatement(preparedStatement);
			JDBCUtil.closeConnection(connection);
		}

		return result;
	}

	public Worker() {
		// Khởi tạo dữ liệu và đối tượng ở đây
		highestPointPlayer = getPlayerWithHighestPoint();
		highestWinStreakPlayer = getPlayerWithHighestWinStreak();
		highestTotalMatchPlayer = getPlayerWithHighestTotalMatch();
	}

	public List<String> getOnlineUsers() {
		return onlineUsers;
	}

	public List<String> getAllUsers() {
		return allUsers;
	}

	public void updateOnlineUsers() {
		// Thực hiện logic để cập nhật người dùng trực tuyến
		onlineUsers = getOnlineUsersFromDatabase();
	}

	public void updateAllUsers() {
		// Thực hiện logic để cập nhật người dùng trực tuyến
		allUsers = getAllUsersFromDatabase();
	}

	private boolean validateLogin(String email, String hashedPassword) throws SQLException {
		Connection connection = JDBCUtil.getConnection();

		try {
			try (PreparedStatement preparedStatement = connection
					.prepareStatement("SELECT * FROM nameid WHERE email=? AND password=?")) {
				preparedStatement.setString(1, email);
				preparedStatement.setString(2, hashedPassword);

				ResultSet resultSet = preparedStatement.executeQuery();
				return resultSet.next();
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			connection.close();
		}
	}

	public boolean isEmailExists(String email) {
		Connection connection = null;
		try {
			connection = JDBCUtil.getConnection();
			String query = "SELECT COUNT(*) FROM nameid WHERE email=?";
			try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
				preparedStatement.setString(1, email);
				try (ResultSet resultSet = preparedStatement.executeQuery()) {
					if (resultSet.next()) {
						int count = resultSet.getInt(1);
						return count > 0;
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			// Đóng kết nối
			closeConnection(connection);
		}
		return false;
	}

	private String generateOTP() {
		int otpLength = 6;
		StringBuilder otp = new StringBuilder();

		// Sử dụng Random để sinh ngẫu nhiên từ 0-9
		Random random = new Random();
		for (int i = 0; i < otpLength; i++) {
			otp.append(random.nextInt(10));
		}

		return otp.toString();
	}
	// Thêm biến để lưu trữ OTP

	public boolean isUsernameExists(String username) {
		Connection connection = null;
		try {
			connection = JDBCUtil.getConnection();
			String query = "SELECT COUNT(*) FROM nameid WHERE username=?";
			try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
				preparedStatement.setString(1, username);
				try (ResultSet resultSet = preparedStatement.executeQuery()) {
					if (resultSet.next()) {
						int count = resultSet.getInt(1);
						return count > 0;
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			// Đóng kết nối
			closeConnection(connection);
		}
		return false;
	}

	// Hàm để đóng kết nối
	private void closeConnection(Connection connection) {
		try {
			if (connection != null && !connection.isClosed()) {
				connection.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public int signUpUser(String username, String hashedPassword, String email, String gender) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		try {
			// Tạo một PreparedStatement cho bảng nameid
			connection = JDBCUtil.getConnection();
			// Chuẩn bị câu truy vấn SQL cho bảng nameid
			String sql = "INSERT INTO nameid (username, password, email, isonline, sex, point, winstreak, totalmatch, pointiq) VALUES (?, ?, ?, 0, ?, 0, 0, 0, 0)";
			// Tạo 1 PreparedStatement cho bảng nameid
			preparedStatement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
			// Lấy giá trị từ form và thiết lập cho các tham số trong câu truy vấn
			preparedStatement.setString(1, username);
			preparedStatement.setString(2, hashedPassword);
			preparedStatement.setString(3, email);
			preparedStatement.setString(4, gender);

			// Thi hành câu truy vấn chèn và kiểm tra số lượng dòng bị ảnh hưởng
			int rowsAffected = preparedStatement.executeUpdate();

			if (rowsAffected > 0) {
				int generatedKey = getGeneratedKey(preparedStatement);
				return generatedKey;
			} else {
				return -1; // Trường hợp thất bại
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
			return -1; // Trường hợp lỗi
		} finally {
			JDBCUtil.closeStatement(preparedStatement);
			JDBCUtil.closeConnection(connection);
		}
	}

	private int getGeneratedKey(PreparedStatement preparedStatement) throws SQLException {
		int generatedKey = -1;
		// Lấy giá trị khóa chính được tạo tự động (nếu có)
		try (var generatedKeys = preparedStatement.getGeneratedKeys()) {
			if (generatedKeys.next()) {
				generatedKey = generatedKeys.getInt(1);
			}
		}
		return generatedKey;
	}

	private int getIqPoints(String email) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		int iqPoints = 0;

		try {
			connection = JDBCUtil.getConnection();
			String sql = "SELECT pointiq FROM nameid WHERE email=?";
			preparedStatement = connection.prepareStatement(sql);
			preparedStatement.setString(1, email);

			resultSet = preparedStatement.executeQuery();

			if (resultSet.next()) {
				iqPoints = resultSet.getInt("pointiq");
			}

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			JDBCUtil.closeResultSet(resultSet);
			JDBCUtil.closeStatement(preparedStatement);
			JDBCUtil.closeConnection(connection);
		}

		return iqPoints;
	}

	private static void updateOnlineStatus(String email, int status) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;

		try {
			String sql = "UPDATE nameid SET isonline = ? WHERE email = ?";
			connection = JDBCUtil.getConnection();
			preparedStatement = connection.prepareStatement(sql);
			preparedStatement.setInt(1, status);
			preparedStatement.setString(2, email);
			preparedStatement.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			// Đóng tất cả các resource
			JDBCUtil.closeStatement(preparedStatement);
			JDBCUtil.closeConnection(connection);
		}
	}

	private static String getUsernameByEmail(String email) {
		Connection connection = JDBCUtil.getConnection();
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		String username = null;

		try {
			String sql = "SELECT username FROM nameid WHERE email=?";
			preparedStatement = connection.prepareStatement(sql);
			preparedStatement.setString(1, email);
			resultSet = preparedStatement.executeQuery();

			if (resultSet.next()) {
				// Nếu có dữ liệu, lấy giá trị từ cột "username"
				username = resultSet.getString("username");
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
		} finally {
			JDBCUtil.closeResultSet(resultSet);
			JDBCUtil.closeStatement(preparedStatement);
			JDBCUtil.closeConnection(connection);
		}

		return username;
	}

	private int displayRanking(String playerName) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;

		try {
			connection = JDBCUtil.getConnection();
			String sql = "SELECT username, point FROM nameid ORDER BY point DESC";
			preparedStatement = connection.prepareStatement(sql);
			resultSet = preparedStatement.executeQuery();

			int rank = 1;

			while (resultSet.next()) {
				String username = resultSet.getString("username");
				int points = resultSet.getInt("point");

				if (username.equals(playerName)) {
					// Trả về giá trị rank nếu tên người chơi được tìm thấy
					return rank;
				}

				rank++;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			JDBCUtil.closeResultSet(resultSet);
			JDBCUtil.closeStatement(preparedStatement);
			JDBCUtil.closeConnection(connection);
		}

		// Trả về -1 nếu không tìm thấy tên người chơi
		return -1;
	}
	/*
	 * public List<Question> getQuestionsAndAnswers() { List<Question> questions =
	 * new ArrayList<>(); Connection connection = JDBCUtil.getConnection(); try {
	 * String query =
	 * "SELECT question.id, question.text, answer.id , answer.text, answer.iscorrect "
	 * +
	 * "FROM question JOIN answer ON question.id = answer.question_id ORDER BY RAND() LIMIT 5;"
	 * ; try (PreparedStatement statement = connection.prepareStatement(query)) {
	 * ResultSet resultSet = statement.executeQuery(); while (resultSet.next()) {
	 * int questionId = resultSet.getInt("question.id"); String questionText =
	 * resultSet.getString("qttext"); int answerId = resultSet.getInt("answer.id");
	 * String answerText = resultSet.getString("text"); boolean isCorrect =
	 * resultSet.getBoolean("iscorrect"); Question question =
	 * findOrCreateQuestion(questionId, questionText); question.addAnswer(new
	 * Answer(answerId, answerText, isCorrect)); List<Answer> answers = new
	 * ArrayList<>(); do { answers.add(new Answer(answerId, answerText, isCorrect));
	 * } while (resultSet.next() && resultSet.getInt("question.id") == questionId);
	 * } } } catch (SQLException e) { e.printStackTrace(); }
	 * 
	 * return questions; }
	 */

	/*
	 * private Question findOrCreateQuestion(int id, String text) { for (Question
	 * question : questions) { if (question.getId() == id) { return question; } }
	 * Question newQuestion = new Question(id, text); questions.add(newQuestion);
	 * return newQuestion; } private static class Question { private int id; private
	 * String text; private List<Answer> answers;
	 * 
	 * public Question(int id, String text) { this.id = id; this.text = text;
	 * this.answers = new ArrayList<>(); }
	 * 
	 * public int getId() { return id; }
	 * 
	 * public String getText() { return text; }
	 * 
	 * public List<Answer> getAnswers() { return answers; }
	 * 
	 * public void addAnswer(Answer answer) { answers.add(answer); } }
	 */
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
						ImageData image = new ImageData(imageData, correctAnswer);
						imageDataList.add(image);
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return imageDataList;
	}

	private static class ImageData implements Serializable {
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
	}

}