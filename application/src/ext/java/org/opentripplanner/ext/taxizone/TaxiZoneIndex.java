package org.opentripplanner.ext.taxizone;

import java.util.List;
import java.util.Optional;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.index.strtree.STRtree;
import org.opentripplanner.ext.taxizone.model.TaxiZone;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.geometry.WgsCoordinate;

/**
 * Spatial index over car pickup provider zones. Used to look up which provider zone covers a given
 * pickup–dropoff coordinate pair.
 *
 * <p>TODO: Multi-provider support. Currently only the first matching zone is used.
 * In the future all matching providers should be available so users can choose.
 */
public class TaxiZoneIndex {

  private final STRtree index = new STRtree();

  public TaxiZoneIndex(List<TaxiZone> zones) {
    for (TaxiZone zone : zones) {
      index.insert(zone.geometry().getEnvelopeInternal(), zone);
    }
  }

  /**
   * Returns the first zone whose geometry contains both {@code pickup} and
   * {@code dropoff}. Returns an empty optional if no zone covers both endpoints.
   */
  public Optional<TaxiZone> findFirstZone(WgsCoordinate pickup, WgsCoordinate dropoff) {
    var gf = GeometryUtils.getGeometryFactory();
    Point pickupPoint = gf.createPoint(pickup.asJtsCoordinate());
    Point dropoffPoint = gf.createPoint(dropoff.asJtsCoordinate());

    Envelope envelope = new Envelope(pickup.asJtsCoordinate());
    @SuppressWarnings("unchecked")
    List<TaxiZone> candidates = index.query(envelope);

    for (TaxiZone zone : candidates) {
      var geom = zone.geometry();
      if (geom.contains(pickupPoint) && geom.contains(dropoffPoint)) {
        return Optional.of(zone);
      }
    }
    return Optional.empty();
  }
}
