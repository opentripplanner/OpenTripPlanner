package org.opentripplanner.netex.mapping;

import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.netex.index.api.NetexEntityIndexReadOnlyView;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.opentripplanner.netex.mapping.support.NetexMainAndSubMode;
import org.opentripplanner.transit.model.framework.EntityById;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.BikeAccess;
import org.opentripplanner.transit.model.network.GroupOfRoutes;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.RouteBuilder;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.organization.Branding;
import org.opentripplanner.transit.model.organization.Operator;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.BrandingRefStructure;
import org.rutebanken.netex.model.FlexibleLine_VersionStructure;
import org.rutebanken.netex.model.Line_VersionStructure;
import org.rutebanken.netex.model.Network;
import org.rutebanken.netex.model.OperatorRefStructure;
import org.rutebanken.netex.model.PresentationStructure;

/**
 * Maps NeTEx line to OTP Route.
 */
class RouteMapper {

  private final DataImportIssueStore issueStore;
  private final HexBinaryAdapter hexBinaryAdapter = new HexBinaryAdapter();
  private final TransportModeMapper transportModeMapper = new TransportModeMapper();

  private final FeedScopedIdFactory idFactory;
  private final EntityById<Agency> agenciesById;
  private final EntityById<Operator> operatorsById;
  private final EntityById<Branding> brandingsById;
  private final Multimap<FeedScopedId, GroupOfRoutes> groupsOfLinesByRouteId;
  private final EntityById<GroupOfRoutes> groupOfRoutesById;
  private final NetexEntityIndexReadOnlyView netexIndex;
  private final AuthorityToAgencyMapper authorityMapper;
  private final Set<String> ferryIdsNotAllowedForBicycle;

  RouteMapper(
    DataImportIssueStore issueStore,
    FeedScopedIdFactory idFactory,
    EntityById<Agency> agenciesById,
    EntityById<Operator> operatorsById,
    EntityById<Branding> brandingsById,
    Multimap<FeedScopedId, GroupOfRoutes> groupsOfLinesByRouteId,
    EntityById<GroupOfRoutes> groupOfRoutesById,
    NetexEntityIndexReadOnlyView netexIndex,
    String timeZone,
    Set<String> ferryIdsNotAllowedForBicycle
  ) {
    this.issueStore = issueStore;
    this.idFactory = idFactory;
    this.agenciesById = agenciesById;
    this.operatorsById = operatorsById;
    this.brandingsById = brandingsById;
    this.groupsOfLinesByRouteId = groupsOfLinesByRouteId;
    this.groupOfRoutesById = groupOfRoutesById;
    this.netexIndex = netexIndex;
    this.authorityMapper = new AuthorityToAgencyMapper(idFactory, timeZone);
    this.ferryIdsNotAllowedForBicycle = ferryIdsNotAllowedForBicycle;
  }

