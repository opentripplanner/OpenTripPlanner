package org.opentripplanner.netex.mapping;

import org.entur.netex2.model.CarPoolingService;
import org.opentripplanner.core.model.i18n.NonLocalizedString;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.EntityById;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.organization.Operator;

/**
 * Maps a NeTEx CarPoolingService to an OTP Route with the mode carpool. Car pooling has no line in
 * NeTEx. The service takes that role, it is the named offer the journeys belong to.
 */
class CarPoolingServiceMapper {

  private final FeedScopedIdFactory idFactory;
  private final EntityById<Agency> agenciesById;
  private final EntityById<Operator> operatorsById;
  private final String timeZone;

  CarPoolingServiceMapper(
    FeedScopedIdFactory idFactory,
    EntityById<Agency> agenciesById,
    EntityById<Operator> operatorsById,
    String timeZone
  ) {
    this.idFactory = idFactory;
    this.agenciesById = agenciesById;
    this.operatorsById = operatorsById;
    this.timeZone = timeZone;
  }

  Route mapRoute(CarPoolingService service) {
    FeedScopedId organisationId = idFactory.createId(
      service.getOrganisationRef().getValue().getRef()
    );
    Agency agency = findOrCreateAgency(organisationId);
    String name = MultilingualStringMapper.nullableValueOf(service.getName());

    return Route.of(idFactory.createId(service.getId()))
      .withAgency(agency)
      .withOperator(operatorsById.get(organisationId))
      .withLongName(new NonLocalizedString(name == null ? agency.getName() : name))
      .withMode(TransitMode.CARPOOL)
      .build();
  }

  /**
   * The organisation of a service is an authority or an operator. A route must have an agency, so
   * an operator is used as the agency as well.
   */
  private Agency findOrCreateAgency(FeedScopedId organisationId) {
    Agency agency = agenciesById.get(organisationId);
    if (agency == null) {
      Operator operator = operatorsById.get(organisationId);
      agency = Agency.of(organisationId)
        .withName(operator.getName())
        .withUrl(operator.getUrl())
        .withPhone(operator.getPhone())
        .withTimezone(timeZone)
        .build();
      agenciesById.add(agency);
    }
    return agency;
  }
}
