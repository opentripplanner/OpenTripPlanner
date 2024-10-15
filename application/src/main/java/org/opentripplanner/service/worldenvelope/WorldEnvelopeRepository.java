package org.opentripplanner.service.worldenvelope;

import java.io.Serializable;
import java.util.Optional;
import org.opentripplanner.service.worldenvelope.model.WorldEnvelope;

/**
 * The class implementing this interface is responsible for keeping the WorldEnvelope in memory for
 * shared access. It provides a container for the envelope. This is used set and pass the immutable
 * envelope around inside the application context.
 * <p>
 * The entire repository is serialized in the <em>graph.obj</em> file, so
 * it needs to be serializable.
 * <p>
 * The envelope is built at graph build time. Be aware that before the envelope is set, accessing
 * it will cause a null pointer exception.
 * <p>
 * The implementation must be THREAD-SAFE.
 * <p>
 * This serves as an example for creating a service backed by a repository. In this case it would
 * be ok to drop this interface and let the service implementation also hold the data -
 * serving both the role of the Service interface and implementing the repository. The
 * world-envelope-graph-builder is the only component which needs the repository and making it more tightly
 * coupled would be ok. But, with the repository interface it is loosely coupled.
 */
public interface WorldEnvelopeRepository extends Serializable {
  Optional<WorldEnvelope> retrieveEnvelope();

  void saveEnvelope(WorldEnvelope envelope);
}
