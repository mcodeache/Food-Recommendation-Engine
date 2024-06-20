import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;

public class EmployeeHandler {
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private Database database;
    private int userId;

    public EmployeeHandler(PrintWriter out, BufferedReader in, String username, Database database, int userId) {
        this.out = out;
        this.in = in;
        this.username = username;
        this.database = database;
        this.userId = userId;
    }

    public void handle() throws IOException, SQLException {
        sendMenu();
        while (true) {
            String request = in.readLine();

            if ("menu".equals(request)) {
                sendMenu();
            } else if ("4".equals(request)) {
                out.println("Exiting...");
                out.println("END_OF_MESSAGE");
                out.flush();
                break;
            } else {
                handleOperation(request);
                sendMenu();
            }
        }
    }

    private void sendMenu() {
        out.println("Employee Menu: 1) View Next Day Recommendation 2) Give Feedback 3) View Menu 4) Exit");
        out.flush();
    }

    private void handleOperation(String choice) throws IOException, SQLException {
        switch (choice) {
            case "1":
                viewNextDayRecommendation();
                break;
            case "2":
                giveFeedback();
                break;
            case "3":
                viewMenuItems();
                break;
            default:
                out.println("Invalid choice. Please enter a number from 1 to 4.");
                out.flush();
                break;
        }
    }

    private void viewMenuItems() throws SQLException {
        ResultSet menuItems = database.fetchMenuItems();
        boolean hasItems = false;

        while (menuItems.next()) {
            hasItems = true;
            String itemName = menuItems.getString("item_name");
            double price = menuItems.getDouble("price");
            boolean availability = menuItems.getBoolean("availability");
            out.printf("%s: $%.2f - %s%n", itemName, price, availability ? "Available" : "Not Available");
        }

        if (!hasItems) {
            out.println("No menu items available.");
        }

        out.println("END_OF_MESSAGE");
        out.flush();
    }

    private void viewNextDayRecommendation() throws SQLException {
        ResultSet rs = database.fetchNextDayRecommendations();
        if (rs.next()) {
            out.println("Breakfast: " + rs.getString("breakfast"));
            out.println("Lunch: " + rs.getString("lunch"));
            out.println("Dinner: " + rs.getString("dinner"));
        } else {
            out.println("No recommendations available.");
        }
        out.println("END_OF_MESSAGE");
        rs.close();
        out.flush();
    }

    private void giveFeedback() throws IOException, SQLException {
        out.println("Enter the menu item ID you want to give feedback for:");
        out.flush();
        int itemID = Integer.parseInt(in.readLine());

        out.println("Enter your feedback for item ID " + itemID + ":");
        out.flush();
        String feedback = in.readLine();

        out.println("Enter your rating (1-5) for item ID " + itemID + ":");
        out.flush();
        int rating = Integer.parseInt(in.readLine());

        boolean success = database.submitFeedback(itemID, feedback, rating, username, userId);
        if (success) {
            out.println("Feedback submitted successfully.");
        } else {
            out.println("Failed to submit feedback.");
        }
        out.println("END_OF_MESSAGE");
        out.flush();
    }
}
