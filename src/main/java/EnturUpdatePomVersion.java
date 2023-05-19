import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

public class EnturUpdatePomVersion {

  private static final String VERSION_SEP = "-";
  private static final String ENTUR_PREFIX = "entur" + VERSION_SEP;
  private static final Path POM_FILE = Path.of("pom.xml");

  private final List<String> tags = new ArrayList<>();
  private final List<String> pomFile = new ArrayList<>();
  private String mainVersion;
  private int versionNumber = 0;

  public static void main(String[] args) {
    try {
      new EnturUpdatePomVersion().withArgs(args).run();
    } catch (Exception e) {
      System.err.println(e.getMessage());
      e.printStackTrace(System.err);
      System.exit(10);
    }
  }

  private EnturUpdatePomVersion withArgs(String[] args) throws IOException {
    if (args.length != 1 || args[0].matches("(?i)-h|--help")) {
      printHelp();
      System.exit(1);
    }
    String arg = args[0];

    if (arg.matches("\\d+")) {
      versionNumber = resolveVersionFromNumericString(arg);
    } else {
      tags.addAll(readTagsFromFile(arg));
    }
    return this;
  }

  private void run() throws IOException {
    readAndReplaceVersion();
    replacePomFile();
  }

  public void readAndReplaceVersion() throws IOException {
    var pattern = Pattern.compile(
      "(\\s*<version>)(\\d+.\\d+.\\d+)" +
      VERSION_SEP +
      "(" +
      ENTUR_PREFIX +
      "\\d+|SNAPSHOT)(</version>\\s*)"
    );
    boolean found = false;
    int i = 0;

    for (String line : Files.readAllLines(POM_FILE, UTF_8)) {
      // Look for the version in the 25 first lines
      if (!found) {
        var m = pattern.matcher(line);
        if (m.matches()) {
          mainVersion = m.group(2);
          String newVersion = mainVersion + VERSION_SEP + ENTUR_PREFIX + resolveVersionNumber();
          line = m.group(1) + newVersion + m.group(4);
          System.out.println(newVersion);
          found = true;
        }
        if (++i == 25) {
          throw new IllegalStateException(
            "Version not found in first 25 lines of the pom.xml file."
          );
        }
      }
      pomFile.add(line);
    }
    if (!found) {
      throw new IllegalStateException(
        "Version not found in 'pom.xml'. Nothing matching pattern: " + pattern
      );
    }
  }

  public void replacePomFile() throws IOException {
    Files.delete(POM_FILE);
    Files.write(POM_FILE, pomFile, UTF_8);
  }

  private static void printHelp() {
    System.err.println(
      "Use this small program to replace the OTP version '2.1.0-SNAPSHOT' \n" +
      "with a new version number with a Entur qualifier like '2.1.0-entur-1'.\n" +
      "\n" +
      "Usage:\n" +
      "  $ java -cp .circleci  UpdatePomVersion <NEW-VERSION-NUMBER>\n"
    );
  }

  private int resolveVersionNumber() {
    var pattern = Pattern.compile("v" + mainVersion + VERSION_SEP + ENTUR_PREFIX + "(\\d+)");
    int maxTagVersion = tags
      .stream()
      .mapToInt(tag -> {
        var m = pattern.matcher(tag);
        return m.matches() ? Integer.parseInt(m.group(1)) : -999;
      })
      .max()
      .orElse(-999);

    return 1 + Math.max(maxTagVersion, versionNumber);
  }

  public static int resolveVersionFromNumericString(String arg) {
    try {
      return Integer.parseInt(arg);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
        "Unable to parse input, decimal number expected: '" + arg + "'"
      );
    }
  }

  private static Collection<String> readTagsFromFile(String arg) throws IOException {
    var tags = Files.readAllLines(Path.of(arg));
    if (tags.isEmpty()) {
      throw new IllegalStateException("Unable to load git tags from file: " + arg);
    }
    return tags;
  }
}
