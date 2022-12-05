package org.opentripplanner.standalone.config.framework.project;

import static java.util.Map.entry;
import static org.opentripplanner.model.projectinfo.OtpProjectInfo.projectInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.opentripplanner.framework.application.OtpAppException;

/**
 * Replaces environment variable placeholders specified on the format ${variable} in a text with the
 * current system environment variable values.
 * <p>
 * The following OTP Project info is also available in addition to system environment variables:
 *
 * <ul>
 *   <li>{@code maven.version} - Full Maven version string, including the SNAPSHOT qualifier if present.</li>
 *   <li>{@code maven.version.short} - Maven version without the SNAPSHOT qualifier</li>
 *   <li>{@code maven.version.major} - Major version number</li>
 *   <li>{@code maven.version.minor} - Minor version number</li>
 *   <li>{@code maven.version.patch} - Patch version number</li>
 *   <li>{@code maven.version.qualifier} - Maven SNAPSHOT qualifier.</li>
 *   <li>{@code git.branch} - Git branch</li>
 *   <li>{@code git.commit} - Git commit hash</li>
 *   <li>{@code git.commit.timestamp} - Timestamp for the Git commit.</li>
 * </ul>
 */
public class EnvironmentVariableReplacer {

  /**
   * A pattern matching a placeholder like '${VAR_NAME}'. The placeholder must start with '${' and
   * end with '}'. The environment variable name must consist of only alphanumerical characters(a-z,
   * A-Z, 0-9), dot `.` and underscore '_'.
   */
  private static final Pattern PATTERN = Pattern.compile("\\$\\{([.\\w]+)}");

  private static final Map<String, String> PROJECT_INFO = Map.ofEntries(
    entry("maven.version", projectInfo().version.version),
    entry("maven.version.short", projectInfo().version.unqualifiedVersion()),
    entry("maven.version.major", Integer.toString(projectInfo().version.major)),
    entry("maven.version.minor", Integer.toString(projectInfo().version.minor)),
    entry("maven.version.patch", Integer.toString(projectInfo().version.patch)),
    entry("maven.version.qualifier", projectInfo().version.qualifier),
    entry("graph.file.header", projectInfo().graphFileHeaderInfo.asString()),
    entry(
      "otp.serialization.version.id",
      projectInfo().graphFileHeaderInfo.otpSerializationVersionId()
    ),
    entry("git.branch", projectInfo().versionControl.branch),
    entry("git.commit", projectInfo().versionControl.commit),
    entry("git.commit.timestamp", projectInfo().versionControl.commitTime)
  );

  /**
   * Search for {@link #PATTERN}s and replace each placeholder with the value of the corresponding
   * environment variable.
   *
   * @param source is used only to generate human friendly error message in case the text contain a
   *               placeholder whitch can not be found.
   * @throws IllegalArgumentException if a placeholder exist in the {@code text}, but the
   *                                  environment variable do not exist.
   */
  public static String insertEnvironmentVariables(String text, String source) {
    return insertVariables(text, source, System::getenv);
  }

  public static String insertVariables(
    String text,
    String source,
    Function<String, String> getEnvVar
  ) {
    Map<String, String> environmentVariables = new HashMap<>();
    Matcher matcher = PATTERN.matcher(text);

    while (matcher.find()) {
      String envVar = matcher.group(0);
      String nameOnly = matcher.group(1);
      if (!environmentVariables.containsKey(nameOnly)) {
        String value = getEnvVar.apply(nameOnly);
        if (value != null) {
          environmentVariables.put(envVar, value);
        } else if (PROJECT_INFO.containsKey(nameOnly)) {
          environmentVariables.put(envVar, PROJECT_INFO.get(nameOnly));
        } else {
          throw new OtpAppException(
            "Environment variable name '" +
            nameOnly +
            "' in config '" +
            source +
            "' not found in the system environment variables."
          );
        }
      }
    }
    for (Map.Entry<String, String> entry : environmentVariables.entrySet()) {
      text = text.replace(entry.getKey(), entry.getValue());
    }
    return text;
  }
}
