package Database;

import java.sql.*;
import java.util.Map;
import RecommendationEngine.SentimentAnalysisResult;
import RecommendationEngine.SentimentAnalysis;

public class Database {
    private final String url;
    private final String username;
    private final String password;
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
        String query = "SELECT * FROM MENUITEM";
        Statement statement = connection.createStatement();
        return statement.executeQuery(query);
    }

    public void addMenuItem(String[] itemDetails) throws SQLException {
        String query = "INSERT INTO MENUITEM (item_name, price, availability, meal_type, diet_type, spice_level, preference, sweet_tooth) VALUES (?, ?, ?, ?, ?, ?, ? ,?)";
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setString(1, itemDetails[0]);
        preparedStatement.setDouble(2, Double.parseDouble(itemDetails[1]));
        preparedStatement.setString(3, itemDetails[2]);
        preparedStatement.setString(4, itemDetails[3]);
        preparedStatement.setString(5, itemDetails[4]);
        preparedStatement.setString(6, itemDetails[5]);
        preparedStatement.setString(7, itemDetails[6]);
        preparedStatement.setInt(8, Integer.parseInt(itemDetails[7]));
        preparedStatement.executeUpdate();
        System.out.println("Menu item added: " + itemDetails[0]);
    }

    public void deleteMenuItem(int itemId) throws SQLException {
        String query = "DELETE FROM MENUITEM WHERE menuitem_id = ?";
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setInt(1, itemId);
        int rowsDeleted = preparedStatement.executeUpdate();
        if (rowsDeleted > 0) {
            System.out.println("Menu item deleted: " + itemId);
        } else {
            System.out.println("Menu item not found: " + itemId);
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

    public ResultSet fetchUserProfile(int userId) throws SQLException {
        String query = "SELECT * FROM userProfile WHERE user_id = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, userId);
        return statement.executeQuery();
    }


    public ResultSet fetchNextDayRecommendations(ResultSet userProfile) throws SQLException {
        int sweetTooth = 0;
        String spiceLevel = "", preference = "", diet_type = "";

        String query = "SELECT r.menuitem_id, r.meal_type, r.recommendation_date, m.item_name, m.price, m.availability " +
                "FROM RECOMMENDATION r " +
                "JOIN MENUITEM m ON r.menuitem_id = m.menuitem_id " +
                "WHERE DATE(r.recommendation_date) = CURDATE() " +
                "ORDER BY " +
                "CASE WHEN m.diet_type = ? THEN 1 ELSE 2 END, " +
                "CASE WHEN m.preference = ? THEN 1 ELSE 2 END, " +
                "CASE WHEN m.spice_level = ? THEN 1 ELSE 2 END, " +
                "CASE WHEN m.sweet_tooth = ? THEN 1 ELSE 2 END";

        while (userProfile.next()){
            diet_type = userProfile.getString("diet_type");
            preference = userProfile.getString("preference");
            spiceLevel = userProfile.getString("spice_level");
            sweetTooth = userProfile.getInt("sweet_tooth");
        }

        PreparedStatement statement = connection.prepareStatement(query);

        statement.setString(1, diet_type);
        statement.setString(2, preference);
        statement.setString(3, spiceLevel);
        statement.setInt(4, sweetTooth);


        return statement.executeQuery();
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

    public void saveEmployeeSelection(int itemId, String mealType, int userId) throws SQLException {
        String query = "INSERT INTO employee_selection (menuitem_id, meal_type, created_on, user_id) VALUES (?, ?, CURDATE(), ?)";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, itemId);
        statement.setString(2, mealType);
        statement.setInt(3, userId);
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
        String query = "SELECT es.menuitem_id, COUNT(*) AS selection_count, AVG(mi.average_rating) AS avg_rating, AVG(mi.average_sentiment) AS avg_sentiment " +
                "FROM employee_selection es " +
                "JOIN menuitem mi ON es.menuitem_id = mi.menuitem_id " +
                 "WHERE es.created_on = CURDATE() " +
                "GROUP BY es.menuitem_id";
        return executeQuery(query);
    }

    public void addNotification(String message) throws SQLException {
        String query = "INSERT INTO NOTIFICATION (message, created_on) VALUES (?, CURDATE())";
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setString(1, message);
        preparedStatement.executeUpdate();
        System.out.println("Notification added for feeback rollout");
    }

    public ResultSet fetchTodayNotifications() throws SQLException {
        String query = "SELECT message FROM NOTIFICATION WHERE created_on = CURDATE()";
        PreparedStatement statement = connection.prepareStatement(query);
        return statement.executeQuery();
    }

    public void updateNotificationViewedOn(int userId) throws SQLException {
        String query = "UPDATE user SET notification_viewed_on = CURRENT_TIMESTAMP WHERE user_id = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, userId);
        statement.executeUpdate();
        statement.close();
    }

    public boolean hasSelectionRecordForToday(int userId) throws SQLException {
        String query = "SELECT COUNT(*) FROM EMPLOYEE_SELECTION WHERE user_id = ? AND created_on = CURDATE()";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, userId);
        ResultSet resultSet = statement.executeQuery();

        boolean hasRecord = false;
        if (resultSet.next()) {
            hasRecord = resultSet.getInt(1) > 0;
        }

        statement.close();
        return hasRecord;
    }

    private ResultSet executeQuery(String query) throws SQLException {
        Statement stmt = connection.createStatement();
        return stmt.executeQuery(query);
    }

    private boolean getRecordByField(String tableName, String fieldName, Object fieldValue) throws SQLException {
        String query = String.format("SELECT * FROM %s WHERE %s = ?", tableName, fieldName);
        PreparedStatement statement = connection.prepareStatement(query);
        if (fieldValue instanceof Integer) {
            statement.setInt(1, (Integer) fieldValue);
        } else if (fieldValue instanceof String) {
            statement.setString(1, (String) fieldValue);
        } else {
            throw new IllegalArgumentException("Unsupported field value type");
        }
        return false;
    }
}
