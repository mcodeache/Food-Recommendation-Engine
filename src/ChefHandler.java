import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class ChefHandler {
    private final int userId;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private Database database;


    public ChefHandler(PrintWriter out, BufferedReader in, String username, Database database, int userId) {
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
            } else if ("6".equals(request)) {
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
        out.println("Chef Menu: 1) View Food Menu 2) Roll Out Next Day Menu 3) View Employee Selection 4) View Today's Recommmendation 5) View Discarded Items 6) Exit");
        out.flush();
    }

    private void handleOperation(String choice) throws IOException, SQLException {
            switch (choice) {
                case "1":
                    viewFoodMenu();
                    break;
                case "2":
                    rollOutNextDayMenu();
                    break;
                case "3":
                    viewEmployeeSelection();
                    break;
                case "4":
                    viewRecommendations();
                    break;
                case "5":
                    getDiscardedItems();
                    break;
                case "6":
                    return;
                default:
                    out.println("Invalid choice. Please try again.");
                    break;
            }
    }

    private void getDiscardedItems() throws SQLException, IOException {
        ResultSet rs = database.fetchDiscardedItems();

        StringBuilder report = new StringBuilder();
        report.append(String.format("%-10s %-20s %-15s %-15s %-20s%n", "Item ID", "Menu Item", "Avg Rating", "Avg Sentiment", "Overall Sentiment"));

        while (rs.next()) {
            int itemId = rs.getInt("menuitem_id");
            String itemName = rs.getString("item_name");
            double avgRating = rs.getDouble("average_rating");
            double avgSentiment = rs.getDouble("average_sentiment");
            String overallSentiment = rs.getString("overall_sentiment");

            report.append(String.format("%-10d %-20s %-15.2f %-15.2f %-20s%n", itemId, itemName, avgRating, avgSentiment, overallSentiment));
        }

        if (report.length() == 0) {
            out.println("No items found with low average rating or sentiment.");
        } else {
            out.println("Items with Low Average Rating or Sentiment:");
            out.println(report);
        }
        out.println("END_OF_MESSAGE");
        out.flush();
        String selectedOption = in.readLine();
        if(selectedOption != null){
            if (selectedOption.equals("1")){
                int itemId = Integer.parseInt(in.readLine());
                String itemName = database.getMenuNameById(itemId);
                database.deleteMenuItem(itemName);
            }
            else if(selectedOption.equals("2")){
                int itemId = Integer.parseInt(in.readLine());
                String itemName = database.getMenuNameById(itemId);
                if (itemName != null){
                    String question1 = "What didn't you like about" + itemName;
                    String question2 = "How would you like "+ itemName + "to taste?";
                    String question3 = "Please Share your mom's recipe";
                    database.addNotification(question1);
                    database.addNotification(question2);
                    database.addNotification(question3);
                    out.println("completed");
                }
                getDiscardedItems();
            }
        }
    }

    private void viewRecommendations() throws SQLException {
        ResultSet rs = database.fetchMenuItemFeedback();
        Map<Integer, MenuItemFeedback> feedbackMap = new HashMap<>();

        // Collect feedback data
        while (rs.next()) {
            int menuItemId = rs.getInt("menuitem_id");
            String itemName = rs.getString("item_name");
            int rating = rs.getInt("rating");
            double sentimentScore = rs.getDouble("sentiment_score");
            String sentiment = rs.getString("sentiment");

            feedbackMap.putIfAbsent(menuItemId, new MenuItemFeedback(menuItemId, itemName));
            feedbackMap.get(menuItemId).addFeedback(rating, sentimentScore, sentiment);
            feedbackMap.get(menuItemId).extractKeywordsFromFeedback(sentiment);
        }
        rs.close();

        // Sort by average sentiment and rating
        List<MenuItemFeedback> feedbackList = new ArrayList<>(feedbackMap.values());
        feedbackList.sort((f1, f2) -> {
            int sentimentComparison = Double.compare(f2.getAverageSentiment(), f1.getAverageSentiment());
            if (sentimentComparison != 0) {
                return sentimentComparison;
            }
            return Double.compare(f2.getAverageRating(), f1.getAverageRating());
        });

        String report = updateMenuItemsAndGenerateReport(feedbackList);

        out.println(report);
        out.println("END_OF_MESSAGE");
        out.flush();
    }


    private String updateMenuItemsAndGenerateReport(List<MenuItemFeedback> feedbackList) throws SQLException {
        StringBuilder report = new StringBuilder();
        report.append(String.format("%-20s %-10s %-15s %-15s %-10s %-20s\n", "Menu Item", "ID", "Avg Rating", "Avg Sentiment", "Count", "Overall Sentiment"));

        for (MenuItemFeedback feedback : feedbackList) {
            int menuItemId = feedback.getMenuItemId();
            double avgRating = feedback.getAverageRating();
            double avgSentiment = feedback.getAverageSentiment();
            String overallSentiment = feedback.getKeywords().toString();

            database.updateMenuRatings(menuItemId, avgRating, avgSentiment, overallSentiment);

            report.append(String.format("%-20s %-10d %-15.2f %-15.2f %-10d %-20s\n", feedback.getItemName(), menuItemId, avgRating, avgSentiment, feedback.getFeedbackCount(), overallSentiment));
        }

        return report.toString();
    }


    private void viewFoodMenu() throws SQLException {
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


    private int[] getFeedbackIdsForItems(int[] breakfastIds, int[] lunchIds, int[] dinnerIds) throws SQLException {
        int[] feedbackIds = new int[breakfastIds.length + lunchIds.length + dinnerIds.length];
        int index = 0;
        for (int id : breakfastIds) {
            feedbackIds[index++] = database.getFeedbackIdByItemId(id);
        }
        for (int id : lunchIds) {
            feedbackIds[index++] = database.getFeedbackIdByItemId(id);
        }
        for (int id : dinnerIds) {
            feedbackIds[index++] = database.getFeedbackIdByItemId(id);
        }
        return feedbackIds;
    }


    private void rollOutNextDayMenu() throws IOException, SQLException {
//        String numberOfItems = in.readLine();
//        int items_size = Integer.parseInt(numberOfItems);

        int breakfastItems = Integer.parseInt(in.readLine());
        String breakfast = in.readLine();
        int[] breakfastIds = parseItemIds(breakfast);

        int lunchItems = Integer.parseInt(in.readLine());
        String lunch = in.readLine();
        int[] lunchIds = parseItemIds(lunch);

        int dinnerItems = Integer.parseInt(in.readLine());
        String dinner = in.readLine();
        int[] dinnerIds = parseItemIds(dinner);

        int[] feedbackIds = getFeedbackIdsForItems(breakfastIds, lunchIds, dinnerIds);

        boolean success = database.rollOutNextDayMenu(userId, breakfastIds, lunchIds, dinnerIds, feedbackIds);
        if (success) {
            out.println("Next day menu rolled out successfully.");
        } else {
            out.println("Failed to roll out next day menu.");
        }

        out.flush();
    }

    private int[] parseItemIds(String items) {
        String[] itemArray = items.split(",");
        int[] itemIds = new int[itemArray.length];
        for (int i = 0; i < itemArray.length; i++) {
            itemIds[i] = Integer.parseInt(itemArray[i]);
        }
        return itemIds;
    }

    public void viewEmployeeSelection() throws SQLException {
        ResultSet rs = database.fetchDailyEmployeeSelections();
        StringBuilder report = new StringBuilder("Daily Employee Selection Report:\n");

        while (rs.next()) {
            int itemId = rs.getInt("item_id");
            int selectionCount = rs.getInt("selection_count");
            double avgRating = rs.getDouble("avg_rating");
            double avgSentiment = rs.getDouble("avg_sentiment");

            // Assuming you have a method to get item name by ID
            String itemName = database.getMenuNameById(itemId);

            report.append("Item: ").append(itemName)
                    .append(", Count: ").append(selectionCount)
                    .append(", Avg Rating: ").append(String.format("%.2f", avgRating))
                    .append(", Avg Sentiment: ").append(String.format("%.2f", avgSentiment))
                    .append("\n");
        }

        out.println(report.toString());
        out.println("END_OF_MESSAGE");
        out.flush();
        rs.close();
    }

}