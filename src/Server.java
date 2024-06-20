import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final int PORT = 12345;
    private ServerSocket serverSocket;
    private Database database;
    private Users users;
    private ExecutorService executor;

    public Server() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Server started");
            database = new Database("jdbc:mysql://localhost:3306/RecommendationEngine", "root", "ITT@1234");
            database.connect(); // Connect to the database
            users = new Users(database); // Initialize Users object for authentication
            executor = Executors.newCachedThreadPool(); // Create threads
        } catch (IOException | SQLException e) {
            System.err.println("Error starting server: " + e.getMessage());
        }
    }

    public void start() {
        try {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: ");

                ClientHandler clientHandler = new ClientHandler(clientSocket, users, database);
                executor.submit(clientHandler);
            }
        } catch (IOException e) {
            System.err.println("Error accepting client connection: " + e.getMessage());
        } finally {
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
                executor.shutdown(); // Shut down the executor service
                database.disconnect(); // Disconnect from the database
            } catch (IOException | SQLException e) {
                System.err.println("Error closing server: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }
}
