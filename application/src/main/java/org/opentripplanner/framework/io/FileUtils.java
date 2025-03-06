package org.opentripplanner.framework.io;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

public class FileUtils {

  /**
   * Read the content of the file into a string. The file is must be UTF-8 encoded. If an error
   * occurs the exception is converted to a {@link RuntimeException}.
   */
  public static String readFile(File file) {
    try (var is = new FileInputStream(file)) {
      return new String(is.readAllBytes(), UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  /**
   * Write the given input doc to the given file. The file is UTF-8 encoded. If an error
   * occurs the exception is converted to a {@link RuntimeException}.
   */
  public static void writeFile(File file, String doc) {
    try (var fileOut = new FileOutputStream(file)) {
      var out = new PrintWriter(fileOut);
      out.write(doc);
      out.flush();
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  /**
   * This asserts can be used to compare a document with a file content, trailing whitespace
   * including line-breaks are striped away before comparison.
   */
  public static void assertFileEquals(String expectedDoc, File newFile) {
    String resultDoc = readFile(newFile);

    var expectedLines = expectedDoc.split("[\n\r]+");
    var resultLines = resultDoc.split("[\n\r]+");

    int i = 0, j = 0;

    while (i < expectedLines.length && j < resultLines.length) {
      while (expectedLines[i].isBlank()) {
        ++i;
      }
      while (resultLines[j].isBlank()) {
        ++j;
      }
      var expected = expectedLines[i].stripTrailing();
      var result = resultLines[j].stripTrailing();

      if (!expected.equals(result)) {
        throw new IllegalStateException(
          """
          The file(%s) differ from the expected document.
            Expected (line: %3d): %s
            Result   (line: %3d): %s
          """.formatted(newFile.getAbsolutePath(), i, expected, j, result)
        );
      }
      ++i;
      ++j;
    }

    if (i < expectedLines.length) {
      throw new IllegalStateException(
        "Lines missing in new file(" + newFile.getAbsolutePath() + "): " + expectedLines[i]
      );
    }
    if (j < resultLines.length) {
      throw new IllegalStateException(
        "Lines not expected in new file(" + newFile.getAbsolutePath() + "): " + resultLines[j]
      );
    }
  }
}
