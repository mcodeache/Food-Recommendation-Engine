import java.io.*;
import java.net.*;
import java.sql.SQLException;

class ClientHandler extends Thread {
    private Socket clientSocket;
    private Database database;
    private Users users;

    public ClientHandler(Socket socket, Database database) {
        this.clientSocket = socket;
        this.database = database;
        this.users = new Users(database);
    }

    public void run() {
        try {
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            String username = in.readLine();
            String password = in.readLine();

            boolean authenticated = users.authenticate(username, password);

            if (authenticated) {
                out.println("Authentication successful");
                out.flush();
                String menu = users.sendMenuForRole(username);
                out.println(menu);
                out.flush();

                String userChoice;
                while ((userChoice = in.readLine()) != null) {
                    users.processUserChoice(out, username, userChoice);
                    out.println(menu); // Send the menu again after processing the choice
                    out.flush();
                }
            } else {
                out.println("Authentication failed");
                out.flush();
            }
        } catch (IOException | SQLException e) {
            System.out.println("Error handling client: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.out.println("Error closing client socket: " + e.getMessage());
            }
        }
    }
}
