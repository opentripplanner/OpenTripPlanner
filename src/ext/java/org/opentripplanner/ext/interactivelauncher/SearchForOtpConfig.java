package org.opentripplanner.ext.interactivelauncher;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.opentripplanner.standalone.config.ConfigLoader.isConfigFile;

/**
 * Search for directories containing OTP configuration files. The search is
 * recursive and searches sub-directories 10 levels deep.
 */
class SearchForOtpConfig {

  public static final int DEPTH_LIMIT = 10;

  static List<File> search(File rootDir) {
    return recursiveSearch(rootDir, DEPTH_LIMIT).collect(Collectors.toUnmodifiableList());
  }

  @SuppressWarnings("ConstantConditions")
  static private Stream<File> recursiveSearch(File dir, final int depthLimit) {
    if(!dir.isDirectory() || depthLimit == 0) { return Stream.empty(); }

    if(isOtpConfigDataDir(dir)) {
      return Stream.of(dir);
    }

    final int newDepthLimit = depthLimit - 1;
    return Arrays.stream(dir.listFiles())
        .flatMap(f -> recursiveSearch(f, newDepthLimit));
  }

  @SuppressWarnings("ConstantConditions")
  static private boolean isOtpConfigDataDir(File dir) {
    for (File f : dir.listFiles()) {
      if(isConfigFile(f.getName())) {
        return true;
      }
    }
    return false;
  }
}
