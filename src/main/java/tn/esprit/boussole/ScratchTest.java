package tn.esprit.boussole;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import tn.esprit.boussole.utils.MyBdConnexion;

public class ScratchTest {
    public static void main(String[] args) {
        try {
            Connection conn = MyBdConnexion.getinstance().getCnx();
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT email, face_token FROM utilisateur WHERE face_token IS NOT NULL AND face_token != ''");
            
            System.out.println("=== USERS WITH FACE TOKENS IN DATABASE ===");
            int count = 0;
            while (rs.next()) {
                count++;
                String email = rs.getString("email");
                String token = rs.getString("face_token");
                System.out.println("Email: " + email);
                System.out.println("Token: [" + token + "]");
                System.out.println("Length: " + (token != null ? token.length() : "null"));
                System.out.println("---");
            }
            if (count == 0) {
                System.out.println("No users with face_token found in the database!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
