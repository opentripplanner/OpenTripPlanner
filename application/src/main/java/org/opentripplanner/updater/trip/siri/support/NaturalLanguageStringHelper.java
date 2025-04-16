package org.opentripplanner.updater.trip.siri.support;

import java.util.List;
import uk.org.siri.siri21.NaturalLanguageStringStructure;

/**
 * Helper class for SIRI natural language string.
 */
public class NaturalLanguageStringHelper {

  /**
   * Return the first element in a list of SIRI natural language strings.
   * Default to an empty string if the list is null or empty.
   */
  public static String getFirstStringFromList(List<NaturalLanguageStringStructure> strings) {
    if (strings == null) {
      return "";
    }
    return strings.stream().findFirst().map(NaturalLanguageStringStructure::getValue).orElse("");
  }
}
