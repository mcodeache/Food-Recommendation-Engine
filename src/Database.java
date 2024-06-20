import java.sql.*;

public class Database {
    private String url;
    private String username;
    private String password;
    private Connection connection;

    public Database(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.connection = null;
    }

    public void connect() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(url, username, password);
            System.out.println("Connected to database.");
        }
    }

    public void disconnect() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
            System.out.println("Disconnected from database.");
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public ResultSet fetchUser(String username) throws SQLException {
        String query = "SELECT USER.*, ROLE.role_name, USERCREDENTIALS.password_hash " +
                "FROM USER " +
                "JOIN ROLE ON USER.role_id = ROLE.role_id " +
                "JOIN USERCREDENTIALS ON USER.user_id = USERCREDENTIALS.user_id " +
                "WHERE USER.user_name = ?";
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setString(1, username);
        return preparedStatement.executeQuery();
    }

//    public ResultSet fetchMenuForRole(String roleName) throws SQLException {
//        String query = "SELECT MENUITEM.item_name, MENUITEM.price, MENUITEM.availability " +
//                "FROM MENUITEM " +
//                "JOIN ROLE_MENU ON MENUITEM.item_id = ROLE_MENU.item_id " +
//                "JOIN ROLE ON ROLE_MENU.role_id = ROLE.role_id " +
//                "WHERE ROLE.role_name = ?";
//        PreparedStatement preparedStatement = connection.prepareStatement(query);
//        preparedStatement.setString(1, roleName);
//        return preparedStatement.executeQuery();
//    }

    public ResultSet fetchMenuItems() throws SQLException {
        String query = "SELECT item_name, price, availability FROM MENUITEM";
        Statement statement = connection.createStatement();
        return statement.executeQuery(query);
    }

    public void addMenuItem(String itemName, double price, String availability) throws SQLException {
        String query = "INSERT INTO MENUITEM (item_name, price, availability) VALUES (?, ?, ?)";
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setString(1, itemName);
        preparedStatement.setDouble(2, price);
        preparedStatement.setString(3, availability);
        preparedStatement.executeUpdate();
        System.out.println("Menu item added: " + itemName);
    }

    public void deleteMenuItem(String itemName) throws SQLException {
        String query = "DELETE FROM MENUITEM WHERE item_name = ?";
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setString(1, itemName);
        int rowsDeleted = preparedStatement.executeUpdate();
        if (rowsDeleted > 0) {
            System.out.println("Menu item deleted: " + itemName);
        } else {
            System.out.println("Menu item not found: " + itemName);
        }
    }

    public void updateMenuItem(String itemName, double newPrice, String availability) throws SQLException {
        String query = "UPDATE MENUITEM SET price = ?, availability = ? WHERE item_name = ?";
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setDouble(1, newPrice);
        preparedStatement.setString(2, itemName);
        preparedStatement.setString(3,availability);
        int rowsUpdated = preparedStatement.executeUpdate();
        if (rowsUpdated > 0) {
            System.out.println("Menu item updated: " + itemName);
        } else {
            System.out.println("Menu item not found: " + itemName);
        }
    }
    public ResultSet fetchNextDayRecommendations() throws SQLException {
        String query = "SELECT breakfast, lunch, dinner FROM next_day_recommendation";
        Statement statement = connection.createStatement();
        return statement.executeQuery(query);
    }

    public boolean submitFeedback(int itemID, String feedback, int rating, String username, int userId) throws SQLException {
        // First, check if feedback already exists for the user and item
        String checkQuery = "SELECT * FROM feedback WHERE menuitem_id = ? AND user_id = ?";
        PreparedStatement checkStatement = connection.prepareStatement(checkQuery);
        checkStatement.setInt(1, itemID);
        checkStatement.setInt(2, userId);
        ResultSet resultSet = checkStatement.executeQuery();

        boolean result;

        if (resultSet.next()) {
            // Update existing feedback
            String updateQuery = "UPDATE feedback SET feedback_text = ?, rating = ? WHERE menuitem_id = ? AND user_id = ?";
            PreparedStatement updateStatement = connection.prepareStatement(updateQuery);
            updateStatement.setString(1, feedback);
            updateStatement.setInt(2, rating);
            updateStatement.setInt(3, itemID);
            updateStatement.setInt(4, userId);
            int updatedRows = updateStatement.executeUpdate();
            result = updatedRows > 0;
            updateStatement.close();
        } else {
            // Insert new feedback
            String insertQuery = "INSERT INTO feedback (menuitem_id, feedback_text, user_id, rating) VALUES (?, ?, ?, ?)";
            PreparedStatement insertStatement = connection.prepareStatement(insertQuery);
            insertStatement.setInt(1, itemID);
            insertStatement.setString(2, feedback);
            insertStatement.setInt(3, userId);
            insertStatement.setInt(4, rating);
            int insertedRows = insertStatement.executeUpdate();
            result = insertedRows > 0;
            insertStatement.close();
        }

        resultSet.close();
        checkStatement.close();

        return result;
    }



    public boolean rollOutNextDayMenu(String breakfast, String lunch, String dinner) throws SQLException {
        String sql = "INSERT INTO next_day_menu (breakfast, lunch, dinner) VALUES (?, ?, ?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, breakfast);
            preparedStatement.setString(2, lunch);
            preparedStatement.setString(3, dinner);
            int result = preparedStatement.executeUpdate();
            return result > 0;
        }
    }

    public ResultSet fetchMonthlyReport() throws SQLException {
        String sql = "SELECT * FROM monthly_report";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        return preparedStatement.executeQuery();
    }
}
