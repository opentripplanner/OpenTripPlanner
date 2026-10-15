package org.opentripplanner.netex.mapping;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.i18n.NonLocalizedString;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.graph_builder.issue.service.DefaultDataImportIssueStore;
import org.opentripplanner.transit.model.framework.DefaultEntityById;
import org.opentripplanner.transit.model.site.Station;
import org.rutebanken.netex.model.GroupOfStopPlaces;
import org.rutebanken.netex.model.LocationStructure;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.ObjectFactory;
import org.rutebanken.netex.model.SimplePoint_VersionStructure;
import org.rutebanken.netex.model.StopPlaceRefStructure;
import org.rutebanken.netex.model.StopPlaceRefs_RelStructure;

class GroupOfStationsMapperTest {

  private static final String ID = "NSR:GroupOfStopPlaces:1";
  private static final String STOP_PLACE_ID = "NSR:StopPlace:1";
  private static final String DROPPED_STOP_PLACE_ID = "NSR:StopPlace:2";
  private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();

  private final DefaultDataImportIssueStore issueStore = new DefaultDataImportIssueStore();
  private final DefaultEntityById<Station> stations = new DefaultEntityById<>();
  private final GroupOfStationsMapper mapper = new GroupOfStationsMapper(
    issueStore,
    MappingSupport.ID_FACTORY,
    new DefaultEntityById<>(),
    stations
  );

  @Test
  void mapCoordinates() {
    var group = groupOfStopPlaces().withCentroid(point(59.9, 10.7));
    var result = mapper.map(group);
    assertEquals(59.9, result.getCoordinate().latitude());
    assertEquals(10.7, result.getCoordinate().longitude());
    assertThat(issueStore.listIssues()).isEmpty();
  }

  @Test
  void missingCoordinates() {
    var result = mapper.map(groupOfStopPlaces());
    assertNull(result.getCoordinate());
    assertThat(issueTypes()).containsExactly("GroupOfStationWithoutCoordinates");
  }

  /** An invalid centroid is ignored, the group is still mapped since the coordinate is optional. */
  @Test
  void invalidCoordinates() {
    var group = groupOfStopPlaces().withCentroid(point(56162332.0, 10.7));
    var result = mapper.map(group);
    assertEquals(ID, result.getId().getId());
    assertNull(result.getCoordinate());
    assertThat(issueTypes()).containsExactly("GroupOfStationInvalidCoordinates");
  }

  @Test
  void missingNameFallsBackToNameOfFirstMemberFound() {
    stations.add(
      Station.of(MappingSupport.ID_FACTORY.createId(STOP_PLACE_ID))
        .withName(new NonLocalizedString("Station"))
        .withCoordinate(59.9, 10.7)
        .build()
    );
    var group = unnamedGroupOfStopPlaces(DROPPED_STOP_PLACE_ID, STOP_PLACE_ID);
    assertEquals("Station", mapper.map(group).getName().toString());
    assertThat(issueTypes()).containsExactly(
      "GroupOfStopPlacesWithoutName",
      "GroupOfStationWithoutStations"
    );
  }

  /** A member may be missing, e.g. a StopPlace dropped because it has no valid coordinates. */
  @Test
  void missingNameWithoutAnyMemberFound() {
    var group = unnamedGroupOfStopPlaces(DROPPED_STOP_PLACE_ID);
    assertEquals("N/A", mapper.map(group).getName().toString());
    assertThat(issueTypes()).containsExactly(
      "GroupOfStopPlacesWithoutName",
      "GroupOfStationWithoutStations"
    );
  }

  @Test
  void missingNameWithoutMembers() {
    var group = unnamedGroupOfStopPlaces();
    assertEquals("N/A", mapper.map(group).getName().toString());
    assertThat(issueTypes()).containsExactly("GroupOfStopPlacesWithoutName");
  }

  @Test
  void nameWithoutValue() {
    var group = groupOfStopPlaces().withCentroid(point(59.9, 10.7));
    group.getName().setValue(null);
    assertEquals("N/A", mapper.map(group).getName().toString());
    assertThat(issueTypes()).containsExactly("GroupOfStopPlacesWithoutName");
  }

  private static GroupOfStopPlaces groupOfStopPlaces() {
    return new GroupOfStopPlaces().withId(ID).withName(new MultilingualString().withValue("Name"));
  }

  private static GroupOfStopPlaces unnamedGroupOfStopPlaces(String... memberIds) {
    var group = new GroupOfStopPlaces().withId(ID).withCentroid(point(59.9, 10.7));
    if (memberIds.length == 0) {
      return group;
    }
    var members = new StopPlaceRefs_RelStructure();
    for (String memberId : memberIds) {
      members.withStopPlaceRef(
        OBJECT_FACTORY.createStopPlaceRef(new StopPlaceRefStructure().withRef(memberId))
      );
    }
    return group.withMembers(members);
  }

  private static SimplePoint_VersionStructure point(double lat, double lon) {
    return new SimplePoint_VersionStructure().withLocation(
      new LocationStructure()
        .withLatitude(BigDecimal.valueOf(lat))
        .withLongitude(BigDecimal.valueOf(lon))
    );
  }

  private List<String> issueTypes() {
    return issueStore.listIssues().stream().map(DataImportIssue::getType).toList();
  }
}
