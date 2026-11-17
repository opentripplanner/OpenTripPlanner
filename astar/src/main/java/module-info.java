/** Generic A* shortest-path engine. Independent of the street model. */
module org.opentripplanner.astar {
  requires org.opentripplanner.utils;
  requires org.slf4j;
  requires static org.jspecify;

  exports org.opentripplanner.astar;
  exports org.opentripplanner.astar.model;
  exports org.opentripplanner.astar.spi;
  exports org.opentripplanner.astar.strategy;
}
