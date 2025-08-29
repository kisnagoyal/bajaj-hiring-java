package com.example.bajaj_hiring.service;

import com.example.bajaj_hiring.model.*;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class HiringService {

    private final RestTemplate restTemplate = new RestTemplate();

    public void startProcess() {
        // Step 1: Call generateWebhook
        GenerateWebhookRequest request = new GenerateWebhookRequest(
                "Kisna Goyal", "22BCE2367", "kisna2goyal@gmail.com"
        );

        ResponseEntity<GenerateWebhookResponse> response = restTemplate.postForEntity(
                "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA",
                request,
                GenerateWebhookResponse.class
        );

        GenerateWebhookResponse webhookResponse = response.getBody();
        System.out.println("Webhook: " + webhookResponse.getWebhook());
        System.out.println("Access Token: " + webhookResponse.getAccessToken());

        // Step 2: Prepare SQL query (dummy example here, replace with actual answer)
        FinalQueryRequest finalRequest = new FinalQueryRequest(
                "SELECT * FROM employees;"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", webhookResponse.getAccessToken());

        HttpEntity<FinalQueryRequest> entity = new HttpEntity<>(finalRequest, headers);

        ResponseEntity<String> submitResponse = restTemplate.postForEntity(
                webhookResponse.getWebhook(),
                entity,
                String.class
        );

        System.out.println("Submission Response: " + submitResponse.getBody());
    }
}
