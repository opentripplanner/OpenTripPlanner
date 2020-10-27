package org.opentripplanner.ext.interactivelauncher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

class CliArguments {
  final List<File> dirs;
  final String absolutePathPrefix;

  private static final Logger LOG = LoggerFactory.getLogger(CliArguments.class);

  CliArguments(List<File> dirs) {
    this.dirs = List.copyOf(dirs);
    this.absolutePathPrefix = findPrefix(dirs);
  }

  static CliArguments parseArgs(String[] args) {
    if(args.length == 0) {
      LOG.error("No OTP configuration root directory provided.");
      return new CliArguments(List.of());
    }
    List<File> configRootDirs = new ArrayList<>();

    for (String arg : args) {
      File rootDir = new File(arg);
      if(!rootDir.exists()) {
        LOG.error("OTP config directory not found: " + rootDir.getAbsolutePath());
        continue;
      }
      if(!rootDir.isDirectory()) {
        LOG.error("OTP config directory not a directory: " + rootDir.getAbsolutePath());
        continue;
      }
      configRootDirs.addAll(SearchForOtpConfig.search(rootDir));
    }
    return new CliArguments(configRootDirs);
  }

  private String findPrefix(List<File> dirs) {
    return dirs.stream()
        .map(File::getAbsolutePath)
        .reduce((x, y) -> {
          int i = 0;
          while (true) {
            if(i == x.length() || i == y.length()) { break; }
            if(x.charAt(i) != y.charAt(i)) { break; }
            ++i;
          }
          return x.substring(0, i);
        }).orElse("");
  }
}
