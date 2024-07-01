import java.sql.*;
import java.util.Map;

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

    public ResultSet fetchMenuItems() throws SQLException {
        String query = "SELECT meal_type, menuitem_id, item_name, price, availability FROM MENUITEM";
        Statement statement = connection.createStatement();
        return statement.executeQuery(query);
    }

    public void addMenuItem(String itemName, double price, String availability, String item_type) throws SQLException {
        String query = "INSERT INTO MENUITEM (item_name, price, availability, meal_type) VALUES (?, ?, ?, ?)";
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setString(1, itemName);
        preparedStatement.setDouble(2, price);
        preparedStatement.setString(3, availability);
        preparedStatement.setString(4, item_type);
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
        preparedStatement.setString(2,availability);
        preparedStatement.setString(3, itemName);
        int rowsUpdated = preparedStatement.executeUpdate();
        if (rowsUpdated > 0) {
            System.out.println("Menu item updated: " + itemName);
        } else {
            System.out.println("Menu item not found: " + itemName);
        }
    }

    public void updateMenuRatings(int menuItemId, double avgRating, double avgSentiment, String overallSentiment) throws SQLException {
        String query = "UPDATE menuitem SET average_rating = ?, average_sentiment = ?, overall_sentiment = ? WHERE menuitem_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setDouble(1, avgRating);
            ps.setDouble(2, avgSentiment);
            ps.setString(3, overallSentiment);
            ps.setInt(4, menuItemId);
            ps.executeUpdate();
        }
    }

    public ResultSet fetchNextDayRecommendations() throws SQLException {
        String query = "SELECT menuitem_id, meal_type, recommendation_date, feedback_id " +
                "FROM RECOMMENDATION " +
                "WHERE DATE(recommendation_date) = CURDATE()";
        Statement statement = connection.createStatement();
        return statement.executeQuery(query);
    }


    public String getMenuNameById(int menuItemId) throws SQLException {
        String query = "SELECT item_name FROM MENUITEM WHERE menuitem_id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, menuItemId);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString("item_name");
            } else {
                return null;
            }
        }
    }

    public ResultSet fetchDiscardedItems() throws SQLException {
        String query = "SELECT item_name, average_rating, average_sentiment, overall_sentiment, menuitem_id " +
                "FROM menuitem " +
                "WHERE average_rating <= 2";

        PreparedStatement preparedStatement = connection.prepareStatement(query);
        return preparedStatement.executeQuery();
    }

    public boolean submitFeedback(int itemID, String feedback, int rating, int userId, SentimentAnalysisResult sentimentResult) throws SQLException {

        sentimentResult = SentimentAnalysis.analyzeFeedback(feedback);
        int sentimentScore = 0;
        String sentimentLabel = "";
        Map<String, Integer> aspectScores = sentimentResult.getAspectSentimentScores();

        for (Map.Entry<String, Integer> entry : aspectScores.entrySet()) {
            String aspect = entry.getKey();
            sentimentScore = entry.getValue();
            sentimentLabel = SentimentAnalysis.getSentimentLabel(aspect, sentimentScore);
            System.out.println(sentimentLabel);
        }

        String checkQuery = "SELECT * FROM feedback WHERE menuitem_id = ? AND user_id = ?";
        PreparedStatement checkStatement = connection.prepareStatement(checkQuery);
        checkStatement.setInt(1, itemID);
        checkStatement.setInt(2, userId);
        ResultSet resultSet = checkStatement.executeQuery();

        boolean result;

        if (resultSet.next()) {
            String updateQuery = "UPDATE feedback SET feedback_text = ?, rating = ?, sentiment = ?, sentiment_score = ? WHERE menuitem_id = ? AND user_id = ?";
            PreparedStatement updateStatement = connection.prepareStatement(updateQuery);
            updateStatement.setString(1, feedback);
            updateStatement.setInt(2, rating);
            updateStatement.setString(3, sentimentLabel);
            updateStatement.setInt(4, sentimentScore);
            updateStatement.setInt(5, itemID);
            updateStatement.setInt(6, userId);
            int updatedRows = updateStatement.executeUpdate();
            result = updatedRows > 0;
            updateStatement.close();
        } else {
            String insertQuery = "INSERT INTO feedback (menuitem_id, feedback_text, user_id, rating, sentiment_score, sentiment) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement insertStatement = connection.prepareStatement(insertQuery);
            insertStatement.setInt(1, itemID);
            insertStatement.setString(2, feedback);
            insertStatement.setInt(3, userId);
            insertStatement.setInt(4, rating);
            insertStatement.setInt(5, sentimentScore);
            insertStatement.setString(6, sentimentLabel);
            int insertedRows = insertStatement.executeUpdate();
            result = insertedRows > 0;
            insertStatement.close();
        }

        resultSet.close();
        checkStatement.close();

        return result;
    }

    public boolean rollOutNextDayMenu(int userId, int[] breakfastIds, int[] lunchIds, int[] dinnerIds, int[] feedbackIds) throws SQLException {
        String sql = "INSERT INTO RECOMMENDATION (menuitem_id, meal_type, recommendation_date) VALUES (?, ?, CURDATE())";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {


            for (int i = 0; i < breakfastIds.length; i++) {
                int breakfastId = breakfastIds[i];
                preparedStatement.setInt(1, breakfastId);
                preparedStatement.setString(2, "breakfast");
//                preparedStatement.setInt(4, feedbackIds[i]);
                preparedStatement.executeUpdate();
            }

            for (int i = 0; i < lunchIds.length; i++) {
                int feedbackIndex = breakfastIds.length + i;
                preparedStatement.setInt(1, lunchIds[i]);
                preparedStatement.setString(2, "lunch");
//                preparedStatement.setInt(4, feedbackIds[feedbackIndex]);
                preparedStatement.executeUpdate();
            }

            for (int i = 0; i < dinnerIds.length; i++) {
                int feedbackIndex = breakfastIds.length + lunchIds.length + i;
                preparedStatement.setInt(1, dinnerIds[i]);
                preparedStatement.setString(2, "dinner");
//                preparedStatement.setInt(4, feedbackIds[feedbackIndex]);
                preparedStatement.executeUpdate();
            }

            return true;
        }
    }

    public void saveEmployeeSelection(int itemId, String mealType) throws SQLException {
        String query = "INSERT INTO employee_selection (menuitem_id, meal_type) VALUES (?, ?)";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, itemId);
        statement.setString(2, mealType);
        statement.executeUpdate();
    }

    public int getFeedbackIdByItemId(int itemId) throws SQLException {
        String query = "SELECT feedback_id FROM feedback WHERE menuitem_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, itemId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("feedback_id");
                } else {
                    return -1;
                }
            }
        }
    }

    public ResultSet fetchMenuItemFeedback() throws SQLException {
        String query = "SELECT mi.menuitem_id, mi.item_name, f.rating, f.sentiment_score, f.sentiment " +
                "FROM menuitem mi " +
                "JOIN feedback f ON mi.menuitem_id = f.menuitem_id";
        return executeQuery(query);
    }


    public ResultSet fetchMonthlyReport() throws SQLException {
        String sql = "SELECT * FROM monthly_report";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        return preparedStatement.executeQuery();
    }

    public ResultSet fetchDailyEmployeeSelections() throws SQLException {
        String query = "SELECT es.menuitem_id, COUNT(*) AS selection_count, AVG(f.rating) AS avg_rating, AVG(f.sentiment_score) AS avg_sentiment " +
                "FROM employee_selection es " +
                "JOIN feedback f ON es.feedback_id = f.feedback_id " +
                "WHERE es.created_on = CURDATE() " +
                "GROUP BY es.menuitem_id";
        return executeQuery(query);
    }

    public void addNotification(String message) throws SQLException {
        String query = "INSERT INTO NOTIFICATION (message, notification_date) VALUES (?, CURDATE())";
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setString(1, message);
        preparedStatement.executeUpdate();
        System.out.println("Notification added for feeback rollout");
    }

    private ResultSet executeQuery(String query) throws SQLException {
        Statement stmt = connection.createStatement();
        return stmt.executeQuery(query);
    }

}
