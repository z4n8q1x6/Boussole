package tn.esprit.chargesdepenses.models;

public class Fournisseur {

    private Long id;
    private String nom;
    private String matriculeFiscal;
    private String telephone;

    // AJOUT INDISPENSABLE : pour correspondre au SQL "franchise_id INT NOT NULL"
    private int franchiseId;

    // Constructeurs
    public Fournisseur() {}

    // Constructeur pour la récupération (avec ID et franchiseId)
    public Fournisseur(Long id, String nom, String matriculeFiscal, String telephone, int franchiseId) {
        this.id = id;
        this.nom = nom;
        this.matriculeFiscal = matriculeFiscal;
        this.telephone = telephone;
        this.franchiseId = franchiseId;
    }

    // Constructeur pour l'insertion (sans ID car auto-incrément)
    public Fournisseur(String nom, String matriculeFiscal, String telephone, int franchiseId) {
        this.nom = nom;
        this.matriculeFiscal = matriculeFiscal;
        this.telephone = telephone;
        this.franchiseId = franchiseId;
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getMatriculeFiscal() { return matriculeFiscal; }
    public void setMatriculeFiscal(String matriculeFiscal) { this.matriculeFiscal = matriculeFiscal; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public int getFranchiseId() { return franchiseId; }
    public void setFranchiseId(int franchiseId) { this.franchiseId = franchiseId; }

    @Override
    public String toString() {
        return "Fournisseur{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", matriculeFiscal='" + matriculeFiscal + '\'' +
                ", telephone='" + telephone + '\'' +
                ", franchiseId=" + franchiseId +
                '}';
    }
}