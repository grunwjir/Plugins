package org.perfcake.reporting.destination;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.perfcake.reporting.destination.dto.test.TestDto;
import org.perfcake.reporting.destination.dto.test_execution.TestExecutionDto;
import org.perfcake.reporting.destination.dto.test_execution.ValuesGroupDto;
import org.perfcake.reporting.destination.dto.util.authentication.AuthenticationResult;
import org.perfcake.reporting.destination.dto.util.authentication.LoginCredentialParams;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class RestClient {

    private String repoUrl;
    private ObjectMapper mapper;
    private String accessToken;

    RestClient(String repoUrl) {
        this.repoUrl = repoUrl;
        mapper = new ObjectMapper();
        accessToken = authenticate().getToken();
    }

    public void addExecutionValues(Long testExecutionId, ValuesGroupDto valuesGroup) {
        HttpURLConnection conn = createPostConnection("/rest/json/test-executions/" + testExecutionId + "/values");

        try {
            mapper.writeValue(conn.getOutputStream(), valuesGroup);
            conn.getResponseCode();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            conn.disconnect();
        }
    }

    public Long createTestExecution(TestExecutionDto testExecution) {
        HttpURLConnection conn = createPostConnection("/rest/json/test-executions");

        try {
            mapper.writeValue(conn.getOutputStream(), testExecution);
            String[] parts = conn.getHeaderField("Location").split("/");
            return Long.parseLong(parts[parts.length - 1]);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            conn.disconnect();
        }
    }

    public TestDto getTestByUid(String uid) {
        HttpURLConnection conn = createGetConnection("/rest/json/tests/uid/" + uid);
        try {
            return mapper.readValue(conn.getInputStream(), TestDto.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            conn.disconnect();
        }
    }

    public TestExecutionDto getTestExecution(Long id) {
        HttpURLConnection conn = createGetConnection("/rest/json/test-executions/" + id);
        try {
            return mapper.readValue(conn.getInputStream(), TestExecutionDto.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            conn.disconnect();
        }
    }

    private AuthenticationResult authenticate() {
            HttpURLConnection conn = createPostConnection("/rest/json/authentication");
            LoginCredentialParams login = new LoginCredentialParams();
            login.setUsername("grunwjir");
            login.setPassword("123456");

        try {
            mapper.writeValue(conn.getOutputStream(), login);
            return mapper.readValue(conn.getInputStream(), AuthenticationResult.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            conn.disconnect();
        }
    }

    private HttpURLConnection createConnection(String serviceUrl, String method) {
        try {
            URL url = new URL(repoUrl + serviceUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            if (method.equals("POST") || method.equals("PUT")) {
                conn.setDoOutput(true);
            }
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            if (!serviceUrl.equals("/rest/json/authentication")) {
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            }
            return conn;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private HttpURLConnection createGetConnection(String serviceUrl) {
        return createConnection(serviceUrl, "GET");
    }

    private HttpURLConnection createPutConnection(String serviceUrl) {
        return createConnection(serviceUrl, "PUT");
    }

    private HttpURLConnection createPostConnection(String serviceUrl) {
        return createConnection(serviceUrl, "POST");
    }
}