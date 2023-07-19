package org.opentripplanner.graph_builder.model;

/** Modules that add elements to a graph. These are plugins to the GraphBuilder. */
public interface GraphBuilderModule {
  /**
   * Process whatever inputs were supplied to this module and update the model objects(graph,
   * transitModel and issueStore).
   */
  void buildGraph();

  /** Check that all inputs to the graphbuilder are valid; throw an exception if not. */
  default void checkInputs() {
    // the vast majority of modules don't have any checks
  }
}
