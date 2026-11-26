package org.opentripplanner.netex.loader.parser;

import jakarta.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.entur.netex2.model.CarPoolingService;
import org.entur.netex2.model.MobilityService_VersionFrameStructure;
import org.entur.netex2.model.MobilityServices_RelStructure;
import org.entur.netex2.model.QuayRefStructure;
import org.entur.netex2.model.VehicleMeetingPoint;
import org.entur.netex2.model.VehicleMeetingPointAssignment;
import org.entur.netex2.model.VehicleMeetingPointAssignmentsInFrame_RelStructure;
import org.entur.netex2.model.VehicleMeetingPointsInFrame_RelStructure;
import org.jspecify.annotations.Nullable;
import org.opentripplanner.netex.index.NetexEntityIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class MobilityServiceFrameParser extends NetexParser<MobilityService_VersionFrameStructure> {

  private static final Logger LOG = LoggerFactory.getLogger(MobilityServiceFrameParser.class);

  private final Collection<CarPoolingService> carPoolingServices = new ArrayList<>();
  private final Collection<VehicleMeetingPoint> vehicleMeetingPoints = new ArrayList<>();
  private final Map<String, String> quayIdByVehicleMeetingPointRef = new HashMap<>();

  @Override
  void parse(MobilityService_VersionFrameStructure frame) {
    parseMobilityServices(frame.getMobilityServices());
    parseVehicleMeetingPoints(frame.getVehicleMeetingPoints());
    parseVehicleMeetingPointAssignments(frame.getVehicleMeetingPointAssignments());

    // The type of service tells that it is car pooling, the mode of operation is not needed
    informOnElementIntentionallySkipped(LOG, frame.getModesOfOperation());

    // Keep list sorted alphabetically
    warnOnMissingMapping(LOG, frame.getFleets());
    warnOnMissingMapping(LOG, frame.getMobilityServiceConstraintZones());
    warnOnMissingMapping(LOG, frame.getOnlineServices());
    warnOnMissingMapping(LOG, frame.getPoolsOfVehicles());
    warnOnMissingMapping(LOG, frame.getVehicleMeetingLinks());
    warnOnMissingMapping(LOG, frame.getVehicleMeetingPlaceAssignments());
    warnOnMissingMapping(LOG, frame.getVehicleMeetingPlaces());

    verifyCommonUnusedPropertiesIsNotSet(LOG, frame);
  }

  @Override
  void setResultOnIndex(NetexEntityIndex index) {
    index.carPoolingServiceById.addAll(carPoolingServices);
    index.vehicleMeetingPointById.addAll(vehicleMeetingPoints);
    index.quayIdByVehicleMeetingPointRef.addAll(quayIdByVehicleMeetingPointRef);
  }

  private void parseMobilityServices(@Nullable MobilityServices_RelStructure services) {
    if (services == null) {
      return;
    }

    for (JAXBElement<?> service : services.getMobilityService()) {
      if (service.getValue() instanceof CarPoolingService carPoolingService) {
        carPoolingServices.add(carPoolingService);
      } else {
        warnOnMissingMapping(LOG, service.getValue());
      }
    }
  }

  private void parseVehicleMeetingPoints(
    @Nullable VehicleMeetingPointsInFrame_RelStructure points
  ) {
    if (points == null) {
      return;
    }

    vehicleMeetingPoints.addAll(points.getVehicleMeetingPoint());
  }

  private void parseVehicleMeetingPointAssignments(
    @Nullable VehicleMeetingPointAssignmentsInFrame_RelStructure assignments
  ) {
    if (assignments == null) {
      return;
    }

    for (VehicleMeetingPointAssignment assignment : assignments.getVehicleMeetingPointAssignment()) {
      var placeRef = assignment.getAddressablePlaceRef();

      if (placeRef != null && placeRef.getValue() instanceof QuayRefStructure quayRef) {
        String meetingPointRef = assignment.getVehicleMeetingPointRef().getRef();
        quayIdByVehicleMeetingPointRef.put(meetingPointRef, quayRef.getRef());
      } else {
        warnOnMissingMapping(LOG, placeRef);
      }
    }
  }
}
