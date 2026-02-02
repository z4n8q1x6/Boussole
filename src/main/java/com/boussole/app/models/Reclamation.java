package com.boussole.app.models;

import java.sql.Date;

public class Reclamation {

  private int id;
  private String sujet;
  private String description;
  private StatutReclamation statut;
  private Date dateCreation;

  // Constructors
  public Reclamation() {}

  public Reclamation(
      int id, String sujet, String description, StatutReclamation statut, Date dateCreation) {
    this.id = id;
    this.sujet = sujet;
    this.description = description;
    this.statut = statut;
    this.dateCreation = dateCreation;
  }

  // Getters and Setters
  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getSujet() {
    return sujet;
  }

  public void setSujet(String sujet) {
    this.sujet = sujet;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public StatutReclamation getStatut() {
    return statut;
  }

  public void setStatut(StatutReclamation statut) {
    this.statut = statut;
  }

  public Date getDateCreation() {
    return dateCreation;
  }

  public void setDateCreation(Date dateCreation) {
    this.dateCreation = dateCreation;
  }
}
