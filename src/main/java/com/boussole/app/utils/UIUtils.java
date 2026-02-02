package com.boussole.app.utils;

import javafx.scene.control.*;

public class UIUtils {
  public static void clear(TextInputControl... fields) {
    for (TextInputControl field : fields) {
      field.clear();
    }
  }
}
