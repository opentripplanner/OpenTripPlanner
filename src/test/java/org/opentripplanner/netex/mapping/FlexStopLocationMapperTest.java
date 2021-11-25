package org.opentripplanner.netex.mapping;

import net.opengis.gml._3.AbstractRingPropertyType;
import net.opengis.gml._3.DirectPositionListType;
import net.opengis.gml._3.LinearRingType;
import net.opengis.gml._3.PolygonType;
import org.junit.Test;
import org.opentripplanner.model.FlexLocationGroup;
import org.opentripplanner.model.FlexStopLocation;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.WgsCoordinate;
import org.opentripplanner.model.WheelChairBoarding;
import org.rutebanken.netex.model.FlexibleArea;
import org.rutebanken.netex.model.FlexibleStopPlace;
import org.rutebanken.netex.model.FlexibleStopPlace_VersionStructure;
import org.rutebanken.netex.model.KeyListStructure;
import org.rutebanken.netex.model.KeyValueStructure;
import org.rutebanken.netex.model.MultilingualString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.opentripplanner.netex.mapping.MappingSupport.ID_FACTORY;

public class FlexStopLocationMapperTest {

  public static final String FLEXIBLE_STOP_PLACE_ID = "RUT:FlexibleStopPlace:1";
  public static final String FLEXIBLE_STOP_PLACE_NAME = "Sauda-HentMeg";
  public static final String FLEXIBLE_AREA_ID = "RUT:FlexibleArea:1";
  public static final Collection<Double> areaPosList = new ArrayList<>(Arrays.asList(
      59.62575084033623,
      6.3023991052849,
      59.62883380609349,
      6.289718020117876,
      59.6346950024935,
      6.293494451572027,
      59.63493377028342,
      6.295211011323889,
      59.638287192982595,
      6.294073790488267,
      59.64753178824841,
      6.311475414973009,
      59.65024392097467,
      6.317762315064251,
      59.6531402366151,
      6.322203913422278,
      59.65512520740007,
      6.327847103606584,
      59.65622305339289,
      6.354024123279035,
      59.67015747207181,
      6.344389931671587,
      59.67371334545938,
      6.353938295291437,
      59.669853904428514,
      6.359452743494389,
      59.659335756554185,
      6.369387333058425,
      59.667522846995965,
      6.393891223516542,
      59.67128498496401,
      6.408267411438566,
      59.671057317147415,
      6.416742925213495,
      59.669940638321414,
      6.423759363199314,
      59.66409644047066,
      6.421935518462957,
      59.662383105889404,
      6.418995909887838,
      59.64933503529621,
      6.391552410854604,
      59.642120082714285,
      6.3725629685993965,
      59.63698730002605,
      6.317783255517423,
      59.62575084033623,
      6.3023991052849
  ));

  @Test
  public void mapFlexStopLocation() {

    FlexStopLocationMapper flexStopLocationMapper = new FlexStopLocationMapper(ID_FACTORY, List.of());

    FlexibleStopPlace flexibleStopPlace = getFlexibleStopPlace();

    FlexStopLocation flexStopLocation = (FlexStopLocation) flexStopLocationMapper.map(flexibleStopPlace);

    assertNotNull(flexStopLocation);
  }

  @Test
  public void mapFlexStopLocationGroup() {

    Stop stop1 = getStop("1", 59.6505778, 6.3608759);
    Stop stop2 = getStop("2", 59.6630333, 6.3697245);

    FlexStopLocationMapper flexStopLocationMapper = new FlexStopLocationMapper(
        ID_FACTORY,
        List.of(stop1, stop2)
    );

    FlexibleStopPlace flexibleStopPlace = getFlexibleStopPlace();
    flexibleStopPlace.setKeyList(new KeyListStructure().withKeyValue(
        new KeyValueStructure()
          .withKey("FlexibleStopAreaType")
          .withValue("UnrestrictedPublicTransportAreas")));

    FlexLocationGroup flexLocationGroup = (FlexLocationGroup) flexStopLocationMapper.map(
        flexibleStopPlace);

    // Only one of the stops should be inside the polygon
    assertEquals(1, flexLocationGroup.getLocations().size());

    assertNotNull(flexLocationGroup);
  }

  private FlexibleStopPlace getFlexibleStopPlace() {
    return new FlexibleStopPlace()
        .withId(FLEXIBLE_STOP_PLACE_ID)
        .withName(new MultilingualString().withValue(FLEXIBLE_STOP_PLACE_NAME))
        .withAreas(new FlexibleStopPlace_VersionStructure.Areas().withFlexibleAreaOrFlexibleAreaRefOrHailAndRideArea(
            new FlexibleArea()
                .withId(FLEXIBLE_AREA_ID)
                .withPolygon(new PolygonType().withExterior(new AbstractRingPropertyType().withAbstractRing(
                    MappingSupport.createJaxbElement(new LinearRingType().withPosList(new DirectPositionListType()
                        .withValue(areaPosList))))))));
  }

  private Stop getStop(String id, double latitude, double longitude) {
    return new Stop(
        ID_FACTORY.createId(id),
        id,
        id,
        null,
        WgsCoordinate.creatOptionalCoordinate(latitude, longitude),
        WheelChairBoarding.NO_INFORMATION,
        null,
        null,
        null,
        null,
        null,
        null
    );
  }
}
