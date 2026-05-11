package tn.esprit.boussole.service;

import io.github.cdimascio.dotenv.Dotenv;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Base64;

public class FacePlusPlusService {

    private static final Dotenv dotenv = Dotenv.load();
    private static final String API_KEY = "5hG5su943EpQmUSJY_hH2qhtFjACjf7K";
    private static final String API_SECRET = "NsmAiKTb8JDKlUMbMWg1DFSo0Fef3QK8";
    private static final String FACESET_TOKEN = "boussole_faceset"; // Modifié pour correspondre au .env de Symfony

    private final OkHttpClient client = new OkHttpClient();

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public String detectFace(byte[] imageBytes) throws IOException {
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        RequestBody formBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("api_key", API_KEY)
                .addFormDataPart("api_secret", API_SECRET)
                .addFormDataPart("image_base64", base64Image)
                .build();

        Request request = new Request.Builder()
                .url("https://api-us.faceplusplus.com/facepp/v3/detect")
                .post(formBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String jsonData = response.body().string();
            if (!response.isSuccessful()) throw new IOException("Erreur API Face++ (Detect): " + jsonData);

            JSONObject jsonObject = new JSONObject(jsonData);
            JSONArray faces = jsonObject.optJSONArray("faces");

            if (faces != null && faces.length() > 0) {
                return faces.getJSONObject(0).getString("face_token");
            }
            return null;
        }
    }

    public boolean ensureFaceSetExists() {
        // Pause préventive pour éviter la saturation
        sleep(1100); 

        RequestBody checkBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("api_key", API_KEY)
                .addFormDataPart("api_secret", API_SECRET)
                .addFormDataPart("outer_id", FACESET_TOKEN)
                .build();

        Request checkRequest = new Request.Builder()
                .url("https://api-us.faceplusplus.com/facepp/v3/faceset/getdetail")
                .post(checkBody)
                .build();

        try (Response response = client.newCall(checkRequest).execute()) {
            if (response.isSuccessful()) {
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        // Si on doit créer, on attend encore un peu
        sleep(1100);

        RequestBody createBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("api_key", API_KEY)
                .addFormDataPart("api_secret", API_SECRET)
                .addFormDataPart("outer_id", FACESET_TOKEN)
                .addFormDataPart("display_name", "Utilisateurs Boussole")
                .build();

        Request createRequest = new Request.Builder()
                .url("https://api-us.faceplusplus.com/facepp/v3/faceset/create")
                .post(createBody)
                .build();

        try (Response response = client.newCall(createRequest).execute()) {
            String body = response.body().string();
            if (response.isSuccessful()) {
                System.out.println("FaceSet créé avec succès.");
                return true;
            } else {
                System.err.println("Erreur création FaceSet: " + body);
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void addFaceToSet(String faceToken) throws IOException {
        if (!ensureFaceSetExists()) {
            throw new IOException("Impossible d'accéder ou de créer le FaceSet (Vérifiez les limites de l'API).");
        }

        // Pause avant l'ajout effectif
        sleep(1100);

        RequestBody formBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("api_key", API_KEY)
                .addFormDataPart("api_secret", API_SECRET)
                .addFormDataPart("outer_id", FACESET_TOKEN)
                .addFormDataPart("face_tokens", faceToken)
                .build();

        Request request = new Request.Builder()
                .url("https://api-us.faceplusplus.com/facepp/v3/faceset/addface")
                .post(formBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String body = response.body().string();
            if (!response.isSuccessful()) throw new IOException("Erreur API Face++ (AddFace): " + body);
        }
    }

    public void clearFaceSet() {
        RequestBody formBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("api_key", API_KEY)
                .addFormDataPart("api_secret", API_SECRET)
                .addFormDataPart("outer_id", FACESET_TOKEN)
                .addFormDataPart("face_tokens", "RemoveAllFaceTokens")
                .build();

        Request request = new Request.Builder()
                .url("https://api-us.faceplusplus.com/facepp/v3/faceset/removeface")
                .post(formBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                System.out.println("FaceSet complètement vidé (nettoyage réussi).");
            } else {
                System.err.println("Erreur vidage FaceSet : " + response.body().string());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void syncLocalDatabaseTokensToFaceSet() {
        ensureFaceSetExists();
        
        java.util.List<String> tokensToSync = new java.util.ArrayList<>();
        String sql = "SELECT face_token FROM utilisateur WHERE face_token IS NOT NULL AND face_token != ''";
        try (java.sql.PreparedStatement ps = tn.esprit.boussole.utils.MyBdConnexion.getinstance().getCnx().prepareStatement(sql);
             java.sql.ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                tokensToSync.add(rs.getString("face_token").trim());
            }
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }

        if (tokensToSync.isEmpty()) {
            System.out.println("Aucun token en BDD à synchroniser.");
            return;
        }

        System.out.println("Synchronisation de " + tokensToSync.size() + " tokens vers Face++...");
        
        for (String token : tokensToSync) {
            try {
                RequestBody formBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("api_key", API_KEY)
                        .addFormDataPart("api_secret", API_SECRET)
                        .addFormDataPart("outer_id", FACESET_TOKEN)
                        .addFormDataPart("face_tokens", token)
                        .build();

                Request request = new Request.Builder()
                        .url("https://api-us.faceplusplus.com/facepp/v3/faceset/addface")
                        .post(formBody)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    System.out.println("Sync token " + token + " -> " + response.isSuccessful());
                }
                
                // Pause pour la limite API Gratuite (1 requête/seconde)
                Thread.sleep(1100);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("Synchronisation Java terminée !");
    }

    public java.util.List<String> searchFace(byte[] imageBytes) throws IOException {
        // Pour la recherche, on suppose que le FaceSet existe déjà pour gagner du temps.
        // Si la recherche échoue avec "INVALID_OUTER_ID", alors on essaiera de le créer.
        
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        RequestBody formBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("api_key", API_KEY)
                .addFormDataPart("api_secret", API_SECRET)
                .addFormDataPart("image_base64", base64Image)
                .addFormDataPart("outer_id", FACESET_TOKEN)
                .addFormDataPart("return_result_count", "5")
                .build();

        Request request = new Request.Builder()
                .url("https://api-us.faceplusplus.com/facepp/v3/search")
                .post(formBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String jsonData = response.body().string();
            
            if (!response.isSuccessful()) {
                // Si le FaceSet n'existe pas, on le crée et on réessaie (une seule fois)
                if (jsonData.contains("INVALID_OUTER_ID")) {
                    if (ensureFaceSetExists()) {
                        sleep(1100); // Pause avant de réessayer
                        return searchFace(imageBytes); // Récursion (une fois max en théorie)
                    }
                }
                if (jsonData.contains("EMPTY_FACESET")) {
                    return null;
                }
                throw new IOException("Erreur API Face++ (Search): " + jsonData);
            }

            JSONObject jsonObject = new JSONObject(jsonData);
            JSONArray results = jsonObject.optJSONArray("results");

            if (results != null && results.length() > 0) {
                java.util.List<String> matchedTokens = new java.util.ArrayList<>();
                for (int i = 0; i < results.length(); i++) {
                    JSONObject match = results.getJSONObject(i);
                    if (match.getDouble("confidence") > 80.0) {
                        matchedTokens.add(match.getString("face_token"));
                    }
                }
                if (!matchedTokens.isEmpty()) {
                    return matchedTokens;
                }
            }
            return null;
        }
    }
}
