package org.opentripplanner.netex.loader.parser;

import static org.opentripplanner.netex.config.IgnorableFeature.PARKING;

import jakarta.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.netex.config.IgnorableFeature;
import org.opentripplanner.netex.index.NetexEntityIndex;
import org.opentripplanner.netex.support.JAXBUtils;
import org.rutebanken.netex.model.FlexibleStopPlace;
import org.rutebanken.netex.model.GroupOfStopPlaces;
import org.rutebanken.netex.model.Parking;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.Quays_RelStructure;
import org.rutebanken.netex.model.Site_VersionFrameStructure;
import org.rutebanken.netex.model.Site_VersionStructure;
import org.rutebanken.netex.model.StopPlace;
import org.rutebanken.netex.model.TariffZone;
import org.rutebanken.netex.model.TariffZone_VersionStructure;
import org.rutebanken.netex.model.Zone_VersionStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SiteFrameParser extends NetexParser<Site_VersionFrameStructure> {

  private static final Logger LOG = LoggerFactory.getLogger(SiteFrameParser.class);

  private final Collection<FlexibleStopPlace> flexibleStopPlaces = new ArrayList<>();

  private final Collection<GroupOfStopPlaces> groupsOfStopPlaces = new ArrayList<>();

  private final Collection<StopPlace> multiModalStopPlaces = new ArrayList<>();

  private final Collection<StopPlace> stopPlaces = new ArrayList<>();

  private final Collection<TariffZone_VersionStructure> tariffZones = new ArrayList<>();

  private final Collection<Quay> quays = new ArrayList<>();

  private final Collection<Parking> parkings = new ArrayList<>(0);

  private final Set<IgnorableFeature> ignoredFeatures;

  SiteFrameParser(Set<IgnorableFeature> ignoredFeatures) {
    this.ignoredFeatures = ignoredFeatures;
  }

  @Override
  public void parse(Site_VersionFrameStructure frame) {
    if (frame.getStopPlaces() != null) {
      parseStopPlaces(frame.getStopPlaces().getStopPlace_());
    }
    if (frame.getGroupsOfStopPlaces() != null) {
      parseGroupsOfStopPlaces(frame.getGroupsOfStopPlaces().getGroupOfStopPlaces());
    }
    if (OTPFeature.FlexRouting.isOn() && frame.getFlexibleStopPlaces() != null) {
      parseFlexibleStopPlaces(frame.getFlexibleStopPlaces().getFlexibleStopPlace());
    }

    if (frame.getTariffZones() != null) {
      parseTariffZones(frame.getTariffZones().getTariffZone());
    }

    if (!ignoredFeatures.contains(PARKING) && frame.getParkings() != null) {
      parseParkings(frame.getParkings().getParking());
    }
    // Keep list sorted alphabetically
    warnOnMissingMapping(LOG, frame.getAccesses());
    warnOnMissingMapping(LOG, frame.getAddresses());
    warnOnMissingMapping(LOG, frame.getCountries());
    warnOnMissingMapping(LOG, frame.getCheckConstraints());
    warnOnMissingMapping(LOG, frame.getCheckConstraintDelays());
    warnOnMissingMapping(LOG, frame.getCheckConstraintThroughputs());
    warnOnMissingMapping(LOG, frame.getNavigationPaths());
    warnOnMissingMapping(LOG, frame.getPathJunctions());
    warnOnMissingMapping(LOG, frame.getPathLinks());
    warnOnMissingMapping(LOG, frame.getPointsOfInterest());
    warnOnMissingMapping(LOG, frame.getPointOfInterestClassifications());
    warnOnMissingMapping(LOG, frame.getPointOfInterestClassificationHierarchies());
    warnOnMissingMapping(LOG, frame.getSiteFacilitySets());
    warnOnMissingMapping(LOG, frame.getTopographicPlaces());

    verifyCommonUnusedPropertiesIsNotSet(LOG, frame);
  }

  @Override
  void setResultOnIndex(NetexEntityIndex netexIndex) {
    netexIndex.flexibleStopPlaceById.addAll(flexibleStopPlaces);
    netexIndex.groupOfStopPlacesById.addAll(groupsOfStopPlaces);
    netexIndex.multiModalStopPlaceById.addAll(multiModalStopPlaces);
    netexIndex.stopPlaceById.addAll(stopPlaces);
    netexIndex.tariffZonesById.addAll(tariffZones);
    netexIndex.quayById.addAll(quays);
    netexIndex.parkings.addAll(parkings);
  }

  private void parseFlexibleStopPlaces(Collection<FlexibleStopPlace> flexibleStopPlacesList) {
    flexibleStopPlaces.addAll(flexibleStopPlacesList);
  }

  private void parseGroupsOfStopPlaces(Collection<GroupOfStopPlaces> groupsOfStopPlacesList) {
    groupsOfStopPlaces.addAll(groupsOfStopPlacesList);
  }

  private void parseParkings(List<Parking> parking) {
    parkings.addAll(parking);
  }

  private void parseStopPlaces(List<JAXBElement<? extends Site_VersionStructure>> stopPlaceList) {
    for (JAXBElement<? extends Site_VersionStructure> jaxBStopPlace : stopPlaceList) {
      StopPlace stopPlace = (StopPlace) jaxBStopPlace.getValue();
      if (isMultiModalStopPlace(stopPlace)) {
        multiModalStopPlaces.add(stopPlace);
      } else {
        stopPlaces.add(stopPlace);
        parseQuays(stopPlace.getQuays());
      }
    }
  }

  private void parseTariffZones(List<JAXBElement<? extends Zone_VersionStructure>> tariffZoneList) {
    for (JAXBElement<? extends Zone_VersionStructure> tariffZone : tariffZoneList) {
      if (tariffZone.getValue() instanceof TariffZone) {
        tariffZones.add((TariffZone) tariffZone.getValue());
      }
    }
  }

  private void parseQuays(Quays_RelStructure quayRefOrQuay) {
    if (quayRefOrQuay == null) {
      return;
    }
    JAXBUtils.forEachJAXBElementValue(Quay.class, quayRefOrQuay.getQuayRefOrQuay(), quays::add);
  }

  private boolean isMultiModalStopPlace(StopPlace stopPlace) {
    return (
      stopPlace.getKeyList() != null &&
      stopPlace
        .getKeyList()
        .getKeyValue()
        .stream()
        .anyMatch(
          keyValueStructure ->
            keyValueStructure.getKey().equals("IS_PARENT_STOP_PLACE") &&
            keyValueStructure.getValue().equals("true")
        )
    );
  }
}
