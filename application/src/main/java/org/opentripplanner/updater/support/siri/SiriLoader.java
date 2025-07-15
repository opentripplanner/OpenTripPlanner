package org.opentripplanner.updater.support.siri;

import jakarta.xml.bind.JAXBException;
import java.util.Optional;
import uk.org.siri.siri21.Siri;

/**
 * The Siri loader is used to fetch updates from a source like http(s) or directory.
 */
public interface SiriLoader {
  /**
   * Request a new Siri SX update.
   */
  Optional<Siri> fetchSXFeed(String requestorRef) throws JAXBException;

  /**
   * Request a new Siri ET update.
   */
  Optional<Siri> fetchETFeed(String requestorRef) throws JAXBException;
}
