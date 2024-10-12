package org.opentripplanner.netex.loader.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.netex.index.NetexEntityIndex;
import org.rutebanken.netex.model.FareFrame_VersionFrameStructure;
import org.rutebanken.netex.model.FareZone;
import org.rutebanken.netex.model.TariffZone_VersionStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FareFrameParser extends NetexParser<FareFrame_VersionFrameStructure> {

  private static final Logger LOG = LoggerFactory.getLogger(FareFrameParser.class);

  private final Collection<TariffZone_VersionStructure> fareZones = new ArrayList<>();

  @Override
  void parse(FareFrame_VersionFrameStructure frame) {
    if (frame.getFareZones() != null) {
      parseFareZones(frame.getFareZones().getFareZone());
    }

    // Keep list sorted alphabetically
    warnOnMissingMapping(LOG, frame.getAccessRightParameterAssignments());
    warnOnMissingMapping(LOG, frame.getBorderPoints());
    warnOnMissingMapping(LOG, frame.getControllableElements());
    warnOnMissingMapping(LOG, frame.getDistanceMatrixElements());
    warnOnMissingMapping(LOG, frame.getDistributionAssignments());
    warnOnMissingMapping(LOG, frame.getDistributionChannels());
    warnOnMissingMapping(LOG, frame.getFareProducts());
    warnOnMissingMapping(LOG, frame.getFareScheduledStopPoints());
    warnOnMissingMapping(LOG, frame.getFareSections());
    warnOnMissingMapping(LOG, frame.getFareStructureElements());
    warnOnMissingMapping(LOG, frame.getFareTables());
    warnOnMissingMapping(LOG, frame.getFulfilmentMethods());
    warnOnMissingMapping(LOG, frame.getGeographicalIntervals());
    warnOnMissingMapping(LOG, frame.getGeographicalStructureFactors());
    warnOnMissingMapping(LOG, frame.getGeographicalUnits());
    warnOnMissingMapping(LOG, frame.getGroupOfDistributionAssignments());
    warnOnMissingMapping(LOG, frame.getGroupsOfDistanceMatrixElements());
    warnOnMissingMapping(LOG, frame.getGroupsOfDistributionChannels());
    warnOnMissingMapping(LOG, frame.getGroupsOfSalesOfferPackages());
    warnOnMissingMapping(LOG, frame.getMode());
    warnOnMissingMapping(LOG, frame.getNoticeAssignments());
    warnOnMissingMapping(LOG, frame.getNotices());
    warnOnMissingMapping(LOG, frame.getParkingTariffs());
    warnOnMissingMapping(LOG, frame.getPriceGroups());
    warnOnMissingMapping(LOG, frame.getPricingParameterSet());
    warnOnMissingMapping(LOG, frame.getQualityStructureFactors());
    warnOnMissingMapping(LOG, frame.getSalesOfferPackageElements());
    warnOnMissingMapping(LOG, frame.getSalesOfferPackages());
    warnOnMissingMapping(LOG, frame.getSalesOfferPackageSubstitutions());
    warnOnMissingMapping(LOG, frame.getSeriesConstraints());
    warnOnMissingMapping(LOG, frame.getTariffs());
    warnOnMissingMapping(LOG, frame.getTimeIntervals());
    warnOnMissingMapping(LOG, frame.getTimeStructureFactors());
    warnOnMissingMapping(LOG, frame.getTimeUnits());
    warnOnMissingMapping(LOG, frame.getTransportOrganisationRef());
    warnOnMissingMapping(LOG, frame.getTypesOfTravelDocuments());
    warnOnMissingMapping(LOG, frame.getUsageParameters());
    warnOnMissingMapping(LOG, frame.getValidableElements());

    verifyCommonUnusedPropertiesIsNotSet(LOG, frame);
  }

  @Override
  void setResultOnIndex(NetexEntityIndex netexIndex) {
    netexIndex.tariffZonesById.addAll(fareZones);
  }

  private void parseFareZones(List<FareZone> fareZone) {
    fareZones.addAll(fareZone);
  }
}
