package org.opentripplanner.netex.mapping;

import jakarta.xml.bind.JAXBElement;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import net.opengis.gml._3.LineStringType;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.impl.PackedCoordinateSequence;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issues.MissingProjectionInServiceLink;
import org.opentripplanner.netex.index.api.ReadOnlyHierarchicalMap;
import org.opentripplanner.netex.index.api.ReadOnlyHierarchicalMapById;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.opentripplanner.transit.model.framework.ImmutableEntityById;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.rutebanken.netex.model.JourneyPattern_VersionStructure;
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
  private final ImmutableEntityById<RegularStop> stopById;
  private final DataImportIssueStore issueStore;
  private final double maxStopToShapeSnapDistance;

  ServiceLinkMapper(
    FeedScopedIdFactory idFactory,
    ReadOnlyHierarchicalMapById<ServiceLink> serviceLinkById,
    ReadOnlyHierarchicalMap<String, String> quayIdByStopPointRef,
    ImmutableEntityById<RegularStop> stopById,
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

  List<LineString> getGeometriesByJourneyPattern(
    JourneyPattern_VersionStructure journeyPattern,
    StopPattern stopPattern
  ) {
    LineString[] geometries = generateGeometriesFromServiceLinks(journeyPattern, stopPattern);

    // Make sure all geometries are generated
    for (int i = 0; i < stopPattern.getSize() - 1; ++i) {
      if (geometries[i] == null) {
        geometries[i] = createSimpleGeometry(stopPattern.getStop(i), stopPattern.getStop(i + 1));
      }
    }
    return Arrays.asList(geometries);
  }

  private LineString[] generateGeometriesFromServiceLinks(
    JourneyPattern_VersionStructure journeyPattern,
    StopPattern stopPattern
  ) {
    LineString[] geometries = new LineString[stopPattern.getSize() - 1];
    if (journeyPattern.getLinksInSequence() == null) {
      return geometries;
    }
    List<LinkInLinkSequence_VersionedChildStructure> linksInJourneyPattern = journeyPattern
      .getLinksInSequence()
      .getServiceLinkInJourneyPatternOrTimingLinkInJourneyPattern();

    if (linksInJourneyPattern.size() != stopPattern.getSize() - 1) {
      issueStore.add(
        "WrongNumberOfServiceLinks",
        "The journey pattern %s should have exactly %d ServiceLinks",
        journeyPattern.getId(),
        stopPattern.getSize() - 1
      );
      return geometries;
    }

    for (int i = 0; i < linksInJourneyPattern.size(); i++) {
      var linkInLinkSequence = linksInJourneyPattern.get(i);
      if (
        linkInLinkSequence instanceof
        ServiceLinkInJourneyPattern_VersionedChildStructure serviceLinkInJourneyPattern
      ) {
        String serviceLinkRef = serviceLinkInJourneyPattern.getServiceLinkRef().getRef();
        ServiceLink serviceLink = serviceLinkById.lookup(serviceLinkRef);

        if (serviceLink != null) {
          geometries[i] = mapServiceLink(serviceLink, stopPattern, i);
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
    return geometries;
  }

  @Nullable
  private LineString mapServiceLink(
    ServiceLink serviceLink,
    StopPattern stopPattern,
    int stopIndex
  ) {
    if (
      serviceLink.getProjections() == null ||
      serviceLink.getProjections().getProjectionRefOrProjection() == null
    ) {
      issueStore.add(new MissingProjectionInServiceLink(serviceLink.getId()));
      return null;
    } else if (!isFromToPointRefsValid(serviceLink, stopPattern, stopIndex)) {
      return null;
    }

    for (JAXBElement<?> projectionElement : serviceLink
      .getProjections()
      .getProjectionRefOrProjection()) {
      Object projectionObj = projectionElement.getValue();
      if (projectionObj instanceof LinkSequenceProjection_VersionStructure linkSequenceProjection) {
        LineStringType lineString = linkSequenceProjection.getLineString();
        if (!isProjectionValid(lineString, serviceLink.getId())) {
          return null;
        }

        List<Double> positionList = lineString.getPosList().getValue();
        Coordinate[] coordinates = new Coordinate[positionList.size() / 2];
        for (int i = 0; i < positionList.size(); i += 2) {
          coordinates[i / 2] = new Coordinate(positionList.get(i + 1), positionList.get(i));
        }
        final LineString geometry = geometryFactory.createLineString(coordinates);

        if (
          !isGeometryValid(geometry, serviceLink.getId()) ||
          !areEndpointsWithinTolerance(
            geometry,
            stopPattern.getStop(stopIndex),
            stopPattern.getStop(stopIndex + 1),
            serviceLink.getId()
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

  private boolean isFromToPointRefsValid(
    ServiceLink serviceLink,
    StopPattern stopPattern,
    int stopIndex
  ) {
    String fromPointQuayId = quayIdByStopPointRef.lookup(serviceLink.getFromPointRef().getRef());
    RegularStop fromPointStop = stopById.get(idFactory.createId(fromPointQuayId));

    String toPointQuayId = quayIdByStopPointRef.lookup(serviceLink.getToPointRef().getRef());
    RegularStop toPointStop = stopById.get(idFactory.createId(toPointQuayId));

    if (fromPointStop == null || toPointStop == null) {
      issueStore.add(
        "ServiceLinkWithoutQuay",
        "Service link with missing or unknown quays. Link: %s",
        serviceLink
      );
      return false;
    } else if (!fromPointStop.equals(stopPattern.getStop(stopIndex))) {
      issueStore.add(
        "ServiceLinkQuayMismatch",
        "Service link %s with quays different from point in journey pattern. Link point: %s, journey pattern point: %s",
        serviceLink,
        stopPattern.getStop(stopIndex).getId().getId(),
        fromPointQuayId
      );
      return false;
    } else if (!toPointStop.equals(stopPattern.getStop(stopIndex + 1))) {
      issueStore.add(
        "ServiceLinkQuayMismatch",
        "Service link %s with quays different to point in journey pattern. Link point: %s, journey pattern point: %s",
        serviceLink,
        stopPattern.getStop(stopIndex).getId().getId(),
        toPointQuayId
      );
      return false;
    }
    return true;
  }

  private boolean isProjectionValid(LineStringType lineString, String id) {
    if (lineString == null) {
      issueStore.add(
        "ServiceLinkWithoutLineString",
        "Ignore linkSequenceProjection without linestring for: %s",
        id
      );
      return false;
    }
    List<Double> coordinates = lineString.getPosList().getValue();
    if (coordinates.size() < 4) {
      issueStore.add(
        "ServiceLinkGeometryError",
        "Ignore linkSequenceProjection with invalid linestring, " +
        "containing fewer than two coordinates for: %s",
        id
      );
      return false;
    } else if (coordinates.size() % 2 != 0) {
      issueStore.add(
        "ServiceLinkGeometryError",
        "Ignore linkSequenceProjection with invalid linestring, " +
        "containing odd number of values for coordinates: %s",
        id
      );
      return false;
    }
    return true;
  }

  private boolean isGeometryValid(Geometry geometry, String id) {
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
    return true;
  }

  private boolean areEndpointsWithinTolerance(
    Geometry geometry,
    StopLocation fromStop,
    StopLocation toStop,
    String id
  ) {
    Coordinate[] coordinates = geometry.getCoordinates();
    Coordinate geometryStartCoordinate = coordinates[0];
    Coordinate geometryEndCoordinate = coordinates[coordinates.length - 1];

    Coordinate startCoordinate = fromStop.getCoordinate().asJtsCoordinate();
    Coordinate endCoordinate = toStop.getCoordinate().asJtsCoordinate();
    if (
      SphericalDistanceLibrary.fastDistance(startCoordinate, geometryStartCoordinate) >
      maxStopToShapeSnapDistance
    ) {
      issueStore.add(
        "ServiceLinkGeometryTooFar",
        "Ignore linkSequenceProjection with too long distance between stop and start of linestring, " +
        " stop %s, distance: %s, link id: %s",
        fromStop,
        SphericalDistanceLibrary.fastDistance(startCoordinate, geometryStartCoordinate),
        id
      );
      return false;
    } else if (
      SphericalDistanceLibrary.fastDistance(endCoordinate, geometryEndCoordinate) >
      maxStopToShapeSnapDistance
    ) {
      issueStore.add(
        "ServiceLinkGeometryTooFar",
        "Ignore linkSequenceProjection with too long distance between stop and end of linestring, " +
        " stop %s, distance: %s, link id: %s",
        toStop,
        SphericalDistanceLibrary.fastDistance(endCoordinate, geometryEndCoordinate),
        id
      );
      return false;
    }
    return true;
  }
}
