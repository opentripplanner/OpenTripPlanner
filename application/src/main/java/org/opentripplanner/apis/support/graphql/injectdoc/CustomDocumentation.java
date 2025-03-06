package org.opentripplanner.apis.support.graphql.injectdoc;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import javax.annotation.Nullable;
import org.opentripplanner.framework.application.OtpAppException;
import org.opentripplanner.utils.text.TextVariablesSubstitution;

/**
 * Load custom documentation from a properties file and make it available to any
 * consumer using the {@code type-name[.field-name]} as key for lookups.
 */
public class CustomDocumentation {

  private static final String APPEND_SUFFIX = ".append";
  private static final String DESCRIPTION_SUFFIX = ".description";
  private static final String DEPRECATED_SUFFIX = ".deprecated";

  /** Put custom documentaion in the following sandbox package */
  private static final String DOC_PATH = "org/opentripplanner/ext/apis/transmodel/";
  private static final String FILE_NAME = "custom-documentation";
  private static final String FILE_EXTENSION = ".properties";

  private static final CustomDocumentation EMPTY = new CustomDocumentation(Map.of());

  private final Map<String, String> textMap;

  /**
   * Package local to be unit-testable
   */
  CustomDocumentation(Map<String, String> textMap) {
    this.textMap = textMap;
  }

  public static CustomDocumentation of(ApiDocumentationProfile profile) {
    if (profile == ApiDocumentationProfile.DEFAULT) {
      return EMPTY;
    }
    var map = loadCustomDocumentationFromPropertiesFile(profile);
    return map.isEmpty() ? EMPTY : new CustomDocumentation(map);
  }

  public boolean isEmpty() {
    return textMap.isEmpty();
  }

  /**
   * Get documentation for a type. The given {@code typeName} is used as the key. The
   * documentation text is resolved by:
   * <ol>
   *   <li>
   *     first looking up the given {@code key} + {@code ".description"}. If a value is found, then
   *     the value is returned.
   *   <li>
   *     then {@code key} + {@code ".description.append"} is used. If a value is found the
   *     {@code originalDoc} + {@code value} is returned.
   *   </li>
   * </ol>
   * @param typeName Use {@code TYPE_NAME} or {@code TYPE_NAME.FIELD_NAME} as key.
   */
  public Optional<String> typeDescription(String typeName, @Nullable String originalDoc) {
    return text(typeName, DESCRIPTION_SUFFIX, originalDoc);
  }

  /**
   * Same as {@link #typeDescription(String, String)} except the given {@code typeName} and
   * {@code fieldName} is used as the key.
   * <pre>
   * key := typeName + "." fieldNAme
   * </pre>
   */
  public Optional<String> fieldDescription(
    String typeName,
    String fieldName,
    @Nullable String originalDoc
  ) {
    return text(key(typeName, fieldName), DESCRIPTION_SUFFIX, originalDoc);
  }

  /**
   * Get <em>deprecated reason</em> for a field (types cannot be deprecated). The key
   * ({@code key = typeName + '.' + fieldName} is used to retrieve the reason from the properties
   * file. The deprecated documentation text is resolved by:
   * <ol>
   *   <li>
   *     first looking up the given {@code key} + {@code ".deprecated"}. If a value is found, then
   *     the value is returned.
   *   <li>
   *     then {@code key} + {@code ".deprecated.append"} is used. If a value is found the
   *     {@code originalDoc} + {@code text} is returned.
   *   </li>
   * </ol>
   * Any {@code null} values are excluded from the result and if both the input {@code originalDoc}
   * and the resolved value is {@code null}, then {@code empty} is returned.
   */
  public Optional<String> fieldDeprecatedReason(
    String typeName,
    String fieldName,
    @Nullable String originalDoc
  ) {
    return text(key(typeName, fieldName), DEPRECATED_SUFFIX, originalDoc);
  }

  /* private methods */

  /**
   * Create a key from the given {@code typeName} and {@code fieldName}
   */
  private static String key(String typeName, String fieldName) {
    return typeName + "." + fieldName;
  }

  private Optional<String> text(String key, String suffix, @Nullable String originalText) {
    final String k = key + suffix;
    return text(k).or(() -> appendText(k, originalText));
  }

  private Optional<String> text(String key) {
    return Optional.ofNullable(textMap.get(key));
  }

  private Optional<String> appendText(String key, @Nullable String originalText) {
    String value = textMap.get(key + APPEND_SUFFIX);
    if (value == null) {
      return Optional.empty();
    }
    return originalText == null ? Optional.of(value) : Optional.of(originalText + "\n\n" + value);
  }

  /* private methods */

  private static Map<String, String> loadCustomDocumentationFromPropertiesFile(
    ApiDocumentationProfile profile
  ) {
    try {
      final String resource = resourceName(profile);
      var input = ClassLoader.getSystemResourceAsStream(resource);
      if (input == null) {
        throw new OtpAppException("Resource not found: %s", resource);
      }
      var props = new Properties();
      props.load(new InputStreamReader(input, StandardCharsets.UTF_8));
      Map<String, String> map = new HashMap<>();

      for (String key : props.stringPropertyNames()) {
        String value = props.getProperty(key);
        if (value == null) {
          value = "";
        }
        map.put(key, value);
      }
      return TextVariablesSubstitution.insertVariables(map, varName ->
        errorHandlerVariableSubstitution(varName, resource)
      );
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void errorHandlerVariableSubstitution(String name, String source) {
    throw new OtpAppException("Variable substitution failed for '${%s}' in %s.", name, source);
  }

  private static String resourceName(ApiDocumentationProfile profile) {
    return DOC_PATH + FILE_NAME + "-" + profile.name().toLowerCase() + FILE_EXTENSION;
  }
}
