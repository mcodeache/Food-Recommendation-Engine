import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AdminHandler {
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private Database database;

    public AdminHandler(PrintWriter out, BufferedReader in, String username, Database database) {
        this.out = out;
        this.in = in;
        this.username = username;
        this.database = database;
    }

    public void handle() throws IOException, SQLException {
        sendMenu();
        while (true) {
            String request = in.readLine();

            if ("menu".equals(request)) {
                sendMenu();
            } else if ("5".equals(request)) {
                out.println("Logging out...");
                out.println("END_OF_MESSAGE");
                out.flush();
                break;
            } else {
                handleOperation(request);
                sendMenu();
            }
        }
    }

    private void sendMenu() throws IOException {
        out.println("Admin Menu: 1. View Food Menu 2. Add Item in Food Menu 3. Update Item in Food Menu 4. Delete Item in Food Menu 5. Exit");
        out.flush();
    }

    private void handleOperation(String choice) throws IOException, SQLException {
        switch (choice) {
            case "1":
                viewMenuItems();
                break;
            case "2":
                addItem();
                break;
            case "3":
                updateItem();
                break;
            case "4":
                deleteItem();
                break;
            default:
                out.println("Invalid choice. Please enter a number from 1 to 5.");
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

    private void addItem() throws IOException, SQLException {
        String itemName = in.readLine();
        double itemPrice = Double.parseDouble(in.readLine());
        String availability = "yes";
        String itemType = in.readLine();
        database.addMenuItem(itemName, itemPrice, availability, itemType);
        out.println("Item added successfully.");
        out.println("END_OF_MESSAGE");
        out.flush();
    }

    private void updateItem() throws IOException, SQLException {
        String itemName = in.readLine();
        double newItemPrice = Double.parseDouble(in.readLine());
        String availability = in.readLine();
        database.updateMenuItem(itemName, newItemPrice, availability);
        out.println("Item updated successfully.");
        out.println("END_OF_MESSAGE");
        out.flush();
    }

    private void deleteItem() throws IOException, SQLException {
        String itemName = in.readLine();

        database.deleteMenuItem(itemName);
        out.println("Item deleted successfully.");
        out.println("END_OF_MESSAGE");
        out.flush();
    }
}
