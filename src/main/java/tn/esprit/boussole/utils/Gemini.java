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
  private static String[] myModels = {
    "gemini-3-flash-preview", "gemini-2.5-flash", "gemini-2.5-flash-lite"
  };

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
                    Schema.builder().type(Known.STRING).minItems(1L).maxLength(35L).build(),
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

    // Simulated data variables (will be replaced with DB queries after integration)
    String franchiseName = "Tunis Downtown";
    int year = 2024;
    int month = 2;
    double totalRecettes = 450000.00;
    double totalChargesExploitation = 180000.00;
    double totalChargesFinanciere = 45000.00;
    double totalChargesExceptionnelle = 15000.00;
    double resultatNet =
        totalRecettes
            - (totalChargesExploitation + totalChargesFinanciere + totalChargesExceptionnelle);
    double soldeActuel = 120000.00;
    double budgetRevenuCible = 500000.00;
    double budgetDepenseLimite = 250000.00;
    double totalCharges =
        totalChargesExploitation + totalChargesFinanciere + totalChargesExceptionnelle;
    double variance = ((totalRecettes - budgetRevenuCible) / budgetRevenuCible) * 100;
    int unpaidRedevancesCount = 2;
    double unpaidRedevancesAmount = 25000.00;
    int pendingChargesCount = 5;
    int rejectedChargesCount = 1;
    int transactionCount = 47;
    double recettesDepensesRatio = totalRecettes / totalCharges;

    String prompt =
        String.format(
            """
            Analyse les données financières de la franchise %s pour %s %d:

            BILAN MENSUEL:
            - Chiffre d'affaires: %,.2f TND
            - Charges opérationnelles: %,.2f TND
            - Charges financières: %,.2f TND
            - Charges exceptionnelles: %,.2f TND
            - Résultat net: %,.2f TND
            - Solde actuel en caisse: %,.2f TND

            BUDGET PRÉVISIONNEL:
            - Objectif revenu: %,.2f TND (vs réel: %,.2f TND)
            - Limite dépenses: %,.2f TND (vs réel: %,.2f TND)
            - Variance: %+.1f%%

            OBLIGATIONS FINANCIÈRES:
            - Redevances impayées: %d (montant total: %,.2f TND)
            - Charges en attente de validation: %d
            - Charges rejetées: %d

            TRANSACTIONS RÉCENTES:
            - Nombre de transactions ce mois: %d
            - Ratio recettes/dépenses: %.2f

            Détecte les anomalies, risques financiers ou alertes critiques.
            Formule une alerte spécifique et sérieuse basée sur ces données.
            """,
            franchiseName,
            month,
            year,
            totalRecettes,
            totalChargesExploitation,
            totalChargesFinanciere,
            totalChargesExceptionnelle,
            resultatNet,
            soldeActuel,
            budgetRevenuCible,
            totalRecettes,
            budgetDepenseLimite,
            totalCharges,
            variance,
            unpaidRedevancesCount,
            unpaidRedevancesAmount,
            pendingChargesCount,
            rejectedChargesCount,
            transactionCount,
            recettesDepensesRatio);

    AlerteIA alerteIA = new AlerteIA();
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

  public static Optional<String> generateAdvice(String prompt) {
    String apiKey = Config.get("GEMINI_API");
    if (apiKey == null) {
      System.err.println("GEMINI_API_KEY not set");
      return Optional.empty();
    }

    Client client = Client.builder().apiKey(apiKey).build();
    int i = 0;
    while (i < myModels.length) {
      try {
        GenerateContentResponse response = client.models.generateContent(myModels[i], prompt, null);
        // DEBUG
        System.out.println(myModels[i]);
        System.out.println(response.text());
        client.close();
        return Optional.of(response.text());
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
