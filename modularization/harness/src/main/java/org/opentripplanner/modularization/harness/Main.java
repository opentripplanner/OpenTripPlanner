package org.opentripplanner.modularization.harness;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Command line entry point.
 *
 * <pre>
 * capture --jar OTP.jar --out DIR [--repo-root DIR] [--datasets DIR] [--only a,b] [--port N] [--xmx 3G]
 * serve --jar OTP.jar --only DATASET [--repo-root DIR] [--datasets DIR] [--port N] [--xmx 3G]
 * compare --baseline DIR --candidate DIR [--approved FILE] [--report FILE]
 * </pre>
 *
 * {@code compare} exits with 0 when the candidate complies with the baseline, and 1 otherwise.
 */
public final class Main {

  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      usage();
      return;
    }
    Map<String, String> opts = parseOptions(args);
    switch (args[0]) {
      case "capture" -> Capture.run(
        Path.of(required(opts, "jar")),
        Path.of(opts.getOrDefault("repo-root", "../..")),
        Path.of(opts.getOrDefault("datasets", "datasets")),
        Path.of(required(opts, "out")),
        opts.containsKey("only") ? Set.of(opts.get("only").split(",")) : Set.of(),
        Integer.parseInt(opts.getOrDefault("port", "8080")),
        opts.getOrDefault("xmx", "3G")
      );
      case "serve" -> Capture.serve(
        Path.of(required(opts, "jar")),
        Path.of(opts.getOrDefault("repo-root", "../..")),
        Path.of(opts.getOrDefault("datasets", "datasets")),
        required(opts, "only"),
        Integer.parseInt(opts.getOrDefault("port", "8080")),
        opts.getOrDefault("xmx", "3G")
      );
      case "compare" -> System.exit(compare(opts));
      default -> usage();
    }
  }

  private static int compare(Map<String, String> opts) throws Exception {
    var differences = Differ.compareDirectories(
      Path.of(required(opts, "baseline")),
      Path.of(required(opts, "candidate"))
    );
    var approved = opts.containsKey("approved")
      ? ApprovedDiscrepancies.read(Path.of(opts.get("approved")))
      : ApprovedDiscrepancies.empty();
    var result = approved.check(differences);
    String report = Report.format(differences.size(), result);
    System.out.println(report);
    if (opts.containsKey("report")) {
      Files.writeString(Path.of(opts.get("report")), report);
    }
    return result.isCompliant() ? 0 : 1;
  }

  private static Map<String, String> parseOptions(String[] args) {
    var opts = new HashMap<String, String>();
    for (int i = 1; i < args.length - 1; i += 2) {
      if (!args[i].startsWith("--")) {
        throw new IllegalArgumentException("Expected an option, got: " + args[i]);
      }
      opts.put(args[i].substring(2), args[i + 1]);
    }
    return opts;
  }

  private static String required(Map<String, String> opts, String name) {
    String v = opts.get(name);
    if (v == null) {
      throw new IllegalArgumentException("Missing --" + name);
    }
    return v;
  }

  private static void usage() {
    System.out.println(
      String.join(
        "\n",
        List.of(
          "capture --jar OTP.jar --out DIR [--repo-root DIR] [--datasets DIR] [--only a,b] [--port N] [--xmx 3G]",
          "compare --baseline DIR --candidate DIR [--approved FILE] [--report FILE]"
        )
      )
    );
  }
}
