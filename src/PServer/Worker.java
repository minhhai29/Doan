package PServer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
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
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.swing.JOptionPane;

import database.JDBCUtil;
import encryptions.AESEncryption;
public class Worker implements Runnable {
    private static final int TIMEOUT_MINUTES = 10;
    private static final List<ClientHandler> clients = new ArrayList<>();
	Map<String, Integer> highestPointPlayer = getPlayerWithHighestPoint();
	Map<String, Integer> highestWinStreakPlayer = getPlayerWithHighestWinStreak();
	Map<String, Integer> highestTotalMatchPlayer = getPlayerWithHighestTotalMatch();
	private List<String> onlineUsers = new ArrayList<>();
	private List<String> allUsers = new ArrayList<>();
	private ScheduledExecutorService executorService;
	private static Worker instance;
	private String generatedOTP;
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
        } finally {
            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdown();
            }
        }
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

	    public ClientHandler(Socket clientSocket) {
	        this.clientSocket = clientSocket;
	        try {
	            // Mở luồng vào/ra cho client
	        	in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
	            out= new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
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
	            	if (requestType != null) {
	                    switch (requestType) {
	                    	case "Hello from client!":
	                    		System.out.println("Client gửi: " + requestType);
	                            break;
	                        case "login":
	                            handleLogin();
	                            break;
	                        case "register":
	                            handleRegister();
	                            break;
	                        case "update_status":
	                        	handleUpdateStatus();
	                        	break;
	                        case "logout":
	                        	handleLogout();
	                        	break;
	                        case "testIQ":
	                        	handletestIQ();
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
	    private void handleLogin() {
	        try {
	            String email = in.readLine();
	            String hashedPassword = in.readLine();

	            if (validateLogin(email, hashedPassword)) {
	                out.write("Login successful \n");
	                out.flush();

	                String username = getUsernameByEmail(email);
	                int pointIQ = getIqPoints(email);
	                int playerRank = displayRanking(username);
	                String playerData = playerRank + "," + pointIQ + "\n";

	                out.write(username + "\n");
	                out.write(playerData);
	                out.flush();
	            } else {
	                out.write("Login failed \n");
	                out.flush();
	                return; // Break the loop if login fails
	            }
	        } catch (SQLException e) {
	            e.printStackTrace();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	    }

	    private void handleRegister() {
	        try {
	            String email = in.readLine();
	            String hashedPassword = in.readLine();
	            String username = in.readLine();
	            String gender = in.readLine();
	            boolean emailExists = isEmailExists(email);
	            boolean usernameExists = isUsernameExists(username);

	            if (emailExists) {
	                out.write("emailexist\n");
	                out.flush();
	            } else if (usernameExists) {
	                out.write("usernameexist\n");
	                out.flush();
	            } else {
	                String otp = generateOTP();
	                // Gửi mã OTP đến email
	                EmailSender.sendEmail(email, otp);
	                out.write("send_otp\n");
	                out.flush();

	                // Đọc mã OTP từ client
	                String enteredOTP = in.readLine();
	                if (otp.equals(enteredOTP)) {
	                    // OTP chính xác, thực hiện đăng ký tài khoản
	                    signUpUser(username, hashedPassword, email, gender);

	                    // Gửi thông báo đăng ký thành công và ID người dùng
	                    out.write("registersuccess\n");
	                    out.flush();
	                } else {
	                    // OTP không chính xác
	                    out.write("Incorrect OTP. Registration failed.\n");
	                    out.flush();
	                }
	            }
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	    }


	    private void handleUpdateStatus() {
	        try {
	            String email = in.readLine();
	            int status = Integer.parseInt(in.readLine());
	            updateOnlineStatus(email, status);
	        } catch (IOException | NumberFormatException e) {
	            e.printStackTrace();
	        }
	    }
	    private void handletestIQ() {
	        try {
	            String email = in.readLine();
	            int pointIQ = getIqPoints(email);
	            if(pointIQ == 0) {
	            	out.write("Cho phép test IQ" + "\n");
	            }else {
	            	out.write("Đã test IQ" + "\n");
	            }
	            out.flush();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	    }
	    private void handleLogout() {
	        try {
	        	out.write("Logout successful\n");
	            out.flush();
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

    }