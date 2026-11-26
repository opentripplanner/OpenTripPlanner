package org.opentripplanner.netex.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opentripplanner.netex.NetexTestDataSupport.createCarPoolingService;
import static org.opentripplanner.netex.mapping.MappingSupport.ID_FACTORY;

import org.entur.netex2.model.CarPoolingService;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.DefaultEntityById;
import org.opentripplanner.transit.model.framework.EntityById;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.organization.Operator;

class CarPoolingServiceMapperTest {

  private static final String SERVICE_ID = "RID:CarPoolingService:1";
  private static final FeedScopedId ORGANISATION_ID = ID_FACTORY.createId("RID:Organisation:1");
  private static final String TIME_ZONE = "Europe/Oslo";

  private final EntityById<Agency> agenciesById = new DefaultEntityById<>();
  private final EntityById<Operator> operatorsById = new DefaultEntityById<>();
  private final CarPoolingServiceMapper mapper = new CarPoolingServiceMapper(
    ID_FACTORY,
    agenciesById,
    operatorsById,
    TIME_ZONE
  );
  private final CarPoolingService service = createCarPoolingService(
    SERVICE_ID,
    "Ride Along car pool",
    ORGANISATION_ID.getId()
  );

  @Test
  void serviceIsMappedToCarpoolRoute() {
    operatorsById.add(operator());

    Route route = mapper.mapRoute(service);

    assertEquals(ID_FACTORY.createId(SERVICE_ID), route.getId());
    assertEquals("Ride Along car pool", route.getLongName().toString());
    assertEquals(TransitMode.CARPOOL, route.getMode());
  }

  @Test
  void operatorOfServiceIsAlsoTheAgency() {
    operatorsById.add(operator());

    Route route = mapper.mapRoute(service);

    assertEquals(operator(), route.getOperator());
    assertEquals(ORGANISATION_ID, route.getAgency().getId());
    assertEquals("Ride Along", route.getAgency().getName());
    assertEquals(TIME_ZONE, route.getAgency().getTimezone().getId());
    assertEquals(route.getAgency(), agenciesById.get(ORGANISATION_ID));
  }

  @Test
  void authorityOfServiceIsTheAgency() {
    var agency = Agency.of(ORGANISATION_ID).withName("Authority").withTimezone(TIME_ZONE).build();
    agenciesById.add(agency);

    Route route = mapper.mapRoute(service);

    assertEquals(agency, route.getAgency());
    assertNull(route.getOperator());
  }

  @Test
  void serviceWithoutNameIsNamedAfterItsOrganisation() {
    operatorsById.add(operator());
    service.setName(null);

    Route route = mapper.mapRoute(service);

    assertEquals("Ride Along", route.getLongName().toString());
  }

  private static Operator operator() {
    return Operator.of(ORGANISATION_ID).withName("Ride Along").build();
  }
}
