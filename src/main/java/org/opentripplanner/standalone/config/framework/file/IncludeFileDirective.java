package org.opentripplanner.standalone.config.framework.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import org.opentripplanner.framework.application.OtpAppException;

/**
 * Replaces a file include directive with the text content of the file "as is". For example:
 * <pre>
 * {
 *   "my-config" : "${includeFile:myConfig.json}"
 * }
 * </pre>
 * The {@code ${includeFile:myConfig.json}} will tell the config parser to find the
 * <em>myConfig.json</em> in the local config directory and insert the content into the
 * configuration. This is done before the environment variables are resolved, enabling environment
 * variable replacement in the included file as well as the config file.
 */
public class IncludeFileDirective {

  public static final String QUOTE = "\"";
  /**
   * A pattern matching a placeholder like '${includeFile:my-own-config.json}'. The placeholder must
   * start with '${includeFile:' and end with '}'. The file name must consist of only alphanumerical
   * characters(a-z, A-Z, 0-9), dot '.', dash '-', and underscore '_'.
   */
  private static final Pattern INCLUDE_FILE_PATTERN = Pattern.compile(
    "\"?\\$\\{includeFile:([-.\\w]+)}\"?"
  );
  private final File configDir;

  private IncludeFileDirective(File configDir) {
    this.configDir = configDir;
  }

  /**
   * Search for {@link #INCLUDE_FILE_PATTERN}s and replace each placeholder with the value of the
   * corresponding environment variable.
   *
   * @param source is used only to generate human friendly error message in case the text contain a
   *               placeholder whitch can not be found.
   * @throws IllegalArgumentException if a placeholder exist in the {@code text}, but the
   *                                  environment variable do not exist.
   */
  public static String includeFileDirective(File configDir, String text, String source) {
    return new IncludeFileDirective(configDir).includeFileDirective(text, source);
  }

  private static String loadFile(File file, String directive, String source) {
    try {
      return IOUtils.toString(new FileInputStream(file), StandardCharsets.UTF_8);
    } catch (FileNotFoundException ex) {
      throw new OtpAppException(
        "File '{}' is not present. Can not include " + "directive '{}' in config file '{}'.",
        file.getPath(),
        directive,
        source
      );
    } catch (IOException e) {
      throw new OtpAppException(
        "Error while parsing file '{}'. Can not include " + "directive '{}' in config file '{}'.",
        file.getPath(),
        directive,
        source
      );
    }
  }

  private String includeFileDirective(String text, String source) {
    Matcher matcher = INCLUDE_FILE_PATTERN.matcher(text);
    Map<String, File> replacements = new HashMap<>();

    while (matcher.find()) {
      String directive = matcher.group(0);
      String fileName = matcher.group(1);
      File file = new File(configDir, fileName);

      if (!file.exists() || !file.canRead()) {
        throw new OtpAppException(
          "The file is not found for directive '" + directive + "' in config '" + source + "'."
        );
      }
      replacements.put(directive, file);
    }

    for (Entry<String, File> entry : replacements.entrySet()) {
      String directive = entry.getKey();
      String fileText = loadFile(entry.getValue(), directive, source);

      // If the insert text is a legal JSON object "[white-space]{ ... }[white-space]", then
      // ignore the optional quotes matched by the directive pattern
      var json = fileText.trim();
      if (json.startsWith("{") && json.endsWith("}")) {
        text = text.replace(entry.getKey(), fileText);
      } else {
        // Add back quotes if matched part of directive pattern
        String startQuote = directive.startsWith(QUOTE) ? QUOTE : "";
        String endQuote = directive.endsWith(QUOTE) ? QUOTE : "";
        text = text.replace(entry.getKey(), startQuote + fileText + endQuote);
      }
    }
    return text;
  }
}
