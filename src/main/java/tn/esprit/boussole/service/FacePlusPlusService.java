package tn.esprit.boussole.service;

import io.github.cdimascio.dotenv.Dotenv;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Base64;

public class FacePlusPlusService {

    private static final Dotenv dotenv = Dotenv.load();
    private static final String API_KEY = dotenv.get("FACEPP_API_KEY");
    private static final String API_SECRET = dotenv.get("FACEPP_API_SECRET");
    private static final String FACESET_TOKEN = "boussole_users_faceset";

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

    public String searchFace(byte[] imageBytes) throws IOException {
        // Pour la recherche, on suppose que le FaceSet existe déjà pour gagner du temps.
        // Si la recherche échoue avec "INVALID_OUTER_ID", alors on essaiera de le créer.
        
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        RequestBody formBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("api_key", API_KEY)
                .addFormDataPart("api_secret", API_SECRET)
                .addFormDataPart("image_base64", base64Image)
                .addFormDataPart("outer_id", FACESET_TOKEN)
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
                JSONObject bestMatch = results.getJSONObject(0);
                double confidence = bestMatch.getDouble("confidence");
                
                if (confidence > 80.0) {
                    return bestMatch.getString("face_token");
                }
            }
            return null;
        }
    }
}
