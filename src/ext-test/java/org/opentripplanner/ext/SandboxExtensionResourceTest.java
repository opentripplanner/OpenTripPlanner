package org.opentripplanner.ext;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import org.junit.jupiter.api.Test;

/**
 * This test has NOTHING to do with the OJP extension. The test just make sure that the Maven
 * project is correctly set up and that the 'ext-test/resources' is available on classpath.
 */
public class SandboxExtensionResourceTest {

  @Test
  @SuppressWarnings("ConstantConditions")
  public void verifyTestResourcesIsAvailableOnClasspath() throws IOException {
    InputStream resource = ClassLoader.getSystemResourceAsStream("test.txt");

    LineNumberReader in = new LineNumberReader(new InputStreamReader(resource));

    String text = in.readLine();

    assertTrue(text.length() > 2, text);
  }
}
