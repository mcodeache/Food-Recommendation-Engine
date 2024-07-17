package UnitTest;

import Server.Users;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import Database.Database;

import static org.junit.jupiter.api.Assertions.*;

public class UserAuthenticationTest {
    private Database db;
    private Users users;

    @BeforeEach
    public void setUp() throws SQLException {
        db = new Database("jdbc:mysql://localhost:3306/RecommendationEngine", "root", "ITT@1234");
        db.connect();
        users = new Users(db);

        try (Connection connection = db.getConnection()) {
            Statement statement = connection.createStatement();

            // Insert roles if they don't already exist
            statement.executeUpdate("INSERT IGNORE INTO ROLE (role_id, role_name) VALUES " +
                    "(1, 'admin'), " +
                    "(2, 'chef'), " +
                    "(3, 'employee')");

            // Insert users if they don't already exist
            statement.executeUpdate("INSERT IGNORE INTO USER (user_id, user_name, role_id) VALUES " +
                    "(7, 'Alice', 1), " +
                    "(8, 'Bob', 2), " +
                    "(9, 'Charlie', 3)");

            // Insert user credentials if they don't already exist
            statement.executeUpdate("INSERT IGNORE INTO USERCREDENTIALS (credentials_id, user_id, password_hash) VALUES " +
                    "(7, 7, 'password123'), " +
                    "(8, 8, 'chefpass'), " +
                    "(9, 9, 'employeepass')");
        }
    }

    @Test
    public void testAuthenticateAdminSuccess() throws SQLException {
        boolean authenticated = users.authenticate("Alice", "password123");
        assertTrue(authenticated);
    }

    @Test
    public void testAuthenticateChefSuccess() throws SQLException {
        boolean authenticated = users.authenticate("Bob", "chefpass");
        assertTrue(authenticated);
    }

    @Test
    public void testAuthenticateEmployeeSuccess() throws SQLException {
        boolean authenticated = users.authenticate("Charlie", "employeepass");
        assertTrue(authenticated);
    }

    @Test
    public void testAuthenticateInvalidPassword() throws SQLException {
        boolean authenticated = users.authenticate("Charlie", "wrongpassword");
        assertFalse(authenticated);
    }

    @Test
    public void testAuthenticateNonExistentUser() throws SQLException {
        boolean authenticated = users.authenticate("nonexistent", "1234");
        assertFalse(authenticated);
    }

    @Test
    public void testAuthenticateInvalidEmployeeIdFormat() throws SQLException {
        boolean authenticated = users.authenticate("invalidIdFormat", "1234");
        assertFalse(authenticated);
    }
}
