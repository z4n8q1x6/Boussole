package com.boussole.app.utils;

import com.boussole.app.models.AlerteIA;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.genai.Client;
// import com.google.genai.errors.ServerException;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Schema;
import com.google.genai.types.Type.Known;

public class Gemini {
  // TODO: make request async
  public static AlerteIA generate_alerte() {
    // Uses GOOGLE_API_KEY env var
    Client client = new Client();
    // Client client = Client.builder().apiKey("API_KEY").build();

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
            .temperature(0.75f)
            .build();

    String prompt =
        """
        Génère une alerte de sécurité avec ces champs:
        - type_alerte: Une catégorie courte
        - message: Une description détaillée
        - score_gravite: Score de sévérité
        """;
    String[] modelToUse = {"gemini-3-flash-preview", "gemini-2.5-flash", "gemini-2.5-flash-lite"};

    GenerateContentResponse response = client.models.generateContent(modelToUse[0], prompt, config);

    // DEBUG
    System.out.println(response.text());

    ObjectMapper mapper = new ObjectMapper();
    AlerteIA alerteIA = new AlerteIA();
    try {
      alerteIA = mapper.readValue(response.text(), AlerteIA.class);
    } catch (JsonMappingException e) {
      System.err.println("Error mapping json: " + e);
    } catch (JsonProcessingException e) {
      System.err.println("Error processing json: " + e);
    }

    client.close();
    return alerteIA;
  }
}

// FORMAT:
// {
// type_alerte : String;
// message : string;
// score_gravite: float;
// }
//
