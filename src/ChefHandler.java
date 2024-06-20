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
        while (true) {
            sendmenu();
            String choice = in.readLine();

            switch (choice) {
                case "view_food_menu":
                    viewFoodMenu();
                    break;
                case "roll_out_next_day_menu":
                    rollOutNextDayMenu();
                    break;
                case "view_monthly_report":
                    viewMonthlyReport();
                    break;
                case "q":
                    return;
                default:
                    out.println("Invalid choice. Please try again.");
                    break;
            }
        }
    }

    private void sendmenu() {
        out.println("Chef Menu: a) View Food Menu b) Roll Out Next Day Menu c) View Monthly Report d) Exit");
        out.flush();
    }

    private void viewFoodMenu() throws SQLException {
        ResultSet rs = database.fetchMenuItems();
        StringBuilder menu = new StringBuilder("\n");
        while (rs.next()) {
            menu.append(rs.getString("item_name")).append(": $")
                    .append(rs.getDouble("price")).append(", Available: ")
                    .append(rs.getString("availability")).append("\n");
        }
        out.println(menu.toString());
        rs.close();
    }

    private void rollOutNextDayMenu() throws IOException, SQLException {
        out.println("Enter Breakfast Menu:");
        String breakfast = in.readLine();
        out.println("Enter Lunch Menu:");
        String lunch = in.readLine();
        out.println("Enter Dinner Menu:");
        String dinner = in.readLine();

        boolean success = database.rollOutNextDayMenu(breakfast, lunch, dinner);
        if (success) {
            out.println("Next day menu rolled out successfully.");
        } else {
            out.println("Failed to roll out next day menu.");
        }
    }

    private void viewMonthlyReport() throws SQLException {
        ResultSet rs = database.fetchMonthlyReport();
        StringBuilder report = new StringBuilder("Monthly Report:\n");
        while (rs.next()) {
            report.append("Date: ").append(rs.getDate("report_date"))
                    .append(", Total Sales: $").append(rs.getDouble("total_sales")).append("\n");
        }
        out.println(report.toString());
        rs.close();
    }
}
