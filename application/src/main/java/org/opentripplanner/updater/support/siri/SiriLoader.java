package org.opentripplanner.updater.support.siri;

import jakarta.xml.bind.JAXBException;
import java.util.Optional;
import org.opentripplanner.updater.UpdateIncrementality;
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

  /**
   * Describes how the payloads returned by this loader relate to the previously fetched ones.
   * <p>
   * A loader that uses the SIRI request/response flow with a requestor ref only receives the
   * changes since the previous request and is therefore
   * {@link UpdateIncrementality#DIFFERENTIAL}, while a loader that fetches the complete data set
   * on every call is {@link UpdateIncrementality#FULL_DATASET}.
   */
  UpdateIncrementality incrementality();
}
