package Server;

import java.sql.ResultSet;
import java.sql.SQLException;
import Database.Database;

public class Users {
    private final Database database;
    int user_id = 0;

    public Users(Database database) {
        this.database = database;
    }

    public boolean authenticate(String username, String password) throws SQLException {
        try {
            database.connect();
            ResultSet resultSet = database.fetchUser(username);

            if (resultSet.next()) {
                String storedPassword = resultSet.getString("password_hash");
                user_id = resultSet.getInt("user_id");
                return storedPassword.equals(password);
            } else {
                return false;
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
                userResult.close();
            }
        }
        return roleName;
    }

    public int getUser_id(){
        return user_id;
    }
}
