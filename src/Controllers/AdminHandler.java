package Controllers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import Database.Database;

public class AdminHandler {
    private final PrintWriter out;
    private final BufferedReader in;
    private final Database database;
    private Map<String, Runnable> menuOptions;

    public AdminHandler(PrintWriter out, BufferedReader in, Database database) {
        this.out = out;
        this.in = in;
        this.database = database;
        initializeMenuOptions();
    }

    private void initializeMenuOptions() {
        menuOptions = new HashMap<>();
        menuOptions.put("1", this::viewMenuItems);
        menuOptions.put("2", this::addItem);
        menuOptions.put("3", this::updateItem);
        menuOptions.put("4", this::deleteItem);
        menuOptions.put("5", this::logout);
    }

    private void viewMenuItems() {
        try {
            ResultSet menuItems = database.fetchMenuItems();
            boolean hasItems = false;

            String headerFormat = "%-10s %-20s %-10s %-15s %-15s %-15s %-15s %-20s %-15s %-15s %-20s %-10s%n";
            String rowFormat = "%-10d %-20s $%-9.2f %-15s %-15s %-15.2f %-15.2f %-20s %-15s %-15s %-20s %-10s%n";

            StringBuilder report = new StringBuilder();

            report.append(String.format(headerFormat, "Item ID", "Menu Item", "Price", "Availability", "Meal Type", "Avg Rating", "Avg Sentiment", "Overall Sentiment", "Diet Type", "Spice Level", "Preference", "Sweet Tooth"));

            while (menuItems.next()) {
                hasItems = true;
                int itemId = menuItems.getInt("menuitem_id");
                String itemName = menuItems.getString("item_name");
                double price = menuItems.getDouble("price");
                String availability = menuItems.getString("availability");
                String mealType = menuItems.getString("meal_type");
                double avgRating = menuItems.getDouble("average_rating");
                double avgSentiment = menuItems.getDouble("average_sentiment");
                String overallSentiment = menuItems.getString("overall_sentiment");
                String dietType = menuItems.getString("diet_type");
                String spiceLevel = menuItems.getString("spice_level");
                String preference = menuItems.getString("preference");
                boolean sweetTooth = menuItems.getBoolean("sweet_tooth");

                report.append(String.format(rowFormat, itemId, itemName, price, availability, mealType, avgRating, avgSentiment, overallSentiment, dietType, spiceLevel, preference, sweetTooth ? "Yes" : "No"));
            }

            if (!hasItems) {
                report.append("No menu items available.\n");
            }

            report.append("END_OF_MESSAGE");
            out.print(report);
            out.flush();
        } catch (SQLException e) {
            handleError(e);
        }
    }


    private void addItem() {
        try {
            String itemName = in.readLine();
            double itemPrice = Double.parseDouble(in.readLine());
            String mealType = in.readLine();
            String dietType = in.readLine();
            String spiceLevel = in.readLine();
            String preference = in.readLine();
            String sweetTooth = in.readLine();

            database.addMenuItem(new String[] { itemName, Double.toString(itemPrice), "yes", mealType, dietType, spiceLevel, preference, sweetTooth });

            out.println("Item added successfully.");
            out.println("END_OF_MESSAGE");
            out.flush();
        } catch (IOException | SQLException e) {
            handleError(e);
        }
    }

    private void updateItem() {
        try {
            String itemName = in.readLine();
            double newItemPrice = Double.parseDouble(in.readLine());
            String availability = in.readLine();

            database.updateMenuItem(itemName, newItemPrice, availability);

            out.println("Item updated successfully.");
            out.println("END_OF_MESSAGE");
            out.flush();
        } catch (IOException | SQLException e) {
            handleError(e);
        }
    }

    private void deleteItem() {
        try {
            int itemId = Integer.parseInt(in.readLine());
            database.deleteMenuItem(itemId);

            out.println("Item deleted successfully.");
            out.println("END_OF_MESSAGE");
            out.flush();
        } catch (IOException | SQLException e) {
            handleError(e);
        }
    }

    private void logout() {
        out.println("Logging out...");
        out.println("END_OF_MESSAGE");
        out.flush();
    }

    private void handleError(Exception e) {
        System.err.println("Error: " + e.getMessage());
        out.println("An error occurred. Please try again.");
        out.flush();
    }

    public void handle() {
        try {
            while (true) {
                sendMenu();
                String request = in.readLine();
                if ("menu".equals(request)) {
                    sendMenu();
                } else if ("5".equals(request)) {
                    menuOptions.get("5").run();
                    break;
                } else {
                    Runnable action = menuOptions.get(request);
                    if (action != null) {
                        action.run();
                    } else {
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
        out.println("Admin Menu: 1. View Food Menu 2. Add Item in Food Menu 3. Update Item in Food Menu 4. Delete Item in Food Menu 5. Exit");
        out.flush();
    }
}
