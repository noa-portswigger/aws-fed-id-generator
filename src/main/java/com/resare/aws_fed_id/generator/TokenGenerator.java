package com.resare.aws_fed_id.generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;

public class TokenGenerator {
  private final Region region;
  private final AwsCredentialsProvider credentialsProvider;

  public TokenGenerator() {
    this.region = Region.US_EAST_1;
    this.credentialsProvider = DefaultCredentialsProvider.builder().build();
  }

  public TokenGenerator(Region region) {
    this.region = region != null ? region : Region.US_EAST_1;
    this.credentialsProvider = DefaultCredentialsProvider.builder().build();
  }

  public TokenGenerator(AwsCredentialsProvider credentialsProvider) {
    this.region = Region.US_EAST_1;
    this.credentialsProvider =
        credentialsProvider != null
            ? credentialsProvider
            : DefaultCredentialsProvider.builder().build();
  }

  public TokenGenerator(Region region, AwsCredentialsProvider credentialsProvider) {
    this.region = region != null ? region : Region.US_EAST_1;
    this.credentialsProvider =
        credentialsProvider != null
            ? credentialsProvider
            : DefaultCredentialsProvider.builder().build();
  }

  /**
   * Generate a token used by a third-party system to authenticate an AWS role. The format of this
   * token conforms to the subject token expected by the Google Cloud Platform (GCP) Secure Token
   * Service (STS) /v1/token endpoint.
   *
   * <p>The format is described <a
   * href="https://cloud.google.com/iam/docs/workload-identity-federation-with-other-clouds">here</a>.
   * The section with python code under "Advanced Scenarios" is particularly helpful. The reason for
   * conforming to this format is mainly to piggyback on the security analysis that no doubt went
   * into the GCP Identity federation setup.
   *
   * @param audience When using with GCP, this is the workload identity pool audience. In other use
   *     cases, use some string such as hostname that uniquely identifies the authenticating party.
   *     The purpose of this value is to avoid replay attacks where a token created for a different
   *     audience is being re-used to authenticate.
   * @return A string containing the token
   */
  public String generate(String audience) {
    try {
      String host = String.format("sts.%s.amazonaws.com", region.id());
      String baseUrl = String.format("https://%s/", host);

      SdkHttpFullRequest request =
          SdkHttpFullRequest.builder()
              .method(SdkHttpMethod.POST)
              .uri(
                  URI.create(
                      String.format("%s?Action=GetCallerIdentity&Version=2011-06-15", baseUrl)))
              .putHeader("Host", host)
              .putHeader("x-goog-cloud-target-resource", audience.replace("https://", ""))
              .build();

      // For now, Google requires the deprecated version.
      // See https://github.com/googleapis/google-auth-library-java/issues/1792 for details.
      @SuppressWarnings("deprecation")
      Aws4Signer signer = Aws4Signer.create();
      Aws4SignerParams signerParams =
          Aws4SignerParams.builder()
              .awsCredentials(credentialsProvider.resolveCredentials())
              .signingName("sts")
              .signingRegion(region)
              .build();

      return createTokenFromSignedRequest(signer.sign(request, signerParams));
    } catch (RuntimeException e) {
      throw new RuntimeException("Failed to generate subject token", e);
    }
  }

  private String createTokenFromSignedRequest(SdkHttpFullRequest signedRequest) {
    try {
      ObjectMapper objectMapper = new ObjectMapper();

      ArrayNode headersArray = objectMapper.createArrayNode();
      for (Map.Entry<String, List<String>> header : signedRequest.headers().entrySet()) {
        // AWS SDK may have multiple values for a header, we take the first one
        String value = header.getValue().isEmpty() ? "" : header.getValue().getFirst();
        ObjectNode headerObj = objectMapper.createObjectNode();
        headerObj.put("key", header.getKey().toLowerCase());
        headerObj.put("value", value);
        headersArray.add(headerObj);
      }

      ObjectNode tokenData = objectMapper.createObjectNode();
      tokenData.put("url", signedRequest.getUri().toString());
      tokenData.put("method", signedRequest.method().name());
      tokenData.set("headers", headersArray);

      // Convert to JSON and URL-encode
      String tokenJson = objectMapper.writeValueAsString(tokenData);
      return URLEncoder.encode(tokenJson, StandardCharsets.UTF_8);

    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to create token from signed request", e);
    }
  }
}
