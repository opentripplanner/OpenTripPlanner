package org.opentripplanner.standalone.config.framework.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.standalone.config.framework.project.EnvironmentVariableReplacer.insertEnvironmentVariables;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.application.OtpAppException;
import org.opentripplanner.model.projectinfo.MavenProjectVersion;
import org.opentripplanner.model.projectinfo.OtpProjectInfo;
import org.opentripplanner.model.projectinfo.VersionControlInfo;
import org.opentripplanner.standalone.config.framework.file.ConfigFileLoader;

public class EnvironmentVariableReplacerTest {

  private String envName = "<not set>";
  private String envValue = "<not set>";

  /**
   * To test the system environment variable substitution we must have a name/value pair that exist
   * in the system environment variables. So to prepare for the test, we look up a random
   * environment variable and use it later to construct the test samples, and the expected results.
   * <p>
   * We search for a environment variable name containing only alphanumeric characters and a value
   * with less than 30 characters. We do this to make it easier for humans to see what is going on,
   * if a test fails. This constraint is just to make the text involved more readable.
   */
  @BeforeEach
  public void setup() {
    Map.Entry<String, String> envVar = System.getenv()
      .entrySet()
      .stream()
      .filter(e -> e.getKey().matches("\\w+") && e.getValue().length() < 30)
      .findFirst()
      .orElse(null);

    if (envVar == null) {
      throw new IllegalStateException("No environment variables for testing found.");
    }
    envName = envVar.getKey();
    envValue = envVar.getValue();
  }

  /**
   * Test replacing environment variables in a text. The {@link EnvironmentVariableReplacer} should
   * replace placeholders like '${ENV_NAME}' with the value of the system environment variable.
   */
  @Test
  public void insertEnvironmentVariablesTest() {
    // Given: a text and a expected result
    String text = "Env.var: ${" + envName + "}, project branch: ${git.branch}.";
    String expectedResult =
      "Env.var: " +
      envValue +
      ", project branch: " +
      OtpProjectInfo.projectInfo().versionControl.branch +
      ".";

    // When:
    String result = insertEnvironmentVariables(text, "test");

    // Then:
    assertEquals(expectedResult, result);
  }

  @Test
  public void verifyThatAEnvVariableMayExistMoreThanOnce() {
    // Given: a text and a expected result
    String text = "Env. var1: ${" + envName + "} and var2: ${" + envName + "}.";
    String expectedResult = "Env. var1: " + envValue + " and var2: " + envValue + ".";

    // When:
    String result = insertEnvironmentVariables(text, "test");

    // Then:
    assertEquals(expectedResult, result);
  }

  @Test
  public void verifyProjectInfo() {
    // Given
    OtpProjectInfo p = OtpProjectInfo.projectInfo();
    MavenProjectVersion version = p.version;
    VersionControlInfo verControl = p.versionControl;

    // Expect
    assertEquals(p.version.version, insertEnvironmentVariables("${maven.version}", "test"));
    assertEquals(
      p.version.unqualifiedVersion(),
      insertEnvironmentVariables("${maven.version.short}", "test")
    );
    assertEquals(
      Integer.toString(version.major),
      insertEnvironmentVariables("${maven.version.major}", "test")
    );
    assertEquals(
      Integer.toString(version.minor),
      insertEnvironmentVariables("${maven.version.minor}", "test")
    );
    assertEquals(
      Integer.toString(version.patch),
      insertEnvironmentVariables("${maven.version.patch}", "test")
    );
    assertEquals(
      version.qualifier,
      insertEnvironmentVariables("${maven.version.qualifier}", "test")
    );
    assertEquals(verControl.branch, insertEnvironmentVariables("${git.branch}", "test"));
    assertEquals(verControl.commit, insertEnvironmentVariables("${git.commit}", "test"));
    assertEquals(
      verControl.commitTime,
      insertEnvironmentVariables("${git.commit.timestamp}", "test")
    );
  }

  /**
   * Test replacing environment variable fails for unknown environment variable.
   */
  @Test
  public void testMissingEnvironmentVariable() {
    assertThrows(OtpAppException.class, () ->
      ConfigFileLoader.nodeFromString(
        "None existing env.var: '${none_existing_env_variable}'.",
        "test"
      )
    );
  }
}
