package tn.esprit.boussole.models;

import java.time.LocalDateTime;

public class user {
    private Integer idUser;
    private String nom;
    private String prenom;
    private String email;
    private String motDePasse;
    private String role;
    private Boolean actif;
    private LocalDateTime dateCreation;
    private Integer idFranchise
            ;




    public enum Role {
        SIEGE,
        ENTREPRISE
    }
    public user(){};

    public user(Integer idFranchise, LocalDateTime dateCreation, Boolean actif, String role, String motDePasse, String email, String prenom, String nom, Integer idUser) {
        this.idFranchise = idFranchise;
        this.dateCreation = dateCreation;
        this.actif = actif;
        this.role = role;
        this.motDePasse = motDePasse;
        this.email = email;
        this.prenom = prenom;
        this.nom = nom;
        this.idUser = idUser;
    };

    public Integer getIdUser() {
        return idUser;
    }

    public void setIdUser(Integer idUser) {
        this.idUser = idUser;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMotDePasse() {
        return motDePasse;
    }

    public void setMotDePasse(String motDePasse) {
        this.motDePasse = motDePasse;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Boolean getActif() {
        return actif;
    }

    public void setActif(Boolean actif) {
        this.actif = actif;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public Integer getidFranchise() {
        return idFranchise ;
    }

    public void setidFranchise(Integer idFranchisee) {this.idFranchise = idFranchisee;}

    @Override
    public String toString() {
        return "utilisateur{" +
                "idUser=" + idUser +
                ", nom='" + nom + '\'' +
                ", prenom='" + prenom + '\'' +
                ", email='" + email + '\'' +
                ", motDePasse='" + motDePasse + '\'' +
                ", role=" + role +
                ", actif=" + actif +
                ", dateCreation=" + dateCreation +
                ", idFranchise=" + idFranchise +
                '}';
    }
}