  Route mapRoute(Line_VersionStructure line) {
    RouteBuilder builder = Route.of(idFactory.createId(line.getId()));

    builder.getGroupsOfRoutes().addAll(getGroupOfRoutes(line));
    builder.withAgency(findOrCreateAuthority(line));
    builder.withOperator(findOperator(line));
    builder.withBranding(findBranding(line));
    NonLocalizedString longName = NonLocalizedString.ofNullable(line.getName().getValue());
    builder.withLongName(longName);
    builder.withShortName(line.getPublicCode());

    NetexMainAndSubMode mode;
    try {
      mode = transportModeMapper.map(line.getTransportMode(), line.getTransportSubmode());
    } catch (TransportModeMapper.UnsupportedModeException e) {
      issueStore.add(
        "UnsupportedModeInLine",
        "Unsupported mode in Line. Mode: %s, line: %s",
        e.mode,
        line.getId()
      );
      return null;
    }

    builder.withMode(mode.mainMode());
    builder.withNetexSubmode(mode.subMode());

    if (line instanceof FlexibleLine_VersionStructure) {
      builder.withFlexibleLineType(
        ((FlexibleLine_VersionStructure) line).getFlexibleLineType().value()
      );
    }

    if (line.getPresentation() != null) {
      PresentationStructure presentation = line.getPresentation();
      if (presentation.getColour() != null) {
        builder.withColor(hexBinaryAdapter.marshal(presentation.getColour()));
      }
      if (presentation.getTextColour() != null) {
        builder.withTextColor(hexBinaryAdapter.marshal(presentation.getTextColour()));
      }
    }

    // we would love to read this information from the actual feed but it's unclear where
    // this information would be located.
    // the standard defines the following enum for baggage
    // https://github.com/NeTEx-CEN/NeTEx/blob/0436cb774778ae68a682c28e0a21013e4886c883/xsd/netex_part_3/part3_fares/netex_usageParameterLuggage_support.xsd#L120
    // and there is a usage of that in one of the Entur examples
    // https://github.com/entur/profile-examples/blob/7f45e036c870205102d96ef58c7ce5008f4edcf1/netex/fares-sales/Entur_PARTIAL_EXAMPLE_INCOMPLETE_FinnmarkFullExample.xml#L1598-L1611
    // but currently it doesn't look it is being parsed.
    // until there is better information from the operators we assume that all ferries allow
    // bicycles on board.
    if (line.getTransportMode().equals(AllVehicleModesOfTransportEnumeration.WATER)) {
      if (ferryIdsNotAllowedForBicycle.contains(line.getId())) {
        builder.withBikesAllowed(BikeAccess.NOT_ALLOWED);
      } else {
        builder.withBikesAllowed(BikeAccess.ALLOWED);
      }
    }

    return builder.build();
  }

  private Collection<GroupOfRoutes> getGroupOfRoutes(Line_VersionStructure line) {
    // Relation can be set both on Line or on GroupOfLines
    // Look on both indexes so that nothing is omitted

    FeedScopedId lineId = idFactory.createId(line.getId());
    // Use set in case ref is set on both sides
    Set<GroupOfRoutes> groupsOfRoutes = new HashSet<>(groupsOfLinesByRouteId.get(lineId));

    FeedScopedId groupOfRoutesId = idFactory.createId(line.getRepresentedByGroupRef().getRef());
    if (this.groupOfRoutesById.containsKey(groupOfRoutesId)) {
      groupsOfRoutes.add(groupOfRoutesById.get(groupOfRoutesId));
    }

    return groupsOfRoutes;
  }

  /**
   * Find an agency by mapping the GroupOfLines/Network Authority. If no authority is found a dummy
   * agency is created and returned.
   */
  private Agency findOrCreateAuthority(Line_VersionStructure line) {
    String groupRef = line.getRepresentedByGroupRef().getRef();

    // Find authority, first in *GroupOfLines* and then if not found look in *Network*
    Network network = netexIndex.lookupNetworkForLine(groupRef);

    if (network != null) {
      String orgRef = network.getTransportOrganisationRef().getValue().getRef();
      Agency agency = agenciesById.get(idFactory.createId(orgRef));
      if (agency != null) return agency;
    }
    // No authority found in Network or GroupOfLines.
    // Use the dummy agency, create if necessary
    return createOrGetDummyAgency(line);
  }

  private Agency createOrGetDummyAgency(Line_VersionStructure line) {
    issueStore.add("LineWithoutAuthority", "No authority found for %s", line.getId());

    Agency agency = agenciesById.get(idFactory.createId(authorityMapper.dummyAgencyId()));

    if (agency == null) {
      agency = authorityMapper.createDummyAgency();
      agenciesById.add(agency);
    }
    return agency;
  }

  @Nullable
  private Operator findOperator(Line_VersionStructure line) {
    OperatorRefStructure opeRef = line.getOperatorRef();

    if (opeRef == null) {
      return null;
    }
    return operatorsById.get(idFactory.createId(opeRef.getRef()));
  }

  @Nullable
  private Branding findBranding(Line_VersionStructure line) {
    BrandingRefStructure brandingRef = line.getBrandingRef();

    if (brandingRef == null) {
      return null;
    }

    return brandingsById.get(idFactory.createId(brandingRef.getRef()));
  }
}
