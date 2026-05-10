package tn.esprit.boussole.service;

import tn.esprit.boussole.models.franchise;
import tn.esprit.boussole.models.user;
import tn.esprit.boussole.utils.MyBdConnexion;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class userService implements crud<user> {
    private Connection cnx;

    public userService() {
        cnx = MyBdConnexion.getinstance().getCnx();
    }


    @Override
    public void insertone(user user) throws SQLException {
        // Ajout de la colonne face_token
        String req = "INSERT INTO utilisateur(nom, prenom, email, mot_de_passe, role, actif, date_creation, id_franchise, face_token) VALUES (?,?,?,?,?,?,?,?,?)";

        try (PreparedStatement ps = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getNom());
            ps.setString(2, user.getPrenom());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getMotDePasse());
            ps.setString(5, user.getRole());
            ps.setBoolean(6, user.getActif());
            ps.setTimestamp(7, user.getDateCreation() != null ? Timestamp.valueOf(user.getDateCreation()) : null);

            if (user.getidFranchise() != null && user.getidFranchise() > 0) {
                ps.setInt(8, user.getidFranchise());
            } else {
                ps.setInt(8, 0);
            }
            
            // Enregistrement du face_token (peut être null)
            ps.setString(9, user.getFaceToken());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    user.setIdUser(rs.getInt(1));
                }
            }
        }
    }

    public void insertUserWithFranchise(user user, franchise f) throws SQLException {
        try {
            cnx.setAutoCommit(false);

            int idFranchise = 0;


            String sqlFranchise = "INSERT INTO franchises (nom, email, telephone, adresse, actif, date_creation, solde_actuel) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement psFranchise = cnx.prepareStatement(sqlFranchise, Statement.RETURN_GENERATED_KEYS)) {
                psFranchise.setString(1, f.getNom());
                psFranchise.setString(2, f.getEmail());
                psFranchise.setString(3, f.getTelephone());
                psFranchise.setString(4, f.getAdresse());
                psFranchise.setBoolean(5, f.getActif());
                psFranchise.setTimestamp(6, f.getDateCreation() != null ? Timestamp.valueOf(f.getDateCreation()) : null);
                psFranchise.setDouble(7, f.getSoldeActuel());

                psFranchise.executeUpdate();

                ResultSet rs = psFranchise.getGeneratedKeys();
                if (rs.next()) {
                    idFranchise = rs.getInt(1);
                    f.setId(idFranchise);
                } else {
                    throw new SQLException("Erreur récupération ID franchise");
                }
            }


            user.setidFranchise(idFranchise);
            insertone(user); // Appelle la méthode mise à jour

            cnx.commit();
        } catch (Exception e) {
            cnx.rollback();
            throw e;
        } finally {
            cnx.setAutoCommit(true);
        }
    }
    public void updateFranchiseStatus(int idFranchise, boolean status) throws SQLException {
        try {
            // Début de la transaction
            cnx.setAutoCommit(false);

            // 1. Mettre à jour la franchise
            String sqlFranchise = "UPDATE franchises SET actif = ? WHERE id = ?";
            try (PreparedStatement psF = cnx.prepareStatement(sqlFranchise)) {
                psF.setBoolean(1, status);
                psF.setInt(2, idFranchise);
                psF.executeUpdate();
            }

            // 2. Mettre à jour TOUS les utilisateurs liés à cette franchise
            // Automatiquement, si l'entreprise est inactive, les users le deviennent aussi
            String sqlUser = "UPDATE utilisateur SET actif = ? WHERE id_franchise = ?";
            try (PreparedStatement psU = cnx.prepareStatement(sqlUser)) {
                psU.setBoolean(1, status);
                psU.setInt(2, idFranchise);
                psU.executeUpdate();
            }

            // Valider les deux changements
            cnx.commit();
            System.out.println("✅ Statut synchronisé : Franchise " + idFranchise + " et ses utilisateurs sont désormais " + (status ? "actifs" : "inactifs"));

        } catch (SQLException e) {
            // En cas d'erreur sur l'une des deux tables, on annule tout
            cnx.rollback();
            throw e;
        } finally {
            cnx.setAutoCommit(true);
        }
    }
    @Override
    public void updateone(user user) throws SQLException {
        // Mise à jour incluant face_token si nécessaire (optionnel, mais bon à avoir)
        String req = "UPDATE utilisateur SET nom = ?, prenom = ?, email = ?, mot_de_passe = ?, role = ?, actif = ?, date_creation = ?, id_franchise = ?, face_token = ? WHERE id_user = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, user.getNom());
            ps.setString(2, user.getPrenom());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getMotDePasse());
            ps.setString(5, user.getRole());
            ps.setBoolean(6, user.getActif());
            ps.setTimestamp(7, user.getDateCreation() != null ? Timestamp.valueOf(user.getDateCreation()) : null);

            if (user.getidFranchise() != null && user.getidFranchise() > 0) {
                ps.setInt(8, user.getidFranchise());
            } else {
                ps.setInt(8, 0);
            }
            
            ps.setString(9, user.getFaceToken());
            ps.setInt(10, user.getIdUser());
            ps.executeUpdate();
        }
    }


    @Override
    public void deleteone(user user) throws SQLException {
        String req = "DELETE FROM utilisateur WHERE id_user = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, user.getIdUser());
            ps.executeUpdate();
        }
    }

    public void deleteUserAndFranchise(user user) throws SQLException {
        try {
            cnx.setAutoCommit(false);

            // 1. Supprimer l'utilisateur
            deleteone(user);

            // 2. Si une franchise est liée, la supprimer aussi
            if (user.getidFranchise() != null && user.getidFranchise() > 0) {
                String reqFranchise = "DELETE FROM franchises WHERE id = ?";
                try (PreparedStatement ps = cnx.prepareStatement(reqFranchise)) {
                    ps.setInt(1, user.getidFranchise());
                    ps.executeUpdate();
                }
            }

            cnx.commit();
        } catch (SQLException e) {
            cnx.rollback();
            throw e;
        } finally {
            cnx.setAutoCommit(true);
        }
    }

    public void deleteFranchiseAndUser(franchise f) throws SQLException {
        try {
            cnx.setAutoCommit(false);

            // 1. Supprimer la franchise
            String reqFranchise = "DELETE FROM franchises WHERE id = ?";
            try (PreparedStatement ps = cnx.prepareStatement(reqFranchise)) {
                ps.setInt(1, f.getId());
                ps.executeUpdate();
            }

            // 2. Supprimer l'utilisateur lié
            String reqUser = "DELETE FROM utilisateur WHERE id_franchise = ?";
            try (PreparedStatement ps = cnx.prepareStatement(reqUser)) {
                ps.setInt(1, f.getId());
                ps.executeUpdate();
            }

            cnx.commit();
        } catch (SQLException e) {
            cnx.rollback();
            throw e;
        } finally {
            cnx.setAutoCommit(true);
        }
    }


    @Override
    public List<user> selectAll(user ignored) throws SQLException {
        List<user> list = new ArrayList<>();
        // Ajout de face_token dans le SELECT
        String req = "SELECT id_user, nom, prenom, email, mot_de_passe, role, actif, date_creation, id_franchise, face_token FROM utilisateur";
        try (PreparedStatement ps = cnx.prepareStatement(req);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                user u = new user();
                u.setIdUser(rs.getInt("id_user"));
                u.setNom(rs.getString("nom"));
                u.setPrenom(rs.getString("prenom"));
                u.setEmail(rs.getString("email"));
                u.setMotDePasse(rs.getString("mot_de_passe"));
                u.setRole(rs.getString("role"));
                u.setActif(rs.getBoolean("actif"));
                Timestamp ts = rs.getTimestamp("date_creation");
                if (ts != null) u.setDateCreation(ts.toLocalDateTime());
                u.setidFranchise(rs.getInt("id_franchise"));
                u.setFaceToken(rs.getString("face_token")); // Récupération du token
                list.add(u);
            }
        }
        return list;
    }

    @Override
    public List<user> selectAll() {
        return List.of();
    }


    // ----------------- INITIALIZE ADMIN -----------------
    public void initializeAdmin() {
        System.out.println("Début de l'initialisation de l'admin...");
        if (cnx == null) {
            System.err.println("Impossible d'initialiser l'admin : connexion à la base de données non établie.");
            return;
        }
        String checkReq = "SELECT COUNT(*) FROM utilisateur WHERE role = 'SIEGE'";

        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(checkReq)) {
            if (rs.next()) {
                int count = rs.getInt(1);
                System.out.println("Nombre d'administrateurs trouvés : " + count);

                if (count == 0) {
                    System.out.println("Aucun administrateur trouvé. Création d'une franchise Siège...");

                    // 1. Créer ou récupérer une franchise "Siège"
                    int idFranchiseSiege = getOrCreateSiegeFranchise();

                    System.out.println("ID Franchise Siège : " + idFranchiseSiege);

                    // 2. Créer l'admin lié à cette franchise
                    String insertReq = "INSERT INTO utilisateur(nom, prenom, email, mot_de_passe, role, actif, date_creation, id_franchise) VALUES (?,?,?,?,?,?,?,?)";

                    try (PreparedStatement ps = cnx.prepareStatement(insertReq)) {
                        ps.setString(1, "Admin");
                        ps.setString(2, "System");
                        ps.setString(3, "admin@boussole.tn");
                        ps.setString(4, "admin123");
                        ps.setString(5, "SIEGE");
                        ps.setBoolean(6, true);
                        ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
                        ps.setInt(8, idFranchiseSiege); // ID Franchise Siège

                        int rows = ps.executeUpdate();
                        if (rows > 0) {
                            System.out.println("✅ Compte admin créé avec succès : admin@boussole.tn / admin123");
                        } else {
                            System.err.println("❌ Échec de la création du compte admin.");
                        }
                    }
                } else {
                    System.out.println("Un compte admin existe déjà.");
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur SQL lors de l'initialisation de l'admin : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private int getOrCreateSiegeFranchise() throws SQLException {
        // Vérifier si une franchise "Siège" existe
        String checkF = "SELECT id FROM franchises WHERE nom = 'SIEGE_PRINCIPAL' LIMIT 1";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(checkF)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }

        // Sinon, la créer
        String insertF = "INSERT INTO franchises (nom, email, telephone, adresse, actif, date_creation, solde_actuel) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(insertF, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, "SIEGE_PRINCIPAL");
            ps.setString(2, "contact@boussole.tn");
            ps.setString(3, "00000000");
            ps.setString(4, "Siège Social");
            ps.setBoolean(5, true);
            ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            ps.setDouble(7, 0.0);

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("Impossible de créer la franchise Siège");
    }
}