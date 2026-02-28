package tn.esprit.boussole.utils;

import static org.junit.jupiter.api.Assertions.*;
import static org.testng.AssertJUnit.assertNotNull;

import java.sql.Connection;
import org.junit.jupiter.api.Test;
import org.testng.annotations.Test;

public class TestDatabase {
  @org.testng.annotations.Test
  void testConnection() {
    Connection c = MyBdConnexion.getinstance().getCnx();
    assertNotNull(c);
    assertDoesNotThrow(() -> c.isValid(2));
    assertDoesNotThrow(() -> c.close());
  }

  private void assertDoesNotThrow(Object o) {
  }
}
