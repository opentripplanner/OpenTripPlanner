package org.opentripplanner.framework.io;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonDataListDownloader<T> {

  private static final Logger log = LoggerFactory.getLogger(JsonDataListDownloader.class);
  private final String jsonParsePath;
  private final Map<String, String> headers;
  private final Function<JsonNode, T> elementParser;
  private final String url;
  private final OtpHttpClient otpHttpClient;

  public JsonDataListDownloader(
    @Nonnull String url,
    @Nonnull String jsonParsePath,
    @Nonnull Function<JsonNode, T> elementParser,
    @Nonnull Map<String, String> headers
  ) {
    this(url, jsonParsePath, elementParser, headers, new OtpHttpClient());
  }

  public JsonDataListDownloader(
    @Nonnull String url,
    @Nonnull String jsonParsePath,
    @Nonnull Function<JsonNode, T> elementParser,
    @Nonnull Map<String, String> headers,
    @Nonnull OtpHttpClient OtpHttpClient
  ) {
    this.url = Objects.requireNonNull(url);
    this.jsonParsePath = Objects.requireNonNull(jsonParsePath);
    this.headers = Objects.requireNonNull(headers);
    this.elementParser = Objects.requireNonNull(elementParser);
    this.otpHttpClient = Objects.requireNonNull(OtpHttpClient);
  }

  public List<T> download() {
    if (url == null) {
      log.warn("Cannot download updates, because url is null!");
      return null;
    }
    try {
      return otpHttpClient.getAndMap(
        URI.create(url),
        headers,
        is -> {
          try {
            return parseJSON(is);
          } catch (IllegalArgumentException e) {
            log.warn("Error parsing bike rental feed from {}", url, e);
          } catch (JsonProcessingException e) {
            log.warn("Error parsing bike rental feed from {} (bad JSON of some sort)", url, e);
          } catch (IOException e) {
            log.warn("Error reading bike rental feed from {}", url, e);
          }
          return null;
        }
      );
    } catch (OtpHttpClientException e) {
      log.warn("Failed to get data from url {}", url);
      return null;
    }
  }

  private static String convertStreamToString(java.io.InputStream is) {
    try (java.util.Scanner scanner = new java.util.Scanner(is).useDelimiter("\\A")) {
      return scanner.hasNext() ? scanner.next() : "";
    }
  }

  private List<T> parseJSON(InputStream dataStream) throws IllegalArgumentException, IOException {
    ArrayList<T> out = new ArrayList<>();

    String rentalString = convertStreamToString(dataStream);

    ObjectMapper mapper = new ObjectMapper();
    JsonNode rootNode = mapper.readTree(rentalString);

    if (!jsonParsePath.isEmpty()) {
      String delimiter = "/";
      String[] parseElement = jsonParsePath.split(delimiter);
      for (String s : parseElement) {
        rootNode = rootNode.path(s);
      }

      if (rootNode.isMissingNode()) {
        throw new IllegalArgumentException("Could not find jSON elements " + jsonParsePath);
      }
    }

    for (JsonNode node : rootNode) {
      if (node == null) {
        continue;
      }
      try {
        T parsedElement = elementParser.apply(node);
        if (parsedElement != null) {
          out.add(parsedElement);
        }
      } catch (Exception e) {
        log.error("Could not process element in JSON list downloaded from {}", url, e);
      }
    }
    return out;
  }
}
