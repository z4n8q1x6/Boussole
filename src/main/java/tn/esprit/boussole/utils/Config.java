package tn.esprit.boussole.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class Config {
  private static final Properties PROPERTIES = new Properties();

  static {
    try (InputStream is =
        Config.class.getClassLoader().getResourceAsStream("tn/esprit/boussole/config.properties")) {

      if (is == null) {
        throw new RuntimeException("config.properties not found");
      }
      PROPERTIES.load(is);
    } catch (IOException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  private Config() {}

  public static String get(String key) {
    return PROPERTIES.getProperty(key);
  }
}
