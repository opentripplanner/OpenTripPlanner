package org.opentripplanner.standalone.config.framework.project;

import static java.util.Map.entry;
import static org.opentripplanner.model.projectinfo.OtpProjectInfo.projectInfo;

import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.opentripplanner.framework.application.OtpAppException;
import org.opentripplanner.utils.text.TextVariablesSubstitution;

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
   * @param source is used only to generate a human friendly error message in case the text
   *               contains a placeholder which cannot be found.
   * @throws IllegalArgumentException if a placeholder exists in the {@code text}, but the
   *                                  environment variable does not exist.
   */
  public static String insertEnvironmentVariables(String text, String source) {
    return insertVariables(text, source, EnvironmentVariableReplacer::getEnvVarOrProjectInfo);
  }

  /**
   * Same as {@link #insertEnvironmentVariables(String, String)}, but the caller mus provide the
   * {@code variableResolver} - environment and project info variables are not available.
   */
  public static String insertVariables(
    String text,
    String source,
    Function<String, String> variableResolver
  ) {
    return TextVariablesSubstitution.insertVariables(
      text,
      variableResolver,
      varName -> errorVariableNameNotFound(varName, source)
    );
  }

  @Nullable
  private static String getEnvVarOrProjectInfo(String key) {
    String value = System.getenv(key);
    if (value == null) {
      return PROJECT_INFO.get(key);
    }
    return value;
  }

  private static void errorVariableNameNotFound(String variableName, String source) {
    throw new OtpAppException(
      "Environment variable name '" +
      variableName +
      "' in config '" +
      source +
      "' not found in the system environment variables."
    );
  }
}
