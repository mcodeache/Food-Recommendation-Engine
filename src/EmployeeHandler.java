import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

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
            } else if ("5".equals(request)) {
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
        out.println("Employee Menu: 1) View Next Day Recommendation 2) Give Feedback 3) Select Items For Tomorrow 4) View Menu 5) Exit");
        out.flush();
    }

    private void handleOperation(String choice) throws IOException, SQLException {
        switch (choice) {
            case "1":
                viewNextDayRecommendations();
                break;
            case "2":
                giveFeedback();
                break;
            case "3":
                selectMenuItems();
                break;
            case "4":
                viewMenuItems();
                break;
            default:
                out.println("Invalid choice. Please enter a number from 1 to 4.");
                out.flush();
                break;
        }
    }


    private void selectMenuItems() throws SQLException, IOException {
        ResultSet userProfile = database.fetchUserProfile(userId);
        ResultSet recommendedItems = database.fetchNextDayRecommendations(userId, userProfile);
        while (recommendedItems.next()) {
            int menuitemId = recommendedItems.getInt("menuitem_id");
            String itemName = database.getMenuNameById(menuitemId);
            String category = recommendedItems.getString("meal_type");

            out.println(menuitemId + ": " + itemName + " (" + category + ")");
        }
        out.println("END_OF_MESSAGE");
        out.flush();

        Set<Integer> selectedBreakfastItems = selectItems();
        Set<Integer> selectedLunchItems = selectItems();
        Set<Integer> selectedDinnerItems = selectItems();

        saveSelections(selectedBreakfastItems, "breakfast");
        saveSelections(selectedLunchItems, "lunch");
        saveSelections(selectedDinnerItems, "dinner");

        out.println("END_OF_MESSAGE");
        out.flush();
    }

    private Set<Integer> selectItems() throws IOException {
        Set<Integer> selectedItems = new HashSet<>();
        for (int i = 0; i < 2; i++) {
            int itemId = Integer.parseInt(in.readLine());
            selectedItems.add(itemId);
        }

        return selectedItems;
    }

    private void saveSelections(Set<Integer> selectedItems, String mealType) throws SQLException {
        for (int itemId : selectedItems) {
            database.saveEmployeeSelection(itemId, mealType);
        }
    }

    private void viewMenuItems() throws SQLException {
        ResultSet menuItems = database.fetchMenuItems();
        boolean hasItems = false;

        while (menuItems.next()) {
            hasItems = true;
            int itemId = menuItems.getInt("menuitem_id");
            String mealType = menuItems.getString("meal_type");
            String itemName = menuItems.getString("item_name");
            double price = menuItems.getDouble("price");
            boolean availability = menuItems.getBoolean("availability");
            out.printf("%d: %s - $%.2f %s - %s%n", itemId, itemName, price, mealType, availability ? "Available" : "Not Available");
        }

        if (!hasItems) {
            out.println("No menu items available.");
        }

        out.println("END_OF_MESSAGE");
        out.flush();
    }

    private void viewNextDayRecommendations() throws SQLException {
        ResultSet userProfile = database.fetchUserProfile(userId);
        ResultSet recommendations = database.fetchNextDayRecommendations(userId, userProfile);
        StringBuilder breakfastReport = new StringBuilder("Breakfast Recommendations:\n");
        StringBuilder lunchReport = new StringBuilder("Lunch Recommendations:\n");
        StringBuilder dinnerReport = new StringBuilder("Dinner Recommendations:\n");

        while (recommendations.next()) {
            int menuItemId = recommendations.getInt("menuitem_id");
            String menuItem = database.getMenuNameById(menuItemId);
            String mealType = recommendations.getString("meal_type");
            Timestamp recommendationDate = recommendations.getTimestamp("recommendation_date");
            int feedbackId = recommendations.getInt("feedback_id");

            String itemReport = String.format("Menu Item: %s, Recommendation Date: %s, Feedback ID: %d%n",
                    menuItem, recommendationDate, feedbackId);

            switch (mealType.toLowerCase()) {
                case "breakfast":
                    breakfastReport.append(itemReport);
                    break;
                case "lunch":
                    lunchReport.append(itemReport);
                    break;
                case "dinner":
                    dinnerReport.append(itemReport);
                    break;
            }
        }

        StringBuilder finalReport = new StringBuilder();
        if (breakfastReport.length() > "Breakfast Recommendations:\n".length()) {
            finalReport.append(breakfastReport);
        } else {
            finalReport.append("No breakfast recommendations for today.\n");
        }

        if (lunchReport.length() > "Lunch Recommendations:\n".length()) {
            finalReport.append(lunchReport);
        } else {
            finalReport.append("No lunch recommendations for today.\n");
        }

        if (dinnerReport.length() > "Dinner Recommendations:\n".length()) {
            finalReport.append(dinnerReport);
        } else {
            finalReport.append("No dinner recommendations for today.\n");
        }

        out.println(finalReport);
        out.println("END_OF_MESSAGE");
        out.flush();
        recommendations.close();
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

        // Analyze sentiment and calculate sentiment score
        SentimentAnalysisResult sentimentResult = (SentimentAnalysisResult) SentimentAnalysis.analyzeFeedback(feedback);
        boolean success = database.submitFeedback(itemID, feedback, rating, userId, sentimentResult);
        if (success) {
            out.println("Feedback submitted successfully.");
        } else {
            out.println("Failed to submit feedback.");
        }
        out.println("END_OF_MESSAGE");
        out.flush();
    }
}
