package org.opentripplanner.netex.mapping;

import java.util.List;
import javax.xml.bind.JAXBElement;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.impl.PackedCoordinateSequence;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.issues.MissingProjectionInServiceLink;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.impl.EntityById;
import org.opentripplanner.netex.index.api.ReadOnlyHierarchicalMap;
import org.opentripplanner.netex.index.api.ReadOnlyHierarchicalMapById;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.opentripplanner.transit.model.site.Stop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.LinkInLinkSequence_VersionedChildStructure;
import org.rutebanken.netex.model.LinkSequenceProjection_VersionStructure;
import org.rutebanken.netex.model.ServiceLink;
import org.rutebanken.netex.model.ServiceLinkInJourneyPattern_VersionedChildStructure;

/**
 * Maps NeTEx ServiceLinks to arrays of LineStrings.
 */
class ServiceLinkMapper {

  private static final GeometryFactory geometryFactory = GeometryUtils.getGeometryFactory();
  private final FeedScopedIdFactory idFactory;
  private final ReadOnlyHierarchicalMapById<ServiceLink> serviceLinkById;
  private final ReadOnlyHierarchicalMap<String, String> quayIdByStopPointRef;
  private final EntityById<Stop> stopById;
  private final DataImportIssueStore issueStore;
  private final double maxStopToShapeSnapDistance;

  ServiceLinkMapper(
    FeedScopedIdFactory idFactory,
    ReadOnlyHierarchicalMapById<ServiceLink> serviceLinkById,
    ReadOnlyHierarchicalMap<String, String> quayIdByStopPointRef,
    EntityById<Stop> stopById,
    DataImportIssueStore issueStore,
    double maxStopToShapeSnapDistance
  ) {
    this.idFactory = idFactory;
    this.serviceLinkById = serviceLinkById;
    this.quayIdByStopPointRef = quayIdByStopPointRef;
    this.stopById = stopById;
    this.issueStore = issueStore;
    this.maxStopToShapeSnapDistance = maxStopToShapeSnapDistance;
  }

  LineString[] getGeometriesByJourneyPattern(
    JourneyPattern journeyPattern,
    TripPattern tripPattern
  ) {
    LineString[] geometries = new LineString[tripPattern.numberOfStops() - 1];
    if (journeyPattern.getLinksInSequence() != null) {
      List<LinkInLinkSequence_VersionedChildStructure> linksInJourneyPattern = journeyPattern
        .getLinksInSequence()
        .getServiceLinkInJourneyPatternOrTimingLinkInJourneyPattern();
      for (int i = 0; i < linksInJourneyPattern.size(); i++) {
        var linkInLinkSequence = linksInJourneyPattern.get(i);
        if (
          linkInLinkSequence instanceof ServiceLinkInJourneyPattern_VersionedChildStructure serviceLinkInJourneyPattern
        ) {
          String serviceLinkRef = serviceLinkInJourneyPattern.getServiceLinkRef().getRef();
          ServiceLink serviceLink = serviceLinkById.lookup(serviceLinkRef);

          if (serviceLink != null) {
            geometries[i] = mapServiceLink(serviceLink, tripPattern, i);
          } else {
            issueStore.add(
              "MissingServiceLink",
              "ServiceLink %s not found in journey pattern %s",
              serviceLinkRef,
              journeyPattern.getId()
            );
          }
        }
      }
    }

    // Make sure all geometries are generated
    for (int i = 0; i < tripPattern.numberOfStops() - 1; ++i) {
      if (geometries[i] == null) {
        geometries[i] = createSimpleGeometry(tripPattern.getStop(i), tripPattern.getStop(i + 1));
      }
    }
    return geometries;
  }

  private LineString mapServiceLink(ServiceLink serviceLink, TripPattern tripPattern, int i) {
    if (
      serviceLink.getProjections() == null ||
      serviceLink.getProjections().getProjectionRefOrProjection() == null
    ) {
      issueStore.add(new MissingProjectionInServiceLink(serviceLink.getId()));
      return null;
    }
    return mapCoordinates(serviceLink, tripPattern, i);
  }

