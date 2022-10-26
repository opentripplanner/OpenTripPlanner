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
}
