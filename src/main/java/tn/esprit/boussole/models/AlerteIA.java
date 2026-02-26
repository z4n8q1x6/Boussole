package tn.esprit.boussole.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;

public class AlerteIA {
  private int id;
  private String type_alerte;
  private String message;
  private float score_gravite;
  private Timestamp date_detection;
  private int franchiseId;

  public AlerteIA() {}

  public AlerteIA(
      int id,
      String type_alerte,
      String message,
      float score_gravite,
      Timestamp date_detection,
      int franchiseId) {
    this.id = id;
    this.type_alerte = type_alerte;
    this.message = message;
    this.score_gravite = score_gravite;
    this.date_detection = date_detection;
    this.franchiseId = franchiseId;
  }

  public int getFranchiseId() {
    return franchiseId;
  }

  public void setFranchiseId(int franchiseId) {
    this.franchiseId = franchiseId;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getType_alerte() {
    return type_alerte;
  }

  public void setType_alerte(String type_alerte) {
    this.type_alerte = type_alerte;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public float getScore_gravite() {
    return score_gravite;
  }

  public void setScore_gravite(float score_gravite) {
    this.score_gravite = score_gravite;
  }

  @JsonProperty("date_detection_formatted")
  public String getDate_detection() {
    if (date_detection == null) return null;
    return date_detection.toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
  }

  public void setDate_detection(Timestamp date_detection) {
    this.date_detection = date_detection;
  }
}
