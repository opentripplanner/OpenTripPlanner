package org.opentripplanner.netex.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.netex.mapping.MappingSupport.ID_FACTORY;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBElement;
import net.opengis.gml._3.DirectPositionListType;
import net.opengis.gml._3.LineStringType;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.netex.index.hierarchy.HierarchicalMap;
import org.opentripplanner.netex.index.hierarchy.HierarchicalMapById;
import org.opentripplanner.netex.mapping.support.NetexMainAndSubMode;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.EntityById;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.LinkSequenceProjection;
import org.rutebanken.netex.model.LinkSequenceProjection_VersionStructure;
import org.rutebanken.netex.model.LinksInJourneyPattern_RelStructure;
import org.rutebanken.netex.model.LocationStructure;
import org.rutebanken.netex.model.Projections_RelStructure;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.ScheduledStopPointRefStructure;
import org.rutebanken.netex.model.ServiceLink;
import org.rutebanken.netex.model.ServiceLinkInJourneyPattern_VersionedChildStructure;
import org.rutebanken.netex.model.ServiceLinkRefStructure;
import org.rutebanken.netex.model.SimplePoint_VersionStructure;

public class ServiceLinkMapperTest {

  private static final Double[] COORDINATES = {
    59.90929,
    10.74527,
    59.90893,
    10.74493,
    59.90870,
    10.74585,
  };

  @Test
  public void mapServiceLinks() {
    JourneyPattern journeyPattern = new JourneyPattern().withId("RUT:JourneyPattern:1300");

    journeyPattern.setLinksInSequence(
      new LinksInJourneyPattern_RelStructure()
        .withServiceLinkInJourneyPatternOrTimingLinkInJourneyPattern(
          new ServiceLinkInJourneyPattern_VersionedChildStructure()
            .withServiceLinkRef(new ServiceLinkRefStructure().withRef("RUT:ServiceLink:1"))
        )
        .withServiceLinkInJourneyPatternOrTimingLinkInJourneyPattern(
          new ServiceLinkInJourneyPattern_VersionedChildStructure()
            .withServiceLinkRef(new ServiceLinkRefStructure().withRef("RUT:ServiceLink:2"))
        )
    );

    ServiceLink serviceLink1 = createServiceLink(
      "RUT:ServiceLink:1",
      "RUT:StopPoint:1",
      "RUT:StopPoint:2",
      new Double[] { COORDINATES[0], COORDINATES[1], COORDINATES[2], COORDINATES[3] }
    );
    ServiceLink serviceLink2 = createServiceLink(
      "RUT:ServiceLink:2",
      "RUT:StopPoint:2",
      "RUT:StopPoint:3",
      new Double[] { COORDINATES[2], COORDINATES[3], COORDINATES[4], COORDINATES[5] }
    );

    HierarchicalMapById<ServiceLink> serviceLinksById = new HierarchicalMapById<>();
    serviceLinksById.add(serviceLink1);
    serviceLinksById.add(serviceLink2);

    Quay quay1 = new Quay().withId("NSR:Quay:1").withCentroid(getLocation(59.9093, 10.7453));
    Quay quay2 = new Quay().withId("NSR:Quay:2").withCentroid(getLocation(59.9089, 10.7449));
    Quay quay3 = new Quay().withId("NSR:Quay:3").withCentroid(getLocation(59.9087, 10.7459));

    List<Quay> quaysById = new ArrayList<>();
    quaysById.add(quay1);
    quaysById.add(quay2);
    quaysById.add(quay3);

    HierarchicalMap<String, String> quayIdByStopPointRef = new HierarchicalMap<>();
    quayIdByStopPointRef.add("RUT:StopPoint:1", "NSR:Quay:1");
    quayIdByStopPointRef.add("RUT:StopPoint:2", "NSR:Quay:2");
    quayIdByStopPointRef.add("RUT:StopPoint:3", "NSR:Quay:3");

    EntityById<RegularStop> stopsById = new EntityById<>();

    DataImportIssueStore issueStore = DataImportIssueStore.NOOP;
    QuayMapper quayMapper = new QuayMapper(ID_FACTORY, issueStore);
    StopPattern.StopPatternBuilder stopPatternBuilder = StopPattern.create(3);

    Station parentStation = Station
      .of(ID_FACTORY.createId("NSR:StopArea:1"))
      .withName(NonLocalizedString.ofNullable("Parent Station"))
      .withCoordinate(59.908, 10.745)
      .build();

    for (int i = 0; i < quaysById.size(); i++) {
      RegularStop stop = quayMapper.mapQuayToStop(
        quaysById.get(i),
        parentStation,
        List.of(),
        new NetexMainAndSubMode(TransitMode.BUS, "UNKNOWN"),
        Accessibility.NO_INFORMATION
      );
      stopPatternBuilder.stops[i] = stop;
      stopsById.add(stop);
    }

    ServiceLinkMapper serviceLinkMapper = new ServiceLinkMapper(
      ID_FACTORY,
      serviceLinksById,
      quayIdByStopPointRef,
      stopsById,
      issueStore,
      150
    );

    List<LineString> shape = serviceLinkMapper.getGeometriesByJourneyPattern(
      journeyPattern,
      stopPatternBuilder.build()
    );

    Coordinate[] coordinates = shape.get(0).getCoordinates();

    assertEquals(0, issueStore.listIssues().size());

    assertEquals(COORDINATES[0], coordinates[0].getY(), 0.000001);
    assertEquals(COORDINATES[1], coordinates[0].getX(), 0.000001);
    assertEquals(COORDINATES[2], coordinates[1].getY(), 0.000001);
    assertEquals(COORDINATES[3], coordinates[1].getX(), 0.000001);

    coordinates = shape.get(1).getCoordinates();

    assertEquals(COORDINATES[2], coordinates[0].getY(), 0.000001);
    assertEquals(COORDINATES[3], coordinates[0].getX(), 0.000001);
    assertEquals(COORDINATES[4], coordinates[1].getY(), 0.000001);
    assertEquals(COORDINATES[5], coordinates[1].getX(), 0.000001);
  }

  private SimplePoint_VersionStructure getLocation(double latitude, double longitude) {
    return new SimplePoint_VersionStructure()
      .withLocation(
        new LocationStructure()
          .withLongitude(BigDecimal.valueOf(longitude))
          .withLatitude(BigDecimal.valueOf(latitude))
      );
  }

  private ServiceLink createServiceLink(String id, String from, String to, Double[] coordinates) {
    DirectPositionListType directPositionListType = new DirectPositionListType()
      .withValue(coordinates);
    LinkSequenceProjection linkSequenceProjection = new LinkSequenceProjection()
      .withLineString(new LineStringType().withPosList(directPositionListType));
    JAXBElement<LinkSequenceProjection_VersionStructure> linkSequenceProjection_versionStructure = MappingSupport.createJaxbElement(
      linkSequenceProjection
    );
    Projections_RelStructure projections_relStructure = new Projections_RelStructure()
      .withProjectionRefOrProjection(linkSequenceProjection_versionStructure);

    return new ServiceLink()
      .withId(id)
      .withFromPointRef(new ScheduledStopPointRefStructure().withRef(from))
      .withToPointRef(new ScheduledStopPointRefStructure().withRef(to))
      .withProjections(projections_relStructure);
  }
}
