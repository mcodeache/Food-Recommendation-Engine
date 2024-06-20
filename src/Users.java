import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Users {
    private final Database database;

    public Users(Database database) {
        this.database = database;
    }

    public boolean authenticate(String username, String password) throws SQLException {
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
            throw new RuntimeException(e);
        }
    }

    public String getRoleName(String loggedInUsername) throws SQLException {
        String roleName = "Unknown";
        ResultSet userResult = null;
        try {
            database.connect();
            userResult = database.fetchUser(loggedInUsername);

            if (userResult.next()) {
                roleName = userResult.getString("role_name");
            }
        } finally {
            if (userResult != null) {
                userResult.close(); // Properly close ResultSet
            }
        }
        return roleName;
    }
}
