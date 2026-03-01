package tn.esprit.boussole.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.genai.Client;
import com.google.genai.errors.ClientException;
import com.google.genai.errors.ServerException;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Schema;
import com.google.genai.types.Type.Known;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.Calendar;
import java.util.Optional;
import tn.esprit.boussole.models.AlerteIA;

public class Gemini {
  private static String[] myModels = {
    "gemini-3-flash-preview", "gemini-2.5-flash", "gemini-2.5-flash-lite"
  };

  public static Optional<AlerteIA> generate_alerte() {
    Calendar cal = Calendar.getInstance();
    int month = cal.get(Calendar.MONTH) + 1;
    int year = cal.get(Calendar.YEAR);
    
    Dotenv dotenv = Dotenv.load();
    String apiKey = dotenv.get("GEMINI_API_KEY");

    if (apiKey == null || apiKey.isEmpty()) {
      System.err.println("GEMINI_API key not found in .env file.");
      return Optional.empty();
    }

    FinancialDataHelper.FinancialData data = FinancialDataHelper.getFinancialData(1, month, year);
    if (data == null) {
      System.err.println("Failed to fetch financial data");
      return Optional.empty();
    }

    Client client = Client.builder().apiKey(apiKey).build();

    Schema schema = Schema.builder()
        .properties(ImmutableMap.of(
            "type_alerte", Schema.builder().type(Known.STRING).minItems(1L).maxLength(35L).build(),
            "message", Schema.builder().type(Known.STRING).minLength(100L).maxLength(1000L).build(),
            "score_gravite", Schema.builder().type(Known.NUMBER).minimum(0.0).maximum(10.0).build()))
        .type(Known.OBJECT)
        .build();

    GenerateContentConfig config = GenerateContentConfig.builder()
        .responseMimeType("application/json")
        .candidateCount(1)
        .responseSchema(schema)
        .temperature(1.75f)
        .build();

    String prompt = buildPrompt(data, month, year);

    AlerteIA alerteIA = new AlerteIA();
    for (int i = 0; i < myModels.length; i++) {
      try {
        GenerateContentResponse response = client.models.generateContent(myModels[i], prompt, config);
        System.out.println(myModels[i]);
        System.out.println(response.text());

        ObjectMapper mapper = new ObjectMapper();
        try {
          alerteIA = mapper.readValue(response.text(), AlerteIA.class);
        } catch (JsonMappingException e) {
          System.err.println("Error mapping json: " + e);
        } catch (JsonProcessingException e) {
          System.err.println("Error processing json: " + e);
        }
        client.close();
        return Optional.of(alerteIA);
      } catch (ServerException e) {
        System.err.println("Failed to generate content: " + e);
      } catch (ClientException e) {
        System.err.println("Failed to generate content: " + e);
        break;
      }
    }
    client.close();
    return Optional.empty();
  }

  private static String buildPrompt(FinancialDataHelper.FinancialData data, int month, int year) {
    return String.format(
        "Analyse les données financières pour %d/%d:\n\n" +
        "RÉSULTATS FINANCIERS:\n" +
        "- Chiffre d'affaires: %.2f TND\n" +
        "- Charges opérationnelles: %.2f TND\n" +
        "- Charges financières: %.2f TND\n" +
        "- Charges exceptionnelles: %.2f TND\n" +
        "- Résultat net: %.2f TND\n" +
        "- Solde actuel: %.2f TND\n\n" +
        "STATUT DES CHARGES:\n" +
        "- Charges en attente: %d\n" +
        "- Charges rejetées: %d\n\n" +
        "ACTIVITÉ:\n" +
        "- Transactions ce mois: %d\n\n" +
        "Détecte les anomalies et risques financiers. Formule une alerte basée sur ces données.",
        month, year, data.totalRecettes, data.totalChargesExploitation,
        data.totalChargesFinanciere, data.totalChargesExceptionnelle,
        data.resultatNet, data.soldeActuel, data.pendingChargesCount,
        data.rejectedChargesCount, data.transactionCount);
  }

  public static Optional<String> generateAdvice(String prompt) {
    Dotenv dotenv = Dotenv.load();
    String apiKey = dotenv.get("GEMINI_API_KEY");

    if (apiKey == null || apiKey.isEmpty()) {
      System.err.println("GEMINI_API key not found in .env file.");
      return Optional.empty();
    }

    Client client = Client.builder().apiKey(apiKey).build();
    for (int i = 0; i < myModels.length; i++) {
      try {
        GenerateContentResponse response = client.models.generateContent(myModels[i], prompt, null);
        System.out.println(myModels[i]);
        System.out.println(response.text());
        client.close();
        return Optional.of(response.text());
      } catch (ServerException e) {
        System.err.println("Failed to generate content: " + e);
      } catch (ClientException e) {
        System.err.println("Failed to generate content: " + e);
        break;
      }
    }
    return Optional.empty();
  }
}
