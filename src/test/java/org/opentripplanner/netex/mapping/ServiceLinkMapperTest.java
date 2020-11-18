package org.opentripplanner.netex.mapping;

import net.opengis.gml._3.DirectPositionListType;
import net.opengis.gml._3.LineStringType;
import org.junit.Assert;
import org.junit.Test;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.model.ShapePoint;
import org.opentripplanner.netex.index.hierarchy.HierarchicalMap;
import org.opentripplanner.netex.index.hierarchy.HierarchicalMapById;
import org.opentripplanner.netex.index.hierarchy.HierarchicalVersionMapById;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.LinkSequenceProjection;
import org.rutebanken.netex.model.LinkSequenceProjection_VersionStructure;
import org.rutebanken.netex.model.LinksInJourneyPattern_RelStructure;
import org.rutebanken.netex.model.Projections_RelStructure;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.ServiceLink;
import org.rutebanken.netex.model.ServiceLinkInJourneyPattern_VersionedChildStructure;
import org.rutebanken.netex.model.ServiceLinkRefStructure;

import javax.xml.bind.JAXBElement;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ServiceLinkMapperTest {

  private static Double[] COORDINATES = { 59.90929, 10.74527, 59.90893, 10.74493, 59.90870, 10.74585 };

  @Test
  public void mapServiceLinks() {
    JourneyPattern journeyPattern = new JourneyPattern().withId("RUT:JourneyPattern:1300");

    journeyPattern.setLinksInSequence(
        new LinksInJourneyPattern_RelStructure()
            .withServiceLinkInJourneyPatternOrTimingLinkInJourneyPattern(
              new ServiceLinkInJourneyPattern_VersionedChildStructure().withServiceLinkRef(
                new ServiceLinkRefStructure()
                  .withRef("RUT:ServiceLink:1")))
            .withServiceLinkInJourneyPatternOrTimingLinkInJourneyPattern(
              new ServiceLinkInJourneyPattern_VersionedChildStructure().withServiceLinkRef(
                new ServiceLinkRefStructure()
                  .withRef("RUT:ServiceLink:2"))));

    ServiceLink serviceLink1 = createServiceLink(
        "RUT:ServiceLink:1",
        200.0,
        new Double[] { COORDINATES[0], COORDINATES[1], COORDINATES[2], COORDINATES[3] }
    );
    ServiceLink serviceLink2 = createServiceLink(
        "RUT:ServiceLink:2",
        100.0,
        new Double[] { COORDINATES[2], COORDINATES[3], COORDINATES[4], COORDINATES[5] });

    HierarchicalMapById<ServiceLink> serviceLinksById = new HierarchicalMapById<>();
    serviceLinksById.add(serviceLink1);
    serviceLinksById.add(serviceLink2);

    Quay quay1 = new Quay().withId("NSR:Quay:1");
    Quay quay2 = new Quay().withId("NSR:Quay:2");
    Quay quay3 = new Quay().withId("NSR:Quay:3");

    HierarchicalVersionMapById<Quay> quaysById = new HierarchicalVersionMapById<>();
    quaysById.add(quay1);
    quaysById.add(quay2);
    quaysById.add(quay3);

    HierarchicalMap<String, String> quayIdByStopPointRef = new HierarchicalMap<>();
    quayIdByStopPointRef.add("RUT:StopPoint:1", "NSR:Quay:1");
    quayIdByStopPointRef.add("RUT:StopPoint:2", "NSR:Quay:2");
    quayIdByStopPointRef.add("RUT:StopPoint:3", "NSR:Quay:3");

    ServiceLinkMapper serviceLinkMapper = new ServiceLinkMapper(
        new FeedScopedIdFactory("RB"),
        new DataImportIssueStore(false)
    );

    Collection<ShapePoint> shapePoints = serviceLinkMapper.getShapePointsByJourneyPattern(journeyPattern,
        serviceLinksById,
        quayIdByStopPointRef,
        quaysById
    );

    List<ShapePoint> shapePointList = shapePoints.stream()
        .sorted(Comparator.comparing(ShapePoint::getSequence)).collect(Collectors.toList());

    Assert.assertEquals(COORDINATES[0], shapePointList.get(0).getLat(), 0.0001);
    Assert.assertEquals(COORDINATES[1], shapePointList.get(0).getLon(), 0.0001);
    Assert.assertEquals(COORDINATES[2], shapePointList.get(1).getLat(), 0.0001);
    Assert.assertEquals(COORDINATES[3], shapePointList.get(1).getLon(), 0.0001);
    Assert.assertEquals(COORDINATES[2], shapePointList.get(2).getLat(), 0.0001);
    Assert.assertEquals(COORDINATES[3], shapePointList.get(2).getLon(), 0.0001);
    Assert.assertEquals(COORDINATES[4], shapePointList.get(3).getLat(), 0.0001);
    Assert.assertEquals(COORDINATES[5], shapePointList.get(3).getLon(), 0.0001);
  }

  private ServiceLink createServiceLink(String id, double distance, Double[] coordinates) {
    DirectPositionListType directPositionListType = new DirectPositionListType().withValue(coordinates);
    LinkSequenceProjection linkSequenceProjection = new LinkSequenceProjection()
        .withLineString(new LineStringType().withPosList(directPositionListType));
    JAXBElement<LinkSequenceProjection_VersionStructure> linkSequenceProjection_versionStructure =
        MappingSupport.createJaxbElement(linkSequenceProjection);
    Projections_RelStructure projections_relStructure =
        new Projections_RelStructure()
            .withProjectionRefOrProjection(linkSequenceProjection_versionStructure);

    return new ServiceLink()
        .withId(id)
        .withDistance(new BigDecimal(distance))
        .withProjections(projections_relStructure);
  }
}
