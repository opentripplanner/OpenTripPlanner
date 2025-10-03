package org.opentripplanner.framework.csv;

import java.util.List;

public class HeadersDoNotMatch extends Exception {

  public HeadersDoNotMatch(String filename, String rawLine, List<String> expected) {
    super(
      "The header does not match the expected values. File: " +
      filename +
      "\n\twas: [" +
      rawLine +
      "]" +
      "\n\texpected: " +
      expected
    );
  }
}
