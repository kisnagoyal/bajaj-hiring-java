package com.example.bajaj_hiring;

import com.example.bajaj_hiring.model.FinalQueryRequest;
import com.example.bajaj_hiring.model.GenerateWebhookRequest;
import com.example.bajaj_hiring.model.GenerateWebhookResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;

@Component
public class StartupRunner implements ApplicationRunner {

    private final RestTemplate restTemplate;
    private final JdbcTemplate jdbcTemplate;

    @Value("${student.name}")
    private String name;

    @Value("${student.regNo}")
    private String regNo;

    @Value("${student.email}")
    private String email;

    @Value("${api.generateWebhook}")
    private String generateWebhookUrl;

    public StartupRunner(RestTemplate restTemplate, JdbcTemplate jdbcTemplate) {
        this.restTemplate = restTemplate;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try {
            // 1) Call generateWebhook
            GenerateWebhookRequest req = new GenerateWebhookRequest(name, regNo, email);
            ResponseEntity<GenerateWebhookResponse> res = restTemplate.postForEntity(
                    generateWebhookUrl, req, GenerateWebhookResponse.class);

            if (!res.getStatusCode().is2xxSuccessful() || res.getBody() == null) {
                throw new IllegalStateException("generateWebhook failed: " + res);
            }

            String webhookUrl = res.getBody().getWebhook();
            String accessToken = res.getBody().getAccessToken();
            System.out.println("[startup] webhook=" + webhookUrl);
            System.out.println("[startup] accessToken(len)=" + (accessToken == null ? 0 : accessToken.length()));

            // 2) Decide question based on last two digits of regNo
            String digits = regNo.replaceAll("\\D", "");
            if (digits.length() < 2) throw new IllegalArgumentException("regNo must have >=2 digits");
            int lastTwo = Integer.parseInt(digits.substring(digits.length() - 2));
            boolean isOdd = (lastTwo % 2 == 1);

            // 3) finalQuery — because your regNo is odd, use Q1 SQL (paste here)
            String finalQuery = """
                    SELECT 
                      p.AMOUNT AS SALARY,
                      CONCAT(e.FIRST_NAME, ' ', e.LAST_NAME) AS NAME,
                      TIMESTAMPDIFF(YEAR, e.DOB, CURDATE()) AS AGE,
                      d.DEPARTMENT_NAME
                    FROM PAYMENTS p
                    JOIN EMPLOYEE e ON p.EMP_ID = e.EMP_ID
                    JOIN DEPARTMENT d ON e.DEPARTMENT = d.DEPARTMENT_ID
                    WHERE DAY(p.PAYMENT_TIME) <> 1
                    ORDER BY p.AMOUNT DESC
                    LIMIT 1;
                    """.trim();

            // 4) Persist the result to file
            Path outDir = Path.of("./build/out");
            Files.createDirectories(outDir);
            Path outFile = outDir.resolve("finalQuery.sql");
            Files.writeString(outFile, finalQuery, StandardCharsets.UTF_8);
            System.out.println("[startup] wrote finalQuery to " + outFile.toAbsolutePath());

            // 5) Persist to H2 (submissions table)
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS submissions (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  reg_no VARCHAR(64),
                  is_odd BOOLEAN,
                  final_query CLOB,
                  created_at TIMESTAMP
                )
            """);
            jdbcTemplate.update("INSERT INTO submissions(reg_no, is_odd, final_query, created_at) VALUES (?, ?, ?, ?)",
                    regNo, isOdd, finalQuery, OffsetDateTime.now());
            System.out.println("[startup] saved submission to H2");

            // 6) Submit to returned webhook URL with JWT in Authorization header
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            // **Important:** spec says `Authorization: <accessToken>` (no Bearer)
            headers.set("Authorization", accessToken);

            FinalQueryRequest finalReq = new FinalQueryRequest(finalQuery);
            HttpEntity<FinalQueryRequest> entity = new HttpEntity<>(finalReq, headers);

            ResponseEntity<String> submitResp = restTemplate.postForEntity(webhookUrl, entity, String.class);
            System.out.println("[startup] submit response status=" + submitResp.getStatusCode() + " body=" + submitResp.getBody());

        } catch (Exception ex) {
            // Print stack trace and continue shutdown gracefully
            ex.printStackTrace();
            throw ex; // rethrow if you want the app to fail on error
        }
    }
}
