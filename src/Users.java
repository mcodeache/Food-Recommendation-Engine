import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Users {
    private final Database database;

    public Users(Database database) {
        this.database = database;
    }

    public String sendMenuForRole(String loggedInUsername) throws SQLException {
        String menu = "User does not exist.";
        try {
            database.connect();
            ResultSet userResult = database.fetchUser(loggedInUsername);

            if (userResult.next()) {
                String roleName = userResult.getString("role_name");
                switch (roleName.toLowerCase()) {
                    case "admin":
                        menu = getAdminMenu();
                        break;
                    case "chef":
                        menu = getChefMenu();
                        break;
                    case "employee":
                        menu = getEmployeeMenu();
                        break;
                    default:
                        menu = "Unknown role. Cannot display menu.";
                        break;
                }
            }
        } finally {
            database.disconnect();
        }
        return menu;
    }

    private String getAdminMenu() {
        return "Admin Menu: " +
                "1. View Food Menu\n" +
                "2. Add Item in Food Menu\n" +
                "3. Update Item in Food Menu\n" +
                "4. Delete Item in Food Menu\n" +
                "5. Exit";
    }

    private String getChefMenu() {
        return "Chef Menu:\n" +
                "1. View Food Menu\n" +
                "2. Roll Out Next Day Menu\n" +
                "3. View Monthly Report\n" +
                "4. Exit";
    }

    private String getEmployeeMenu() {
        return "Employee Menu:\n" +
                "1. View Next day Recommendation\n" +
                "2. Give Feedback for today Menu\n" +
                "3. Exit";
    }

    public boolean authenticate(String username, String password) {
        try {
            database.connect();
            ResultSet resultSet = database.fetchUser(username);

            if (resultSet.next()) {
                String storedPassword = resultSet.getString("password_hash");
                return storedPassword.equals(password);
            } else {
                return false; // User does not exist
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                database.disconnect();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void processUserChoice(PrintWriter out, String username, String choice) throws SQLException {
        try {
            database.connect();
            ResultSet userResult = database.fetchUser(username);

            if (userResult.next()) {
                String roleName = userResult.getString("role_name");

                switch (roleName.toLowerCase()) {
                    case "admin":
                        processAdminChoice(out, choice);
                        break;
                    case "chef":
                        processChefChoice(out, choice);
                        break;
                    case "employee":
                        processEmployeeChoice(out, choice);
                        break;
                    default:
                        out.println("Unknown role. Cannot process choice.");
                        out.flush();
                        break;
                }
            } else {
                out.println("User does not exist.");
                out.flush();
            }
        } finally {
            database.disconnect();
        }
    }

    private void processAdminChoice(PrintWriter out, String choice) {
        switch (choice) {
            case "1":
                out.println("Viewing Food Menu...");
                break;
            case "2":
                out.println("Adding Item...");
                break;
            case "3":
                out.println("Updating Item...");
                break;
            case "4":
                out.println("Deleting Item...");
                break;
            case "5":
                out.println("Exiting...");
                break;
            default:
                out.println("Invalid choice. Please enter a number from 1 to 5.");
                break;
        }
        out.flush();
    }

    private void processChefChoice(PrintWriter out, String choice) {
        switch (choice) {
            case "1":
                out.println("Viewing Food Menu...");
                break;
            case "2":
                out.println("Rolling Out Next Day Menu...");
                break;
            case "3":
                out.println("Viewing Monthly Report...");
                break;
            case "4":
                out.println("Exiting...");
                break;
            default:
                out.println("Invalid choice. Please enter a number from 1 to 4.");
                break;
        }
        out.flush();
    }

    private void processEmployeeChoice(PrintWriter out, String choice) {
        switch (choice) {
            case "1":
                out.println("Viewing Next Day Recommendation...");
                break;
            case "2":
                out.println("Giving Feedback...");
                break;
            case "3":
                out.println("Exiting...");
                break;
            default:
                out.println("Invalid choice. Please enter a number from 1 to 3.");
                break;
        }
        out.flush();
    }
}
