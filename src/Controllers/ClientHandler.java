package Controllers;

import java.io.*;
import java.net.*;
import java.sql.*;
import Database.Database;
import Server.Users;
import Exceptions.CustomExceptionHandler;


public class ClientHandler implements Runnable {
    private static final int MAX_ATTEMPTS = 3;

    private final Socket clientSocket;
    private final Users users;
    private final Database database;
    private PrintWriter out;
    private BufferedReader in;
    private int userId = 0;

    public ClientHandler(Socket clientSocket, Users users, Database database) {
        this.clientSocket = clientSocket;
        this.users = users;
        this.database = database;
    }

    @Override
    public void run() {
        int attempts = 0;
        boolean authenticated = false;

        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            while (attempts < MAX_ATTEMPTS && !authenticated) {
                String username = in.readLine();
                String password = in.readLine();

                authenticated = users.authenticate(username, password);

                if (authenticated) {
                    out.println("Authentication successful");

                    String roleName = users.getRoleName(username);
                    userId = users.getUser_id();

                    out.println(roleName);
                    switch (roleName.toLowerCase()) {
                        case "admin":
                            handleAdmin();
                            break;
                        case "chef":
                            handleChef();
                            break;
                        case "employee":
                            handleEmployee();
                            break;
                        default:
                            out.println("Unknown role. Cannot proceed.");
                            break;
                    }
                } else {
                    out.println("Authentication failed. Please try again.");
                    attempts++;
                }
            }

            if (!authenticated) {
                out.println("Maximum authentication attempts reached. Connection closing.");
            }
        } catch (IOException | SQLException exception) {
            CustomExceptionHandler.handleException(exception);
        } finally {
            try {
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                System.out.println("Error closing client socket: " + e.getMessage());
            }
        }
    }

    private void handleAdmin() throws IOException, SQLException {
        AdminHandler adminHandler = new AdminHandler(out, in, database);
        adminHandler.handle();
    }

    private void handleChef() throws IOException, SQLException {
        ChefHandler chefHandler = new ChefHandler(out, in, database, userId);
        chefHandler.handle();
    }

    private void handleEmployee() throws IOException, SQLException {
        EmployeeHandler employeeHandler = new EmployeeHandler(out, in, database, userId);
        employeeHandler.handle();
    }
}
