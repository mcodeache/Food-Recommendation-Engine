package Controllers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import Database.Database;
import RecommendationEngine.SentimentAnalysisResult;
import RecommendationEngine.SentimentAnalysis;

public class EmployeeHandler {
    private final PrintWriter out;
    private final BufferedReader in;
    private final Database database;
    private final int userId;
    private Map<String, Runnable> menuOptions;

    public EmployeeHandler(PrintWriter out, BufferedReader in, Database database, int userId) {
        this.out = out;
        this.in = in;
        this.database = database;
        this.userId = userId;
        initializeMenuOptions();
    }

    private void initializeMenuOptions() {
        menuOptions = new HashMap<>();
        menuOptions.put("1", this::viewNextDayRecommendations);
        menuOptions.put("2", this::giveFeedback);
        menuOptions.put("3", this::selectMenuItems);
        menuOptions.put("4", this::viewMenuItems);
        menuOptions.put("5", this::showNotification);
        menuOptions.put("6", this::exit);
    }

    public void handle() {
        try {
            while (true) {
                sendMenu();
                String request = in.readLine();

                if ("menu".equals(request)) {
                    sendMenu();
                } else if ("6".equals(request)) {
                    menuOptions.get("6").run();
                    break;
                } else {
                    Runnable action = menuOptions.get(request);
                    if (action != null) {
                        action.run();
                    } else {
                        out.println("Invalid choice. Please enter a number from 1 to 6.");
                        out.flush();
                    }
                    sendMenu();
                }
            }
        } catch (IOException e) {
            handleError(e);
        }
    }

    private void sendMenu() {
        out.println("Employee Menu: 1) View Next Day Recommendation 2) Give Feedback 3) Select Items For Tomorrow 4) View Menu 5) Show Notification 6) Exit");
        out.flush();
    }

    private void viewNextDayRecommendations() {
        try {
            ResultSet userProfile = database.fetchUserProfile(userId);
            ResultSet recommendations = database.fetchNextDayRecommendations(userProfile);
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
        } catch (SQLException e) {
            handleError(e);
        }
    }

    private void giveFeedback() {
        try {
            out.println("Enter the menu item ID you want to give feedback for:");
            out.flush();
            int itemID = Integer.parseInt(in.readLine());

            out.println("Enter your feedback for item ID " + itemID + ":");
            out.flush();
            String feedback = in.readLine();

            out.println("Enter your rating (1-5) for item ID " + itemID + ":");
            out.flush();
            int rating = Integer.parseInt(in.readLine());

            SentimentAnalysisResult sentimentResult = SentimentAnalysis.analyzeFeedback(feedback);
            boolean success = database.submitFeedback(itemID, feedback, rating, userId, sentimentResult);
            if (success) {
                out.println("Feedback submitted successfully.");
            } else {
                out.println("Failed to submit feedback.");
            }
            out.println("END_OF_MESSAGE");
            out.flush();
        } catch (IOException | NumberFormatException | SQLException e) {
            handleError(e);
        }
    }

    private void selectMenuItems() {
        try {
//                viewNextDayRecommendations();
                ResultSet userProfile = database.fetchUserProfile(userId);
                ResultSet recommendedItems = database.fetchNextDayRecommendations(userProfile);

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

                out.println("Selections saved successfully.");
                out.println("END_OF_MESSAGE");
                out.flush();
        } catch (IOException | SQLException e) {
            handleError(e);
        }
    }

    private Set<Integer> selectItems() throws IOException {
        Set<Integer> selectedItems = new HashSet<>();
        for (int i = 0; i < 1; i++) {
            try {
                int itemId = Integer.parseInt(in.readLine());
                selectedItems.add(itemId);
            } catch (NumberFormatException e) {
                handleError(e);
            }
        }
        return selectedItems;
    }

    private void saveSelections(Set<Integer> selectedItems, String mealType) {
        try {
            for (int itemId : selectedItems) {
                database.saveEmployeeSelection(itemId, mealType, userId);
            }
        } catch (SQLException e) {
            handleError(e);
        }
    }

    private void viewMenuItems() {
        try {
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
        } catch (SQLException e) {
            handleError(e);
        }
    }

    private void showNotification() {
        try {
            database.updateNotificationViewedOn(userId);
            ResultSet notifications = database.fetchTodayNotifications();
            StringBuilder notificationMessages = new StringBuilder();

            while (notifications.next()) {
                String message = notifications.getString("message");
                notificationMessages.append("\nMessage: ").append(message)
                        .append("\n-----\n");
            }

            if (!notificationMessages.isEmpty()) {
                out.println(notificationMessages);
                out.println("END_OF_MESSAGE");
                out.flush();
            }
        } catch (SQLException e) {
            handleError(e);
        }
    }

    private void exit() {
        out.println("Exiting...");
        out.println("END_OF_MESSAGE");
        out.flush();
    }

    private void handleError(Exception e) {
        System.err.println("Error: " + e.getMessage());
        out.println("An error occurred. Please try again.");
        out.flush();
    }
}
