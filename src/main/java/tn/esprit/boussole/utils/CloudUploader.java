package tn.esprit.boussole.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.Base64;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
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
      OkHttpClient client = createSecureHttpClient();

      String auth = apiKey + ":" + apiSecret;
      String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

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

  public static boolean deleteFromCloudinary(String secureUrl) {
    if (uploadUrl == null || apiKey == null || apiSecret == null) {
      System.err.println(
          "ERROR: Cloudinary not configured. Set CLOUDINARY_URL environment variable.");
      return false;
    }

    try {
      System.out.println("🔍 DELETE: Attempting to delete from URL: " + secureUrl);

      String publicId = extractPublicId(secureUrl);
      if (publicId == null || publicId.isEmpty()) {
        System.err.println("ERROR: Could not extract public_id from URL: " + secureUrl);
        return false;
      }

      System.out.println("🔍 DELETE: Extracted public_id: " + publicId);

      OkHttpClient client = createSecureHttpClient();

      String auth = apiKey + ":" + apiSecret;
      String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

      String cloudName = extractCloudName(uploadUrl);
      System.out.println("🔍 DELETE: Cloud name: " + cloudName);

      String deleteUrl = "https://api.cloudinary.com/v1_1/" + cloudName + "/raw/destroy";
      System.out.println("🔍 DELETE: Delete URL: " + deleteUrl);

      RequestBody body =
          new MultipartBody.Builder()
              .setType(MultipartBody.FORM)
              .addFormDataPart("public_id", publicId)
              .build();

      Request request =
          new Request.Builder()
              .url(deleteUrl)
              .header("Authorization", "Basic " + encodedAuth)
              .post(body)
              .build();

      System.out.println("🔍 DELETE: Sending request...");

      try (Response response = client.newCall(request).execute()) {
        System.out.println("🔍 DELETE: Response code: " + response.code());

        if (!response.isSuccessful()) {
          String errorBody = response.body() != null ? response.body().string() : "No error body";
          System.err.println(
              "Cloudinary Delete Failed (HTTP " + response.code() + "): " + errorBody);
          return false;
        }

        String responseBody = response.body().string();
        System.out.println("🔍 DELETE: Response body: " + responseBody);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(responseBody);

        if (rootNode.has("result") && rootNode.get("result").asText().equals("ok")) {
          System.out.println("✓ PDF deleted successfully from Cloudinary: " + publicId);
          return true;
        } else {
          System.err.println("Cloudinary delete response: " + responseBody);
          return false;
        }
      }
    } catch (Exception e) {
      System.err.println("Error deleting from Cloudinary: " + e.getMessage());
      e.printStackTrace();
      return false;
    }
  }

  private static String extractPublicId(String secureUrl) {
    try {
      int uploadIndex = secureUrl.indexOf("/upload/");
      if (uploadIndex == -1) {
        System.out.println("🔍 DEBUG: /upload/ not found in URL");
        return null;
      }

      String afterUpload = secureUrl.substring(uploadIndex + "/upload/".length());
      System.out.println("🔍 DEBUG: After /upload/: " + afterUpload);

      int slashIndex = afterUpload.indexOf('/');
      if (slashIndex == -1) {
        System.out.println("🔍 DEBUG: No slash found after version");
        return null;
      }

      String publicId = afterUpload.substring(slashIndex + 1);
      System.out.println("🔍 DEBUG: Public ID before trimming: " + publicId);

      if (publicId.endsWith(".pdf")) {
        publicId = publicId.substring(0, publicId.length() - 4);
      }

      publicId = java.net.URLDecoder.decode(publicId, "UTF-8");

      System.out.println("🔍 DEBUG: Final public ID: " + publicId);
      return publicId;
    } catch (Exception e) {
      System.err.println("Error extracting public_id: " + e.getMessage());
      e.printStackTrace();
      return null;
    }
  }

  private static String extractCloudName(String url) {
    try {
      String[] parts = url.split("/");
      for (int i = 0; i < parts.length; i++) {
        if (parts[i].equals("v1_1") && i + 1 < parts.length) {
          return parts[i + 1];
        }
      }
      return null;
    } catch (Exception e) {
      System.err.println("Error extracting cloud name: " + e.getMessage());
      return null;
    }
  }

  private static OkHttpClient createSecureHttpClient() {
    try {
      String keyStorePath = findCertificatePath();

      if (keyStorePath != null) {
        System.out.println("✓ Using certificate: " + keyStorePath);
        KeyStore keyStore = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(keyStorePath)) {
          keyStore.load(fis, null);
        }

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
        tmf.init(keyStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), null);

        return new OkHttpClient.Builder().sslSocketFactory(sslContext.getSocketFactory()).build();
      } else {
        System.out.println("⚠ No certificate bundle found, using default SSL context");
        return new OkHttpClient();
      }
    } catch (Exception e) {
      System.err.println("Error creating secure HTTP client: " + e.getMessage());
      e.printStackTrace();
      return new OkHttpClient();
    }
  }

  private static String findCertificatePath() {
    String[] candidates = {
      System.getenv("CLOUDINARY_CA_BUNDLE"),
      System.getProperty("javax.net.ssl.trustStore"),
      System.getProperty("java.home") + "/lib/security/cacerts",
      "cacert.pem",
      "C:/wamp64/cacert.pem",
      "C:/xampp/apache/bin/curl-ca-bundle.crt",
      "C:/xampp/php/extras/ssl/cacert.pem",
    };

    for (String candidate : candidates) {
      if (candidate != null && !candidate.trim().isEmpty()) {
        File certFile = new File(candidate.trim());
        if (certFile.isFile()) {
          return candidate.trim();
        }
      }
    }

    return null;
  }
}
