import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ChefHandler {
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private Database database;

    public ChefHandler(PrintWriter out, BufferedReader in, String username, Database database) {
        this.out = out;
        this.in = in;
        this.username = username;
        this.database = database;
    }

    public void handle() throws IOException, SQLException {
//        String menu = new Users(database).getChefMenu();
//        out.println(menu);
        out.flush();

        String choice = in.readLine();
        switch (choice) {
            case "1":
                // Handle view menu
                ResultSet menuItems = database.fetchMenuItems();
                out.println("Menu Items:");
                while (menuItems.next()) {
                    String itemName = menuItems.getString("item_name");
                    double price = menuItems.getDouble("price");
                    boolean availability = menuItems.getBoolean("availability");
                    out.printf("%s: $%.2f - %s%n", itemName, price, availability ? "Available" : "Not Available");
                }
                out.println("END_OF_MESSAGE");
                out.flush();
                break;
            case "2":
                // Handle roll out next day menu
                out.println("Rolling out next day menu...");
                // Logic to roll out next day menu
                out.println("Next day menu rolled out successfully.");
                out.flush();
                break;
            case "3":
                // Handle view monthly report
                out.println("Viewing monthly report...");
                // Logic to view monthly report
                out.println("Monthly report generated.");
                out.flush();
                break;
            case "4":
                // Exit
                out.println("Exiting...");
                out.flush();
                break;
            default:
                out.println("Invalid choice. Please enter a number from 1 to 4.");
                out.flush();
                break;
        }
    }
}
