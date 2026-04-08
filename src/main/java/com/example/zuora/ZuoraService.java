package com.example.zuora;

import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Service
public class ZuoraService {

    // Configuration - could be moved to application.properties
    private static final String ZUORA_BASE_URL = "https://rest.apisandbox.zuora.com";
    private static final String CLIENT_ID = "205a9df2-2abb-4770-8868-2dbc599da792";
    private static final String CLIENT_SECRET = "2O3tyQGKfYvQdzfhOlfytObxo1xgjIkShRM1CejGn";
    private static final String DB_URL = "jdbc:sqlite:zuora_db.sqlite3";

    private final HttpClient httpClient;
    private String accessToken;

    public ZuoraService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        // Load SQLite driver
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC driver not found");
        }
    }

    public String createCustomerAccount(CustomerForm form) throws Exception {
        authenticate();
        String apiUrl = ZUORA_BASE_URL + "/v1/accounts";

        String jsonBody = buildCustomerJson(form);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Authorization", "Bearer " + this.accessToken)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        String responseBody = response.body();
        storeResponseInDb(responseBody);
        return responseBody;
    }

    private void authenticate() throws Exception {
        if (this.accessToken != null) {
            return; // Already authenticated
        }
        String authUrl = ZUORA_BASE_URL + "/oauth/token";

        String formData = "grant_type=client_credentials&client_id=" + CLIENT_ID
                + "&client_secret=" + CLIENT_SECRET;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(authUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Authentication failed: HTTP " + response.statusCode()
                    + " - " + response.body());
        }

        String responseBody = response.body();
        this.accessToken = extractJsonValue(responseBody, "access_token");
        System.out.println("Access Token obtained: " + this.accessToken.substring(0, Math.min(this.accessToken.length(), 20)) + "...");
    }

    private String buildCustomerJson(CustomerForm form) {
        // Build JSON payload from form data
        return String.format("""
            {
                "name": "%s",
                "currency": "%s",
                "billToContact": {
                    "firstName": "%s",
                    "lastName": "%s",
                    "workEmail": "%s",
                    "workPhone": "%s",
                    "address1": "%s",
                    "address2": "%s",
                    "city": "%s",
                    "state": "%s",
                    "country": "%s",
                    "zipCode": "%s"
                },
                "soldToContact": {
                    "firstName": "%s",
                    "lastName": "%s",
                    "workEmail": "%s",
                    "workPhone": "%s",
                    "address1": "%s",
                    "address2": "%s",
                    "city": "%s",
                    "state": "%s",
                    "country": "%s",
                    "zipCode": "%s"
                },
                "paymentTerm": "%s",
                "batch": "%s",
                "billCycleDay": %d,
                "autoPay": %b,
                "notes": "%s",
                "ConsumerType__c": "%s"
            }
            """,
            form.getName(),
            form.getCurrency(),
            form.getBillToFirstName(),
            form.getBillToLastName(),
            form.getBillToEmail(),
            form.getBillToPhone(),
            form.getBillToAddress1(),
            form.getBillToAddress2() != null ? form.getBillToAddress2() : "",
            form.getBillToCity(),
            form.getBillToState(),
            form.getBillToCountry(),
            form.getBillToZipCode(),
            form.getSoldToFirstName(),
            form.getSoldToLastName(),
            form.getSoldToEmail(),
            form.getSoldToPhone(),
            form.getSoldToAddress1(),
            form.getSoldToAddress2() != null ? form.getSoldToAddress2() : "",
            form.getSoldToCity(),
            form.getSoldToState(),
            form.getSoldToCountry(),
            form.getSoldToZipCode(),
            form.getPaymentTerm(),
            form.getBatch(),
            form.getBillCycleDay(),
            form.isAutoPay(),
            form.getNotes(),
            form.getConsumerType()
        );
    }

    private void storeResponseInDb(String jsonResponse) {
        // Extract values from response
        String success = extractJsonValueOptional(jsonResponse, "success");
        String processId = extractJsonValueOptional(jsonResponse, "processId");
        String requestId = extractJsonValueOptional(jsonResponse, "requestId");
        String accountId = extractJsonValueOptional(jsonResponse, "id");
        String accountNumber = extractJsonValueOptional(jsonResponse, "accountNumber");

        // If account creation failed, extract error code and message
        String errorCode = "";
        String errorMessage = "";
        if ("false".equals(success)) {
            errorCode = extractJsonValueOptional(jsonResponse, "code");
            errorMessage = extractJsonValueOptional(jsonResponse, "message");
        }

        // Store in database
        String sql = """
            INSERT INTO zuora_api_responses
            (process_id, request_id, account_id, account_number, success, error_code, error_message, raw_response, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            """;

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, processId);
            stmt.setString(2, requestId);
            stmt.setString(3, accountId);
            stmt.setString(4, accountNumber);
            stmt.setString(5, success);
            stmt.setString(6, errorCode);
            stmt.setString(7, errorMessage);
            stmt.setString(8, jsonResponse);

            stmt.executeUpdate();
            System.out.println("Stored in DB - Process ID: " + processId + ", Success: " + success);

        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            throw new RuntimeException("Failed to store response in database", e);
        }
    }

    /**
     * Extract JSON value - optional field
     */
    private String extractJsonValueOptional(String json, String key) {
        // Try string value
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
        java.util.regex.Pattern r = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = r.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        // Try boolean value
        String boolPattern = "\"" + key + "\"\\s*:\\s*(true|false)";
        java.util.regex.Pattern r2 = java.util.regex.Pattern.compile(boolPattern);
        java.util.regex.Matcher m2 = r2.matcher(json);
        if (m2.find()) {
            return m2.group(1);
        }
        return "";
    }

    /**
     * Extract JSON value - required field
     */
    private String extractJsonValue(String json, String key) {
        String value = extractJsonValueOptional(json, key);
        if (value.isEmpty()) {
            throw new RuntimeException("Could not extract '" + key + "' from JSON response");
        }
        return value;
    }
}