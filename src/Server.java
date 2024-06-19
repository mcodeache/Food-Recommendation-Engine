import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            System.out.println("Server started, waiting for clients...");

            Database database = new Database("jdbc:mysql://localhost:3306/RecommendationEngine", "root", "ITT@1234");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected");

                ClientHandler clientHandler = new ClientHandler(clientSocket, database);
                clientHandler.start();
            }
        } catch (IOException e) {
            System.out.println("Could not listen on port 12345");
            System.out.println(e.getMessage());
        }
    }
}
