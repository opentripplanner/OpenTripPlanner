package org.opentripplanner.standalone.config;

import java.io.File;
import java.util.List;

public class CommandLineParametersTestFactory {

  public static CommandLineParameters createCliForTest(File baseDir) {
    CommandLineParameters params = new CommandLineParameters();
    params.baseDirectory = List.of(baseDir);
    return params;
  }
}