  private LineString mapCoordinates(
    ServiceLink serviceLink,
    TripPattern tripPattern,
    int stopIndex
  ) {
    String fromPointQuayId = quayIdByStopPointRef.lookup(serviceLink.getFromPointRef().getRef());
    Stop fromPointStop = stopById.get(idFactory.createId(fromPointQuayId));

    String toPointQuayId = quayIdByStopPointRef.lookup(serviceLink.getToPointRef().getRef());
    Stop toPointStop = stopById.get(idFactory.createId(toPointQuayId));

    if (fromPointStop == null || toPointStop == null) {
      issueStore.add(
        "ServiceLinkWithoutQuay",
        "Service link with missing or unknown quays. Link: %s",
        serviceLink
      );
    } else if (!fromPointStop.equals(tripPattern.getStop(stopIndex))) {
      issueStore.add(
        "ServiceLinkQuayMismatch",
        "Service link %s with quays different from point in journey pattern. Link point: %s, journey pattern point: %s",
        serviceLink,
        tripPattern.getStop(stopIndex).getId().getId(),
        fromPointQuayId
      );
      return null;
    } else if (!toPointStop.equals(tripPattern.getStop(stopIndex + 1))) {
      issueStore.add(
        "ServiceLinkQuayMismatch",
        "Service link %s with quays different to point in journey pattern. Link point: %s, journey pattern point: %s",
        serviceLink,
        tripPattern.getStop(stopIndex).getId().getId(),
        toPointQuayId
      );
      return null;
    }

    for (JAXBElement<?> projectionElement : serviceLink
      .getProjections()
      .getProjectionRefOrProjection()) {
      Object projectionObj = projectionElement.getValue();
      if (projectionObj instanceof LinkSequenceProjection_VersionStructure linkSequenceProjection) {
        if (linkSequenceProjection.getLineString() == null) {
          issueStore.add(
            "ServiceLinkWithoutLineString",
            "Ignore linkSequenceProjection without linestring for: %s",
            linkSequenceProjection
          );
          return null;
        }
        List<Double> coords = linkSequenceProjection.getLineString().getPosList().getValue();
        if (coords.size() < 4) {
          issueStore.add(
            "ServiceLinkGeometryError",
            "Ignore linkSequenceProjection with invalid linestring, " +
            "containing fewer than two coordinates for: %s",
            serviceLink.getId()
          );
          return null;
        } else if (coords.size() % 2 != 0) {
          issueStore.add(
            "ServiceLinkGeometryError",
            "Ignore linkSequenceProjection with invalid linestring, " +
            "containing odd number of values for coordinates: %s",
            serviceLink.getId()
          );
          return null;
        }

        Coordinate[] coordinates = new Coordinate[coords.size() / 2];
        for (int i = 0; i < coords.size(); i += 2) {
          coordinates[i / 2] = new Coordinate(coords.get(i + 1), coords.get(i));
        }
        final LineString geometry = geometryFactory.createLineString(coordinates);

        if (
          !isValid(
            geometry,
            tripPattern.getStop(stopIndex),
            tripPattern.getStop(stopIndex + 1),
            serviceLink.getId(),
            issueStore
          )
        ) {
          return null;
        }
        return geometry;
      }
    }

    issueStore.add(
      "ServiceLinkWithoutProjection",
      "Ignore ServiceLink without projection: %s",
      serviceLink.getId()
    );
    return null;
  }

  /** create a 2-point linestring (a straight line segment) between the two stops */
  private LineString createSimpleGeometry(StopLocation s0, StopLocation s1) {
    Coordinate[] coordinates = new Coordinate[] {
      s0.getCoordinate().asJtsCoordinate(),
      s1.getCoordinate().asJtsCoordinate(),
    };
    CoordinateSequence sequence = new PackedCoordinateSequence.Double(coordinates, 2);

    return geometryFactory.createLineString(sequence);
  }

  private boolean isValid(
    Geometry geometry,
    StopLocation s0,
    StopLocation s1,
    String id,
    DataImportIssueStore issueStore
  ) {
    Coordinate[] coordinates = geometry.getCoordinates();
    if (coordinates.length < 2) {
      issueStore.add(
        "ServiceLinkGeometryError",
        "Ignore linkSequenceProjection with invalid linestring, " +
        "containing fewer than two coordinates for: %s",
        id
      );
      return false;
    }
    if (geometry.getLength() == 0) {
      issueStore.add(
        "ServiceLinkGeometryError",
        "Ignore linkSequenceProjection with invalid linestring, having distance of 0 for: %s",
        id
      );
      return false;
    }
    for (Coordinate coordinate : coordinates) {
      if (Double.isNaN(coordinate.x) || Double.isNaN(coordinate.y)) {
        issueStore.add(
          "ServiceLinkGeometryError",
          "Ignore linkSequenceProjection with invalid linestring, " +
          "containing coordinate with NaN for: %s",
          id
        );
        return false;
      }
    }
    Coordinate geometryStartCoord = coordinates[0];
    Coordinate geometryEndCoord = coordinates[coordinates.length - 1];

    Coordinate startCoord = s0.getCoordinate().asJtsCoordinate();
    Coordinate endCoord = s1.getCoordinate().asJtsCoordinate();
    if (
      SphericalDistanceLibrary.fastDistance(startCoord, geometryStartCoord) >
      maxStopToShapeSnapDistance
    ) {
      issueStore.add(
        "ServiceLinkGeometryTooFar",
        "Ignore linkSequenceProjection with too long distance between stop and start of linestring, " +
        " stop %s, distance: %s, link id: %s",
        s0,
        SphericalDistanceLibrary.fastDistance(startCoord, geometryStartCoord),
        id
      );
      return false;
    } else if (
      SphericalDistanceLibrary.fastDistance(endCoord, geometryEndCoord) > maxStopToShapeSnapDistance
    ) {
      issueStore.add(
        "ServiceLinkGeometryTooFar",
        "Ignore linkSequenceProjection with too long distance between stop and end of linestring, " +
        " stop %s, distance: %s, link id: %s",
        s1,
        SphericalDistanceLibrary.fastDistance(endCoord, geometryEndCoord),
        id
      );
      return false;
    }
    return true;
  }
}
