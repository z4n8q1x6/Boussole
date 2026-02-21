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
import java.util.Optional;
import tn.esprit.boussole.models.AlerteIA;

public class Gemini {
  public static Optional<AlerteIA> generate_alerte() {
    // Client client = new Client(); // Uses GOOGLE_API_KEY env var
    String apiKey = Config.get("GEMINI_API");
    if (apiKey == null) {
      System.err.println("GEMINI_API_KEY not set.");
      return Optional.empty();
    }
    Client client = Client.builder().apiKey(apiKey).build();

    Schema schema =
        Schema.builder()
            .properties(
                ImmutableMap.of(
                    "type_alerte",
                    Schema.builder().type(Known.STRING).minItems(1L).maxLength(50L).build(),
                    "message",
                    Schema.builder().type(Known.STRING).minLength(100L).maxLength(1000L).build(),
                    "score_gravite",
                    Schema.builder().type(Known.NUMBER).minimum(0.0).maximum(10.0).build()))
            .type(Known.OBJECT)
            .build();

    GenerateContentConfig config =
        GenerateContentConfig.builder()
            .responseMimeType("application/json")
            .candidateCount(1)
            .responseSchema(schema)
            .temperature(1.75f)
            .build();

    double totalCharges = 98500.00;
    double totalRecettes = 74200.00;
    // double totalCharges = 1250000.00;
    // double totalRecettes = 1487500.50;
    String prompt =
        String.format(
            """
            Analyse ces données financières:
            - Charges totales: %,.2f TND
            - Recettes totales: %,.2f TND
            - Résultat net: %,.2f TND
            - Détecte les anomalies ou alertes critiques
            """,
            totalCharges, totalRecettes, (totalRecettes - totalCharges));

    AlerteIA alerteIA = new AlerteIA();
    String[] myModels = {"gemini-3-flash-preview", "gemini-2.5-flash", "gemini-2.5-flash-lite"};
    int i = 0;
    while (i < myModels.length) {
      try {
        GenerateContentResponse response =
            client.models.generateContent(myModels[i], prompt, config);
        // DEBUG
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
        // no more tokens for that model or model overloaded... -> try next model
        System.err.println("Failed to generate content: " + e);
        i++;
      } catch (ClientException e) {
        // problem with client api/model_name... -> no retries
        System.err.println("Failed to generate content: " + e);
        break;
      }
    }
    client.close();
    return Optional.empty();
  }
}

// FORMAT (in json):
// {
// type_alerte : String;
// message : string;
// score_gravite: float;
// }
//
