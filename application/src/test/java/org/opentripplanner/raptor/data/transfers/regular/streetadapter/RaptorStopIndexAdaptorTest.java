package org.opentripplanner.raptor.data.transfers.regular.streetadapter;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.core.model.id.FeedScopedIdForTestFactory;
import org.opentripplanner.raptor.spi.RaptorConstants;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.transfer.streetadapter.RaptorStopIndexAdaptor;
import org.opentripplanner.transit.service.SiteRepository;

class RaptorStopIndexAdaptorTest {

  private static final WgsCoordinate COORDINATE = new WgsCoordinate(60.0, 11.0);
  private static final FeedScopedId STOP_ID = FeedScopedIdForTestFactory.id("STOP");
  private static final FeedScopedId AREA_STOP_ID = FeedScopedIdForTestFactory.id("AREA");

  private final SiteRepository siteRepository;
  private final int stopIndex;
  private final int areaStopIndex;
  private final RaptorStopIndexAdaptor subject;

  RaptorStopIndexAdaptorTest() {
    var builder = SiteRepository.of();
    var stop = builder.regularStop(STOP_ID).withCoordinate(COORDINATE).build();
    var areaStop = builder
      .areaStop(AREA_STOP_ID)
      .withGeometry(GeometryUtils.getGeometryFactory().createPoint(COORDINATE.asJtsCoordinate()))
      .build();
    siteRepository = builder.withRegularStop(stop).withAreaStop(areaStop).build();
    stopIndex = stop.getIndex();
    areaStopIndex = areaStop.getIndex();
    subject = new RaptorStopIndexAdaptor(siteRepository);
  }

  @Test
  void sizeIsTheSizeOfTheSharedIndexSpace() {
    assertThat(subject.size()).isEqualTo(siteRepository.stopIndexSize());
  }

  @Test
  void mapsRegularStopBetweenIdAndIndex() {
    assertThat(subject.toStopIndex(STOP_ID)).isEqualTo(stopIndex);
    assertThat(subject.toStopId(stopIndex)).isEqualTo(STOP_ID);
  }

  @Test
  void unknownIdIsNotFound() {
    assertThat(subject.toStopIndex(FeedScopedIdForTestFactory.id("UNKNOWN"))).isEqualTo(
      RaptorConstants.NOT_FOUND
    );
  }

  @Test
  void flexStopsAreNotInTheIndex() {
    assertThat(subject.toStopIndex(AREA_STOP_ID)).isEqualTo(RaptorConstants.NOT_FOUND);
    assertThat(subject.toStopId(areaStopIndex)).isNull();
  }

  @Test
  void indexOutOfRangeHasNoId() {
    assertThat(subject.toStopId(-1)).isNull();
    assertThat(subject.toStopId(subject.size())).isNull();
  }
}
