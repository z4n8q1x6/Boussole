package tn.esprit.boussole.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Utility class to manage user-related queries and session data.
 * Provides convenient methods to retrieve franchise ID and user information
 * from the database using the logged-in user's email.
 */
public class UserManager {

  /**
   * Retrieves the franchise ID of the user by their email address.
   * 
   * @param email the user's email address
   * @return the franchise ID, or -1 if the user is not found or has no franchise assigned
   */
  public static int getFranchiseIdByEmail(String email) {
    if (email == null || email.trim().isEmpty()) {
      System.err.println("UserManager: Email is null or empty");
      return -1;
    }

    String sql = "SELECT id_franchise FROM utilisateur WHERE email = ? LIMIT 1";
    Connection conn = Database.getInstance().getConnection();

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, email.trim());
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          int franchiseId = rs.getInt("id_franchise");
          // Handle NULL values in database (returns 0 for null)
          return franchiseId > 0 ? franchiseId : -1;
        }
      }
    } catch (SQLException e) {
      System.err.println("UserManager: Error retrieving franchise ID for email: " + email);
      e.printStackTrace();
    }

    return -1; // User not found or no franchise assigned
  }

  /**
   * Retrieves the user's email from session preferences.
   * 
   * @return the logged-in user's email, or empty string if not found
   */
  public static String getCurrentUserEmail() {
    try {
      java.util.prefs.Preferences prefs = 
          java.util.prefs.Preferences.userRoot()
              .node("tn.esprit.boussole.gui.loginController");
      return prefs.get("email", "");
    } catch (Exception e) {
      System.err.println("UserManager: Error retrieving email from session");
      e.printStackTrace();
      return "";
    }
  }

  /**
   * Retrieves the franchise ID of the currently logged-in user.
   * Combines getCurrentUserEmail() and getFranchiseIdByEmail().
   * 
   * @return the franchise ID, or -1 if user is not logged in or has no franchise
   */
  public static int getCurrentUserFranchiseId() {
    String email = getCurrentUserEmail();
    if (email.isEmpty()) {
      System.err.println("UserManager: No user logged in");
      return -1;
    }
    return getFranchiseIdByEmail(email);
  }

  /**
   * Validates that the franchise ID is valid (positive integer).
   * 
   * @param franchiseId the franchise ID to validate
   * @return true if franchiseId > 0, false otherwise
   */
  public static boolean isValidFranchiseId(int franchiseId) {
    return franchiseId > 0;
  }
}
