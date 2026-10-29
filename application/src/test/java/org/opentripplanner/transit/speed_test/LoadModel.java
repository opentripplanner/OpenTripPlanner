package org.opentripplanner.transit.speed_test;

import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.transfer.regular.TransferRepository;
import org.opentripplanner.transit.dataimport.TimetableDataImport;
import org.opentripplanner.transit.service.TransitRepository;
import org.opentripplanner.transit.transfer.regular.RegularTransferBuildRepository;

record LoadModel(
  Graph graph,
  TransitRepository transitRepository,
  TimetableDataImport timetableDataImport,
  TransferRepository transferRepository,
  RegularTransferBuildRepository<NearbyStop> regularTransferBuildRepository,
  BuildConfig buildConfig
) {}
