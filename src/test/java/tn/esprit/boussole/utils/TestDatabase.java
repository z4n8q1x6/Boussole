package tn.esprit.boussole.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import org.junit.jupiter.api.Test;

public class TestDatabase {
  @Test
  void testConnection() {
    Connection c = Database.getInstance().getConnection();
    assertNotNull(c);
    assertDoesNotThrow(() -> c.isValid(2));
    assertDoesNotThrow(() -> c.close());
  }
}
