import java.io.*;
import java.net.*;
import java.sql.*;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private Users users;
    private Database database;
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
        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            // Read username and password from client
            String username = in.readLine();
            String password = in.readLine();

            // Authenticate user
            boolean authenticated = users.authenticate(username, password);

            if (authenticated) {
                out.println("Authentication successful");

                String roleName = users.getRoleName(username);
                userId = users.getUser_id();

                out.println(roleName);
                switch (roleName.toLowerCase()) {
                    case "admin":
                        handleAdmin(username);
                        break;
                    case "chef":
                        handleChef(username);
                        break;
                    case "employee":
                        handleEmployee(username);
                        break;
                    default:
                        out.println("Unknown role. Cannot proceed.");
                        break;
                }
            } else {
                out.println("Authentication failed");
            }
        } catch (IOException | SQLException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            try {
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }

    private void handleAdmin(String username) throws IOException, SQLException {
        AdminHandler adminHandler = new AdminHandler(out, in, username, database);
        adminHandler.handle();
    }

    private void handleChef(String username) throws IOException, SQLException {
        ChefHandler chefHandler = new ChefHandler(out, in, username, database, userId);
        chefHandler.handle();
    }

    private void handleEmployee(String username) throws IOException, SQLException {
        EmployeeHandler employeeHandler = new EmployeeHandler(out, in, username, database,userId);
        employeeHandler.handle();
    }
}
