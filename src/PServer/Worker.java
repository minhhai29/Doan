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
public class Worker implements Runnable {
    private static final int TIMEOUT_MINUTES = 10;
    private static final List<ClientHandler> clients = new ArrayList<>();
    private static final List<String> waitingRoom = new ArrayList<>();
    private List<Question> questions;
	Map<String, Integer> highestPointPlayer = getPlayerWithHighestPoint();
	Map<String, Integer> highestWinStreakPlayer = getPlayerWithHighestWinStreak();
	Map<String, Integer> highestTotalMatchPlayer = getPlayerWithHighestTotalMatch();
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
    
    private static PublicKey convertBytesToPublicKey(byte[] keyBytes) throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA"); // Thay "RSA" bằng thuật toán sử dụng
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        return keyFactory.generatePublic(keySpec);
    }

	public void run() {
		try (ServerSocket serverSocket = new ServerSocket(2911)) {
			
            executorService = Executors.newScheduledThreadPool(1);
            executorService.scheduleAtFixedRate(() -> checkAndClose(serverSocket), 0, 1, TimeUnit.MINUTES);

            System.out.println("Server is waiting for connections...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected from: " + clientSocket.getInetAddress().getHostAddress());

                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                
                new Thread(clientHandler).start();
            }
		} catch (SocketTimeoutException e) {
            System.out.println("No connection within " + TIMEOUT_MINUTES + " minutes. Closing the server.");
            closeServerGracefully();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdown();
            }
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
	private void closeServerGracefully() {
        try {
            for (ClientHandler client : clients) {
                client.closeConnection();
            }

            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    
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
	            out= new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
	         // khóa công khai của Server để cho Client nhận
	            KeyPair keyPair = RSAEncryption.generateKeyPair();
	            PublicKey publicKey = keyPair.getPublic();
	            String myPublicKeyString = Base64.getEncoder().encodeToString(publicKey.getEncoded());
	            myPrivateKey = keyPair.getPrivate();

	            out.write(myPublicKeyString);
	            out.newLine();
	            out.flush();
	            
	         // khóa công khai của Client
	            String publicKeyStringFromClient = in.readLine();
	            publicKeyFromClient = convertBytesToPublicKey(Base64.getDecoder().decode(publicKeyStringFromClient));

	            // lấy khóa đối xứng bị mã hóa, giải mã khóa đối xứng dùng khóa riêng tư của Server, ta được khóa đối xứng
	            String keyAESEncodeStringFromClient = in.readLine();
	            String keyAESString = RSAEncryption.decrypt(keyAESEncodeStringFromClient, myPrivateKey);
	            keyAES = new SecretKeySpec(Base64.getDecoder().decode(keyAESString), "AES");
	            
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
	            	String requestAES = AESEncryption.decrypt(requestType, keyAES);
	            	System.out.println("Client gửi: " + requestAES);
	            	if (requestAES != null) {
	                    switch (requestAES) {
	                    	case "Hello from client!":
	                    		
	                            break;
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
	        	
	            String emailAES = in.readLine();
	            String hashedPasswordAES = in.readLine();
	            String email = AESEncryption.decrypt(emailAES, keyAES);
	            String hashedPassword = AESEncryption.decrypt(hashedPasswordAES, keyAES);
	            setUserEmail(email);
	            if (validateLogin(email, hashedPassword)) {
	            	String loginsuccessMes = AESEncryption.encrypt("Login successful", keyAES);
	                out.write(loginsuccessMes + "\n");

	                String username = getUsernameByEmail(email);
	                int pointIQ = getIqPoints(email);
	                int playerRank = displayRanking(username);
	                String playerData = playerRank + "," + pointIQ + "\n";
	                String usernameMes = AESEncryption.encrypt(username, keyAES);
	                String playerDataMes = AESEncryption.encrypt(playerData, keyAES);
	                out.write(usernameMes + "\n");
	                out.write(playerDataMes);
	                out.flush();
	            } else {
	            	String loginfailMes = AESEncryption.encrypt("Login failed", keyAES);
	                out.write(loginfailMes + "\n");
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
	            String emailaes = in.readLine();
	            String hashedPasswordaes = in.readLine();
	            String usernameaes = in.readLine();
	            String genderaes = in.readLine();
	            String email = AESEncryption.decrypt(emailaes, keyAES);
	            String hashedPassword = AESEncryption.decrypt(hashedPasswordaes, keyAES);
	            String username = AESEncryption.decrypt(usernameaes, keyAES);
	            String gender = AESEncryption.decrypt(genderaes, keyAES);
	            boolean emailExists = isEmailExists(email);
	            boolean usernameExists = isUsernameExists(username);

	            if (emailExists) {
	            	String emailexistMes = AESEncryption.encrypt("emailexist", keyAES);
	                out.write(emailexistMes + "\n");
	                out.flush();
	            } else if (usernameExists) {
	            	String usernameexistMes = AESEncryption.encrypt("usernameexist", keyAES);
	                out.write(usernameexistMes + "\n");
	                out.flush();
	            } else {
	                String otpaes = generateOTP();
	                String otp = AESEncryption.decrypt(otpaes, keyAES);
	                // Gửi mã OTP đến email
	                EmailSender.sendEmail(email, otp);
	                String sendotpMes = AESEncryption.encrypt("send_otp", keyAES);
	                out.write(sendotpMes + "\n");
	                out.flush();

	                // Đọc mã OTP từ client
	                String enteredOTPaes = in.readLine();
	                String enteredOTP = AESEncryption.decrypt(enteredOTPaes, keyAES);
	                if (otp.equals(enteredOTP)) {
	                    // OTP chính xác, thực hiện đăng ký tài khoản
	                    signUpUser(username, hashedPassword, email, gender);

	                    // Gửi thông báo đăng ký thành công và ID người dùng
	                    String registersuccessMes = AESEncryption.encrypt("registersuccess", keyAES);
	                    out.write(registersuccessMes + "\n");
	                    out.flush();
	                } else {
	                    // OTP không chính xác
	                	String registerfailMes = AESEncryption.encrypt("Incorrect OTP. Registration failed.", keyAES);
	                    out.write(registerfailMes + "\n");
	                    out.flush();
	                }
	            }
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	    }


	    private void handleUpdateStatus() throws Exception {
	        try {
	            String emailMes = in.readLine();
	            int status = Integer.parseInt(in.readLine());
	            String email = AESEncryption.decrypt(emailMes, keyAES);
	            updateOnlineStatus(email, status);
	        } catch (IOException | NumberFormatException e) {
	            e.printStackTrace();
	        }
	    }
	    private void handletestIQ() throws Exception {
	        try {
	            String emailMes = in.readLine();
	            String email = AESEncryption.decrypt(emailMes, keyAES);
	            int pointIQ = getIqPoints(email);
	            if(pointIQ == 0) {
	            	String iqok = AESEncryption.encrypt("Cho phép test IQ", keyAES);
	            	out.write(iqok + "\n");
	            }else {
	            	String iqnook = AESEncryption.encrypt("Đã test IQ", keyAES);
	            	out.write(iqnook + "\n");
	            }
	            out.flush();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	    }
	    private void handleLogout() throws Exception {
	        try {
	        	String logoutsuccess = AESEncryption.encrypt("Logout successful", keyAES);
	        	out.write(logoutsuccess + "\n");
	            out.flush();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	    }
	    private void handleMatchFind() throws Exception {
            try {
                String usernameMes = in.readLine();
                String username = AESEncryption.decrypt(usernameMes, keyAES);
                addToWaitingRoom(username);
                String inroom = AESEncryption.encrypt("Đang trong phòng đợi. Xin chờ!!!", keyAES);
                out.write(inroom + "\n");
                
                if (waitingRoom.size() >= 2) {
                    selectPlayersAndStartMatch();
                }else if (waitingRoom.size() == 1) {
                	startCountdownThread(username);
                    }
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
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
	                String outroom = AESEncryption.encrypt("outWaitingRoom", keyAES);
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
	            waitingRoom.add(username);
	        }
	    }
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
		    try {
                ObjectOutputStream objectOut= new ObjectOutputStream(clientSocket.getOutputStream());
		        List<Question> questions = getQuestionsAndAnswers();
		        objectOut.writeObject(questions);
		        out.flush();
		    } catch (IOException e) {
		        e.printStackTrace();
		    }
		}
		
		private String getUsername() {
			String email = getUserEmail();
			String username = getUsernameByEmail(email);
		    return username;
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
    private static void checkAndClose(ServerSocket serverSocket) {
        try {
            serverSocket.setSoTimeout(TIMEOUT_MINUTES * 60 * 1000);
            serverSocket.accept();
        } catch (SocketTimeoutException e) {
            System.out.println("No connection within " + TIMEOUT_MINUTES + " minutes. Closing the server.");
            Worker.getInstance().closeServerGracefully();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private boolean validateLogin(String email, String hashedPassword) throws SQLException {
    	Connection connection = JDBCUtil.getConnection();
    	
    	try {
            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT * FROM nameid WHERE email=? AND password=?"
            )) {
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
	public List<Question> getQuestionsAndAnswers() {
	    List<Question> questions = new ArrayList<>();
	    Connection connection = JDBCUtil.getConnection();
	    try {
	        String query = "SELECT question.id, question.text, answer.id , answer.text, answer.iscorrect "
	        		+ "FROM question JOIN answer ON question.id = answer.question_id ORDER BY RAND() LIMIT 5;";
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
	                List<Answer> answers = new ArrayList<>();
	                do {
	                    answers.add(new Answer(answerId, answerText, isCorrect));
	                } while (resultSet.next() && resultSet.getInt("question.id") == questionId);
	            }
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }

	    return questions;
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
    }