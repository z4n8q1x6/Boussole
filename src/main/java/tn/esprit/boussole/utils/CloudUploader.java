package tn.esprit.boussole.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.Base64;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CloudUploader {

  private static final String CLOUDINARY_URL_ENV = System.getenv("CLOUDINARY_URL");
  private static String uploadUrl;
  private static String apiKey;
  private static String apiSecret;

  static {
    if (CLOUDINARY_URL_ENV != null && !CLOUDINARY_URL_ENV.isEmpty()) {
      parseCloudinaryUrl(CLOUDINARY_URL_ENV);
    } else {
      System.err.println("WARNING: CLOUDINARY_URL environment variable not set!");
      System.err.println("Please set it before running the application.");
      System.err.println("Format: cloudinary://api_key:api_secret@cloud_name");
    }
  }

  private static void parseCloudinaryUrl(String cloudinaryUrl) {
    try {
      String url = cloudinaryUrl;
      if (url.startsWith("cloudinary://")) {
        url = url.substring("cloudinary://".length());
      }

      String[] parts = url.split("@");
      if (parts.length != 2) {
        throw new IllegalArgumentException("Invalid CLOUDINARY_URL format");
      }

      String credentials = parts[0];
      String cloudName = parts[1];

      String[] creds = credentials.split(":");
      if (creds.length != 2) {
        throw new IllegalArgumentException("Invalid credentials in CLOUDINARY_URL");
      }

      apiKey = creds[0];
      apiSecret = creds[1];

      // raw/upload for PDFs — extension is preserved in the URL
      // as long as the filename in the multipart body includes ".pdf"
      uploadUrl = "https://api.cloudinary.com/v1_1/" + cloudName + "/raw/upload";

      System.out.println("✓ Cloudinary configured successfully");
      System.out.println("  Cloud: " + cloudName);
      System.out.println("  Upload URL: " + uploadUrl);
    } catch (Exception e) {
      System.err.println("ERROR parsing CLOUDINARY_URL: " + e.getMessage());
      e.printStackTrace();
    }
  }

  public static String uploadToCloudinary(File file) {
    if (uploadUrl == null || apiKey == null || apiSecret == null) {
      System.err.println(
          "ERROR: Cloudinary not configured. Set CLOUDINARY_URL environment variable.");
      return null;
    }

    try {
      OkHttpClient client = new OkHttpClient();

      String auth = apiKey + ":" + apiSecret;
      String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

      // Strip any extension — Cloudinary uses the multipart filename as
      // the public_id. Including ".pdf" puts it literally in the URL and
      // breaks Google Docs viewer. The working Symfony URLs have no extension.
      String rawName = file.getName();
      int dotIndex = rawName.lastIndexOf('.');
      String fileName = (dotIndex > 0) ? rawName.substring(0, dotIndex) : rawName;

      RequestBody fileBody = RequestBody.create(file, MediaType.parse("application/pdf"));

      MultipartBody.Builder bodyBuilder =
          new MultipartBody.Builder()
              .setType(MultipartBody.FORM)
              .addFormDataPart("file", fileName, fileBody)
              .addFormDataPart("resource_type", "raw")
              .addFormDataPart("folder", "boussole/reports");

      RequestBody body = bodyBuilder.build();

      Request request =
          new Request.Builder()
              .url(uploadUrl)
              .header("Authorization", "Basic " + encodedAuth)
              .post(body)
              .build();

      try (Response response = client.newCall(request).execute()) {
        if (!response.isSuccessful()) {
          String errorBody = response.body() != null ? response.body().string() : "No error body";
          System.err.println(
              "Cloudinary Upload Failed (HTTP " + response.code() + "): " + errorBody);
          return null;
        }

        String responseBody = response.body().string();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(responseBody);

        if (rootNode.has("secure_url")) {
          String secureUrl = rootNode.get("secure_url").asText();
          System.out.println("✓ PDF uploaded successfully: " + secureUrl);
          return secureUrl;
        } else if (rootNode.has("error")) {
          System.err.println("Cloudinary error: " + rootNode.get("error").asText());
          return null;
        } else {
          System.err.println("Unexpected response from Cloudinary: " + responseBody);
          return null;
        }
      }
    } catch (Exception e) {
      System.err.println("Error uploading to Cloudinary: " + e.getMessage());
      e.printStackTrace();
      return null;
    }
  }
}
