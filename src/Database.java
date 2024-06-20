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

    // Method to fetch user details by username
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

    // Method to fetch menu items for a given role
    public ResultSet fetchMenuForRole(String roleName) throws SQLException {
        String query = "SELECT MENUITEM.item_name, MENUITEM.price, MENUITEM.availability " +
                "FROM MENUITEM " +
                "JOIN ROLE_MENU ON MENUITEM.item_id = ROLE_MENU.item_id " +
                "JOIN ROLE ON ROLE_MENU.role_id = ROLE.role_id " +
                "WHERE ROLE.role_name = ?";
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setString(1, roleName);
        return preparedStatement.executeQuery();
    }

    //Method to view menu items
    public ResultSet fetchMenuItems() throws SQLException {
        String query = "SELECT item_name, price, availability FROM MENUITEM";
        Statement statement = connection.createStatement();
        return statement.executeQuery(query);
    }


    // Method to add a new menu item
    public void addMenuItem(String itemName, double price, String availability) throws SQLException {
        String query = "INSERT INTO MENUITEM (item_name, price, availability) VALUES (?, ?, ?)";
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setString(1, itemName);
        preparedStatement.setDouble(2, price);
        preparedStatement.setString(3, availability);
        preparedStatement.executeUpdate();
        System.out.println("Menu item added: " + itemName);
    }

    // Method to delete a menu item
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

    // Method to update the price of a menu item
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
}
