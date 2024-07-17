package Server;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import Database.Database;
import Exceptions.CustomExceptionHandler;
import Controllers.*;

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
            database.connect();
            users = new Users(database);
            executor = Executors.newCachedThreadPool();
        } catch (IOException | SQLException exception) {
            CustomExceptionHandler.handleException(exception);
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
                executor.shutdown();
                database.disconnect();
            } catch (IOException | SQLException e) {
                System.out.println("closing server: ");
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }
}
