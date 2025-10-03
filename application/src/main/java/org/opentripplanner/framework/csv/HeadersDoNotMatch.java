package org.opentripplanner.framework.csv;

public class HeadersDoNotMatch extends Exception {

  public HeadersDoNotMatch(String filename) {
    super("The header does not match the expected values for csv file: " + filename);
  }
}
