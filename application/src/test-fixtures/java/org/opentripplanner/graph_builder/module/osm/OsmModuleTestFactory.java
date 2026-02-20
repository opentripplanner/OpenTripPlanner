package org.opentripplanner.graph_builder.module.osm;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import org.opentripplanner.osm.OsmProvider;
import org.opentripplanner.service.osminfo.internal.DefaultOsmInfoGraphBuildRepository;
import org.opentripplanner.service.streetdetails.internal.DefaultStreetDetailsRepository;
import org.opentripplanner.service.vehicleparking.internal.DefaultVehicleParkingRepository;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.internal.DefaultStreetRepository;

public class OsmModuleTestFactory {

  private Graph graph;
  private DefaultOsmInfoGraphBuildRepository osmInfoGraphBuildRepository;
  private DefaultStreetRepository streetRepository;
  private DefaultVehicleParkingRepository vehicleParkingRepository;
  private DefaultStreetDetailsRepository streetDetailsRepository;
  private List<OsmProvider> providers;

  private OsmModuleTestFactory(Collection<OsmProvider> providers) {
    this.providers = List.copyOf(providers);
  }

  public static OsmModuleTestFactory of(OsmProvider provider) {
    return of(List.of(provider));
  }

  public static OsmModuleTestFactory of(Collection<OsmProvider> providers) {
    return new OsmModuleTestFactory(providers);
  }

  public OsmModuleTestFactory withGraph(Graph graph) {
    this.graph = graph;
    return this;
  }

  public OsmModuleTestFactory withOsmInfoGraphBuildRepository(
    DefaultOsmInfoGraphBuildRepository repository
  ) {
    this.osmInfoGraphBuildRepository = repository;
    return this;
  }

  public OsmModuleTestFactory withStreetRepository(DefaultStreetRepository streetRepository) {
    this.streetRepository = streetRepository;
    return this;
  }

  public OsmModuleTestFactory withVehicleParkingRepository(
    DefaultVehicleParkingRepository vehicleParkingRepository
  ) {
    this.vehicleParkingRepository = vehicleParkingRepository;
    return this;
  }

  public OsmModuleTestFactory withStreetDetailsRepository(
    DefaultStreetDetailsRepository streetDetailsRepository
  ) {
    this.streetDetailsRepository = streetDetailsRepository;
    return this;
  }

  public OsmModuleBuilder builder() {
    return OsmModule.of(
      providers,
      getOrElse(graph, Graph::new),
      getOrElse(osmInfoGraphBuildRepository, DefaultOsmInfoGraphBuildRepository::new),
      getOrElse(streetDetailsRepository, DefaultStreetDetailsRepository::new),
      getOrElse(streetRepository, DefaultStreetRepository::new),
      getOrElse(vehicleParkingRepository, DefaultVehicleParkingRepository::new)
    );
  }

  private static <T> T getOrElse(T instance, Supplier<T> factory) {
    return instance == null ? factory.get() : instance;
  }
}
