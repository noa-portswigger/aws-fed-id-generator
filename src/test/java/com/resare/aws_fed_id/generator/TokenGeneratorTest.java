package com.resare.aws_fed_id.generator;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;

public class TokenGeneratorTest {

  private static final String TEST_AUDIENCE = "example.com";
  private static final AwsCredentials DUMMY_CREDENTIALS =
      AwsBasicCredentials.create(
          "AKIAIOSFODNN7EXAMPLE", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
  private static final AwsCredentialsProvider DUMMY_CREDENTIALS_PROVIDER =
      StaticCredentialsProvider.create(DUMMY_CREDENTIALS);

  @Test
  public void testGenerateWithDefaults() throws Exception {
    TokenGenerator generator = new TokenGenerator(DUMMY_CREDENTIALS_PROVIDER);

    String token = generator.generate(TEST_AUDIENCE);

    assertNotNull(token);
    assertFalse(token.isEmpty());

    // Verify token structure
    JsonNode tokenData = parseToken(token);
    assertNotNull(tokenData);
    assertEquals("POST", tokenData.get("method").asText());

    String url = tokenData.get("url").asText();
    assertTrue(url.contains("sts.us-east-1.amazonaws.com"));
    assertTrue(url.contains("Action=GetCallerIdentity"));
  }

  @Test
  public void testGenerateWithCustomRegion() throws Exception {
    TokenGenerator generator = new TokenGenerator(Region.EU_WEST_1, DUMMY_CREDENTIALS_PROVIDER);

    String token = generator.generate(TEST_AUDIENCE);

    JsonNode tokenData = parseToken(token);
    String url = tokenData.get("url").asText();
    assertTrue(url.contains("sts.eu-west-1.amazonaws.com"));
  }

  @Test
  public void testGenerateWithCustomCredentialsProvider() throws Exception {
    AwsCredentials customCredentials =
        AwsBasicCredentials.create(
            "AKIAI44QH8DHBEXAMPLE", "je7MtGbClwBF/2Zp9Utk/h3yCo8nvbEXAMPLEKEY");
    AwsCredentialsProvider customProvider = StaticCredentialsProvider.create(customCredentials);

    TokenGenerator generator = new TokenGenerator(customProvider);

    String token = generator.generate(TEST_AUDIENCE);

    assertNotNull(token);
    assertFalse(token.isEmpty());

    // Token should be different from default credentials due to different signing
    TokenGenerator defaultGenerator = new TokenGenerator(DUMMY_CREDENTIALS_PROVIDER);
    String defaultToken = defaultGenerator.generate(TEST_AUDIENCE);

    assertNotEquals(token, defaultToken);
  }

  @Test
  public void testTokenStructure() throws Exception {
    TokenGenerator generator = new TokenGenerator(DUMMY_CREDENTIALS_PROVIDER);

    String token = generator.generate(TEST_AUDIENCE);
    JsonNode tokenData = parseToken(token);

    // Verify required fields exist
    assertTrue(tokenData.has("url"));
    assertTrue(tokenData.has("method"));
    assertTrue(tokenData.has("headers"));

    // Verify method
    assertEquals("POST", tokenData.get("method").asText());

    // Verify headers structure
    JsonNode headers = tokenData.get("headers");
    assertTrue(headers.isArray());
    assertFalse(headers.isEmpty());

    // Check for required headers
    boolean hasHostHeader = false;
    boolean hasAuthHeader = false;
    boolean hasTargetResourceHeader = false;

    for (JsonNode header : headers) {
      String key = header.get("key").asText();
      if ("host".equals(key)) {
        hasHostHeader = true;
      } else if ("authorization".equals(key)) {
        hasAuthHeader = true;
      } else if ("x-goog-cloud-target-resource".equals(key)) {
        hasTargetResourceHeader = true;
        String value = header.get("value").asText();
        assertTrue(value.contains("example.com"));
      }
    }

    assertTrue(hasHostHeader, "Token should contain Host header");
    assertTrue(hasAuthHeader, "Token should contain Authorization header");
    assertTrue(hasTargetResourceHeader, "Token should contain x-goog-cloud-target-resource header");
  }

  @Test
  public void testAudienceHandling() throws Exception {
    String audienceWithHttps = "https://example.com/api/auth";

    TokenGenerator generator = new TokenGenerator(DUMMY_CREDENTIALS_PROVIDER);

    String token = generator.generate(audienceWithHttps);
    JsonNode tokenData = parseToken(token);

    // Find the x-goog-cloud-target-resource header
    JsonNode headers = tokenData.get("headers");
    for (JsonNode header : headers) {
      if ("x-goog-cloud-target-resource".equals(header.get("key").asText())) {
        String value = header.get("value").asText();
        assertFalse(value.startsWith("https://"), "https:// should be stripped from audience");
        assertTrue(value.startsWith("example.com"));
        break;
      }
    }
  }

  private JsonNode parseToken(String urlEncodedToken) throws Exception {
    String decodedToken = URLDecoder.decode(urlEncodedToken, StandardCharsets.UTF_8);
    ObjectMapper mapper = new ObjectMapper();
    return mapper.readTree(decodedToken);
  }
}
