package org.opentripplanner.transit.speed_test;

import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.transfer.regular.TransferRepository;
import org.opentripplanner.transit.service.TimetableRepository;

record LoadModel(
  Graph graph,
  TimetableRepository timetableRepository,
  TransferRepository transferRepository,
  BuildConfig buildConfig
) {}
