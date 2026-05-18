package org.opentripplanner.service.vehiclerental.street.geofencing;

import java.util.Set;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * Result of applying geofencing zones to the street graph.
 *
 * @param boundaryVertices vertices with boundary-crossing extensions, tracked for cleanup
 * @param zoneIndex spatial index of all zones for containment queries
 */
public record GeofencingZoneApplierResult(
  Set<Vertex> boundaryVertices,
  GeofencingZoneIndex zoneIndex
) {}
