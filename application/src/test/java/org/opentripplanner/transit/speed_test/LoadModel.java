package org.opentripplanner.transit.speed_test;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.transit.service.TimetableRepository;

record LoadModel(Graph graph, TimetableRepository timetableRepository, BuildConfig buildConfig) {}
