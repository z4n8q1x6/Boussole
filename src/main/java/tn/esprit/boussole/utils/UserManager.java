package tn.esprit.boussole.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.prefs.Preferences;

/**
 * Utility class to manage user-related queries and session data.
 * Provides convenient methods to retrieve franchise ID and user information
 * from the database using the logged-in user's email.
 */
public class UserManager {

  /**
   * Retrieves the franchise ID of the user by their email address.
   * @param email the user's email address
   * @return the franchise ID, or -1 if the user is not found or has no franchise assigned
   */
  public static int getFranchiseIdByEmail(String email) {
    if (email == null || email.trim().isEmpty()) {
      System.err.println("UserManager: Email is null or empty");
      return -1;
    }

    System.out.println("UserManager DEBUG: Recherche de l'ID franchise pour l'email: '" + email + "'");

    String sql = "SELECT id_franchise FROM utilisateur WHERE email = ? LIMIT 1";
    Connection conn = MyBdConnexion.getinstance().getCnx();

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, email.trim());
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          int franchiseId = rs.getInt("id_franchise");
          System.out.println("UserManager DEBUG: ID Franchise trouvé dans la base: " + franchiseId);
          // Handle NULL values in database (returns 0 for null)
          return franchiseId > 0 ? franchiseId : -1;
        } else {
          System.out.println("UserManager DEBUG: Aucun utilisateur trouvé avec cet email dans la table 'utilisateur'.");
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
   * @return the logged-in user's email, or empty string if not found
   */
  public static String getCurrentUserEmail() {
    try {
      Preferences prefs =
              Preferences.userRoot()
                      .node("tn.esprit.boussole.gui.loginController");
      String email = prefs.get("email", "");

      System.out.println("UserManager DEBUG: Email récupéré des préférences: '" + email + "'");

      return email;
    } catch (Exception e) {
      System.err.println("UserManager: Error retrieving email from session");
      e.printStackTrace();
      return "";
    }
  }

  /**
   * Retrieves the franchise ID of the currently logged-in user.
   * Combines getCurrentUserEmail() and getFranchiseIdByEmail().
   * @return the franchise ID, or -1 if user is not logged in or has no franchise
   */
  public static int getCurrentUserFranchiseId() {
    String email = getCurrentUserEmail();
    if (email.isEmpty()) {
      System.err.println("UserManager: No user logged in (email is empty in prefs)");
      return -1;
    }
    return getFranchiseIdByEmail(email);
  }

  /**
   * Validates that the franchise ID is valid (positive integer).
   * @param franchiseId the franchise ID to validate
   * @return true if franchiseId > 0, false otherwise
   */
  public static boolean isValidFranchiseId(int franchiseId) {
    return franchiseId > 0;
  }

  // ==================== NEW METHODS FOR INTEGRATION ====================

  /**
   * Get current user role from session
   * @return the user role (SIEGE, ENTREPRISE, or empty string)
   */
  public static String getCurrentUserRole() {
    try {
      Preferences prefs = Preferences.userRoot().node("tn.esprit.boussole.gui.loginController");
      String role = prefs.get("role", "");
      System.out.println("UserManager DEBUG: Rôle récupéré des préférences: '" + role + "'");
      return role;
    } catch (Exception e) {
      System.err.println("UserManager: Error retrieving role from session");
      e.printStackTrace();
      return "";
    }
  }

  /**
   * Check if current user is SIEGE (headquarters)
   * @return true if user role is SIEGE
   */
  public static boolean isCurrentUserSiege() {
    return "SIEGE".equals(getCurrentUserRole());
  }

  /**
   * Check if current user is ENTREPRISE (franchise)
   * @return true if user role is ENTREPRISE
   */
  public static boolean isCurrentUserEntreprise() {
    return "ENTREPRISE".equals(getCurrentUserRole());
  }

  /**
   * Get current user ID from session
   * @return user ID, or -1 if not found
   */
  public static int getCurrentUserId() {
    try {
      String email = getCurrentUserEmail();
      if (email.isEmpty()) return -1;

      String sql = "SELECT id_user FROM utilisateur WHERE email = ? LIMIT 1";
      Connection conn = MyBdConnexion.getinstance().getCnx();

      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, email);
        try (ResultSet rs = ps.executeQuery()) {
          if (rs.next()) {
            return rs.getInt("id_user");
          }
        }
      }
    } catch (SQLException e) {
      System.err.println("UserManager: Error retrieving user ID");
      e.printStackTrace();
    }
    return -1;
  }

  /**
   * Get current user first name from session
   * @return user first name, or empty string if not found
   */
  public static String getCurrentUserPrenom() {
    try {
      Preferences prefs = Preferences.userRoot().node("tn.esprit.boussole.gui.loginController");
      return prefs.get("prenom", "");
    } catch (Exception e) {
      return "";
    }
  }

  /**
   * Get current user full name from database
   * @return user full name (nom + prenom), or empty string if not found
   */
  public static String getCurrentUserFullName() {
    try {
      String email = getCurrentUserEmail();
      if (email.isEmpty()) return "";

      String sql = "SELECT nom, prenom FROM utilisateur WHERE email = ? LIMIT 1";
      Connection conn = MyBdConnexion.getinstance().getCnx();

      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, email);
        try (ResultSet rs = ps.executeQuery()) {
          if (rs.next()) {
            String nom = rs.getString("nom");
            String prenom = rs.getString("prenom");
            return (prenom != null ? prenom + " " : "") + (nom != null ? nom : "");
          }
        }
      }
    } catch (SQLException e) {
      System.err.println("UserManager: Error retrieving user name");
      e.printStackTrace();
    }
    return "";
  }

  /**
   * Check if user is authenticated (has a valid session)
   * @return true if user email exists in preferences
   */
  public static boolean isAuthenticated() {
    return !getCurrentUserEmail().isEmpty();
  }

  /**
   * Clear all session data (logout)
   */
  public static void clearSession() {
    try {
      Preferences prefs = Preferences.userRoot().node("tn.esprit.boussole.gui.loginController");
      prefs.remove("jwt");
      prefs.remove("email");
      prefs.remove("role");
      prefs.remove("prenom");
      System.out.println("UserManager: Session cleared");
    } catch (Exception e) {
      System.err.println("UserManager: Error clearing session");
      e.printStackTrace();
    }
  }

  /**
   * Get the JWT token from session
   * @return JWT token, or empty string if not found
   */
  public static String getCurrentUserToken() {
    try {
      Preferences prefs = Preferences.userRoot().node("tn.esprit.boussole.gui.loginController");
      return prefs.get("jwt", "");
    } catch (Exception e) {
      return "";
    }
  }

  /**
   * Debug method to print all session data
   */
  public static void debugSession() {
    System.out.println("=== USER SESSION DEBUG ===");
    System.out.println("Email: " + getCurrentUserEmail());
    System.out.println("Role: " + getCurrentUserRole());
    System.out.println("Franchise ID: " + getCurrentUserFranchiseId());
    System.out.println("User ID: " + getCurrentUserId());
    System.out.println("Authenticated: " + isAuthenticated());
    System.out.println("==========================");
  }
}