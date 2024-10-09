package org.opentripplanner.netex.loader.parser;

import jakarta.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.Collection;
import org.opentripplanner.netex.index.NetexEntityIndex;
import org.rutebanken.netex.model.Authority;
import org.rutebanken.netex.model.Branding;
import org.rutebanken.netex.model.Operator;
import org.rutebanken.netex.model.Organisation_VersionStructure;
import org.rutebanken.netex.model.OrganisationsInFrame_RelStructure;
import org.rutebanken.netex.model.ResourceFrame_VersionFrameStructure;
import org.rutebanken.netex.model.TypesOfValueInFrame_RelStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ResourceFrameParser extends NetexParser<ResourceFrame_VersionFrameStructure> {

  private static final Logger LOG = LoggerFactory.getLogger(ResourceFrameParser.class);

  private final Collection<Authority> authorities = new ArrayList<>();
  private final Collection<Operator> operators = new ArrayList<>();
  private final Collection<Branding> brandings = new ArrayList<>();

  @Override
  void parse(ResourceFrame_VersionFrameStructure frame) {
    parseOrganization(frame.getOrganisations());
    parseTypesOfValue(frame.getTypesOfValue());

    // Keep list sorted alphabetically
    warnOnMissingMapping(LOG, frame.getBlacklists());
    warnOnMissingMapping(LOG, frame.getControlCentres());
    warnOnMissingMapping(LOG, frame.getDataSources());
    warnOnMissingMapping(LOG, frame.getEquipments());
    warnOnMissingMapping(LOG, frame.getGroupsOfEntities());
    warnOnMissingMapping(LOG, frame.getGroupsOfOperators());
    warnOnMissingMapping(LOG, frame.getOperationalContexts());
    warnOnMissingMapping(LOG, frame.getResponsibilitySets());
    warnOnMissingMapping(LOG, frame.getSchematicMaps());
    warnOnMissingMapping(LOG, frame.getVehicles());
    warnOnMissingMapping(LOG, frame.getVehicleEquipmentProfiles());
    warnOnMissingMapping(LOG, frame.getVehicleModels());
    warnOnMissingMapping(LOG, frame.getVehicleTypes());
    warnOnMissingMapping(LOG, frame.getWhitelists());
    warnOnMissingMapping(LOG, frame.getZones());

    verifyCommonUnusedPropertiesIsNotSet(LOG, frame);
  }

  @Override
  void setResultOnIndex(NetexEntityIndex netexIndex) {
    netexIndex.authoritiesById.addAll(authorities);
    netexIndex.operatorsById.addAll(operators);
    netexIndex.brandingById.addAll(brandings);
  }

  /* private methods */

  private void parseOrganization(OrganisationsInFrame_RelStructure elements) {
    if (elements != null) {
      for (JAXBElement<?> e : elements.getOrganisation_()) {
        parseOrganization((Organisation_VersionStructure) e.getValue());
      }
    }
  }

  private void parseOrganization(Organisation_VersionStructure element) {
    if (element instanceof Authority authority) {
      authorities.add(authority);
    } else if (element instanceof Operator operator) {
      operators.add(operator);
    } else {
      warnOnMissingMapping(LOG, element);
    }
  }

  private void parseTypesOfValue(TypesOfValueInFrame_RelStructure element) {
    if (element == null || element.getValueSetOrTypeOfValue() == null) {
      return;
    }

    for (JAXBElement<?> e : element.getValueSetOrTypeOfValue()) {
      if (e.getValue() instanceof Branding) {
        parseBranding((Branding) e.getValue());
      } else {
        warnOnMissingMapping(LOG, e);
      }
    }
  }

  private void parseBranding(Branding branding) {
    brandings.add(branding);
  }
}
