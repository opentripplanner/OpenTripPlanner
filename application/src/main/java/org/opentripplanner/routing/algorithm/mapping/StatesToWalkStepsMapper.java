package org.opentripplanner.routing.algorithm.mapping;

import static org.opentripplanner.model.plan.walkstep.RelativeDirection.ENTER_STATION;
import static org.opentripplanner.model.plan.walkstep.RelativeDirection.EXIT_STATION;
import static org.opentripplanner.model.plan.walkstep.RelativeDirection.FOLLOW_SIGNS;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.model.plan.leg.ElevationProfile;
import org.opentripplanner.model.plan.walkstep.RelativeDirection;
import org.opentripplanner.model.plan.walkstep.WalkStep;
import org.opentripplanner.model.plan.walkstep.WalkStepBuilder;
import org.opentripplanner.model.plan.walkstep.verticaltransportation.ElevatorUse;
import org.opentripplanner.model.plan.walkstep.verticaltransportation.EscalatorUse;
import org.opentripplanner.model.plan.walkstep.verticaltransportation.StairsUse;
import org.opentripplanner.model.plan.walkstep.verticaltransportation.VerticalTransportationUseFactory;
import org.opentripplanner.routing.graphfinder.EntranceResolver;
import org.opentripplanner.service.streetdetails.StreetDetailsService;
import org.opentripplanner.street.geometry.DirectionUtils;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.internal.notes.StreetNotesService;
import org.opentripplanner.street.model.edge.AreaEdge;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.ElevatorAlightEdge;
import org.opentripplanner.street.model.edge.EscalatorEdge;
import org.opentripplanner.street.model.edge.FreeEdge;
import org.opentripplanner.street.model.edge.PathwayEdge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.StreetTransitEntranceLink;
import org.opentripplanner.street.model.vertex.ExitVertex;
import org.opentripplanner.street.model.vertex.StationEntranceVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.transit.model.site.Entrance;

/**
 * Process a list of states into a list of walking/driving instructions for a street leg.
 */
public class StatesToWalkStepsMapper {

  /**
   * Tolerance for how many meters can be between two consecutive turns will be merged into a singe
   * walk step. See {@link StatesToWalkStepsMapper#removeZag(WalkStepBuilder, WalkStepBuilder)}
   */
  private static final double MAX_ZAG_DISTANCE = 30;

  private final double ellipsoidToGeoidDifference;
  private final StreetNotesService streetNotesService;
  private final VerticalTransportationUseFactory verticalTransportationUseFactory;

  private final List<State> states;
  private final WalkStep previous;
  private final List<WalkStepBuilder> steps = new ArrayList<>();
  private final EntranceResolver entranceResolver;

  private WalkStepBuilder current = null;
  private double lastAngle = 0;

  /**
   * Distance used for appending elevation profiles
   */
  private double distance = 0;

  /**
   * Track whether we are in a roundabout, and if so the exit number
   */
  private int roundaboutExit = 0;
  private String roundaboutPreviousStreet = null;

  /**
   * Converts a list of street edges to a list of turn-by-turn directions.
   *
   * @param previousStep the last walking step of a non-transit leg that immediately precedes this
   *                     one or null, if first leg
   */
  public StatesToWalkStepsMapper(
    List<State> states,
    WalkStep previousStep,
    StreetNotesService streetNotesService,
    StreetDetailsService streetDetailsService,
    EntranceResolver entranceResolver,
    double ellipsoidToGeoidDifference
  ) {
    this.states = states;
    this.previous = previousStep;
    this.streetNotesService = streetNotesService;
    this.entranceResolver = entranceResolver;
    this.ellipsoidToGeoidDifference = ellipsoidToGeoidDifference;
    this.verticalTransportationUseFactory = new VerticalTransportationUseFactory(
      streetDetailsService
    );
  }

  public static String getNormalizedName(String streetName) {
    if (streetName == null) {
      // Avoid null reference exceptions with pathways which don't have names
      return null;
    }
    int idx = streetName.indexOf('(');
    if (idx > 0) {
      return streetName.substring(0, idx - 1);
    }
    return streetName;
  }

  public List<WalkStep> generateWalkSteps() {
    for (int i = 0; i < states.size() - 1; i++) {
      processState(states.get(i), states.get(i + 1));
    }

    return steps.stream().map(WalkStepBuilder::build).toList();
  }

  /**
   * Have we done a U-Turn with the previous two states
   */
  private static boolean isUTurn(WalkStepBuilder twoBack, WalkStepBuilder lastStep) {
    RelativeDirection d1 = lastStep.relativeDirection();
    RelativeDirection d2 = twoBack.relativeDirection();
    return (
      ((d1 == RelativeDirection.RIGHT || d1 == RelativeDirection.HARD_RIGHT) &&
        (d2 == RelativeDirection.RIGHT || d2 == RelativeDirection.HARD_RIGHT)) ||
      ((d1 == RelativeDirection.LEFT || d1 == RelativeDirection.HARD_LEFT) &&
        (d2 == RelativeDirection.LEFT || d2 == RelativeDirection.HARD_LEFT))
    );
  }

  private static double getAbsoluteAngleDiff(double thisAngle, double lastAngle) {
    double angleDiff = thisAngle - lastAngle;
    if (angleDiff < 0) {
      angleDiff += Math.PI * 2;
    }
    double ccwAngleDiff = Math.PI * 2 - angleDiff;
    if (ccwAngleDiff < angleDiff) {
      angleDiff = ccwAngleDiff;
    }
    return angleDiff;
  }

  private static boolean isLink(Edge edge) {
    return (edge instanceof StreetEdge streetEdge && streetEdge.isLink());
  }

  private static ElevationProfile encodeElevationProfile(
    Edge edge,
    double distanceOffset,
    double heightOffset
  ) {
    if (!(edge instanceof StreetEdge elevEdge)) {
      return ElevationProfile.empty();
    }
    if (elevEdge.getElevationProfile() == null) {
      return ElevationProfile.empty();
    }
    var out = ElevationProfile.of();
    for (Coordinate coordinate : elevEdge.getElevationProfile().toCoordinateArray()) {
      out.step(coordinate.x + distanceOffset, coordinate.y + heightOffset);
    }
    return out.build();
  }

  private void processState(State backState, State forwardState) {
    Edge edge = forwardState.getBackEdge();

    boolean createdNewStep = false;
    if (edge instanceof FreeEdge) {
      return;
    } else if (edge instanceof StreetTransitEntranceLink link) {
      var direction = relativeDirectionForTransitLink(link);
      var entrance = entranceResolver.getEntrance(link.entrance());
      createAndSaveStep(backState, forwardState, link.getName(), direction, edge, entrance);
      return;
    }

    if (forwardState.getBackMode() == null) {
      return;
    }
    Geometry geom = edge.getGeometry();
    if (geom == null) {
      return;
    }

    // generate a step for getting off an elevator (all elevator narrative generation occurs
    // when alighting). We don't need to know what came before or will come after
    if (edge instanceof ElevatorAlightEdge elevatorAlightEdge) {
      createAndSaveElevatorWalkStep(backState, forwardState, elevatorAlightEdge);
      return;
    } else if (edge instanceof EscalatorEdge) {
      createAndSaveEscalatorWalkStep(backState, forwardState, edge, geom);
      return;
    } else if (edge instanceof StreetEdge streetEdge && streetEdge.isStairs()) {
      createAndSaveStairsWalkStep(backState, forwardState, edge, geom);
      return;
    } else if (backState.getVertex() instanceof StationEntranceVertex stationEntranceVertex) {
      createAndSaveStationEntranceWalkStep(backState, forwardState, stationEntranceVertex);
      return;
    } else if (edge instanceof PathwayEdge pwe && pwe.signpostedAs().isPresent()) {
      createAndSaveStep(
        backState,
        forwardState,
        pwe.signpostedAs().get(),
        FOLLOW_SIGNS,
        edge,
        null
      );
      return;
    }

    String streetName = edge.getName().toString();
    String streetNameNoParens = getNormalizedName(streetName);

    boolean modeTransition = forwardState.getBackMode() != backState.getBackMode();

    if (current == null) {
      createFirstStep(backState, forwardState);
      createdNewStep = true;
    } else if (
      modeTransition ||
      !continueOnSameStreet(edge, streetNameNoParens) ||
      // went on to or off of a roundabout
      edge.isRoundabout() !=
      (roundaboutExit > 0) ||
      (isLink(edge) && !isLink(backState.getBackEdge()))
    ) {
      // Street name has changed, or we've gone on to or off of a roundabout.

      // if we were just on a roundabout, make note of which exit was taken in the existing step
      if (roundaboutExit > 0) {
        // ordinal numbers from
        current.withExit(Integer.toString(roundaboutExit));
        if (streetNameNoParens.equals(roundaboutPreviousStreet)) {
          current.withStayOn(true);
        }
        roundaboutExit = 0;
      }

      // start a new step
      current = createWalkStep(forwardState, backState);
      createdNewStep = true;
      steps.add(current);

      // indicate that we are now on a roundabout and use one-based exit numbering
      if (edge.isRoundabout()) {
        roundaboutExit = 1;
        roundaboutPreviousStreet = getNormalizedName(backState.getBackEdge().getName().toString());
      }

      double thisAngle = DirectionUtils.getFirstAngle(geom);
      current.withDirections(lastAngle, thisAngle, edge.isRoundabout());
      // new step, set distance to length of first edge
      distance = edge.getDistanceMeters();
    } else {
      // street name has not changed
      double thisAngle = DirectionUtils.getFirstAngle(geom);
      RelativeDirection direction = RelativeDirection.calculate(
        lastAngle,
        thisAngle,
        edge.isRoundabout()
      );
      if (edge.isRoundabout()) {
        // we are on a roundabout, and have already traversed at least one edge of it.
        if (multipleTurnOptionsInPreviousState(backState)) {
          // increment exit count if we passed one.
          roundaboutExit += 1;
        }
      } else if (direction != RelativeDirection.CONTINUE) {
        // we are not on a roundabout, and not continuing straight through.
        // figure out if there were other plausible turn options at the last intersection
        // to see if we should generate a "left to continue" instruction.
        if (isPossibleToTurnToOtherStreet(backState, edge, streetName, thisAngle)) {
          // turn to stay on same-named street
          current = createWalkStep(forwardState, backState);
          createdNewStep = true;
          current.withDirections(lastAngle, thisAngle, false);
          current.withStayOn(true);
          steps.add(current);
          // new step, set distance to length of first edge
          distance = edge.getDistanceMeters();
        }
      }
    }

    setMotorwayExit(backState);

    if (createdNewStep && !modeTransition) {
      // check last three steps for zag
      int lastIndex = steps.size() - 1;
      if (lastIndex >= 2) {
        WalkStepBuilder threeBack = steps.get(lastIndex - 2);
        WalkStepBuilder twoBack = steps.get(lastIndex - 1);
        WalkStepBuilder lastStep = steps.get(lastIndex);
        boolean isOnSameStreet = isOnSameStreet(lastStep, twoBack, threeBack);
        if (twoBack.distance() < MAX_ZAG_DISTANCE && isOnSameStreet && canZagBeRemoved(twoBack)) {
          if (isUTurn(twoBack, lastStep)) {
            steps.remove(lastIndex - 1);
            processUTurn(lastStep, twoBack);
          } else if (canZagBeRemoved(lastStep)) {
            // total hack to remove zags.
            steps.remove(lastIndex);
            steps.remove(lastIndex - 1);
            removeZag(threeBack, twoBack);
          }
        }
      }
    } else {
      if (!createdNewStep && current.elevationProfile() != null) {
        updateElevationProfile(backState, edge);
      }
      distance += edge.getDistanceMeters();
    }

    // increment the total length for this step
    current
      .addDistance(edge.getDistanceMeters())
      .addStreetNotes(streetNotesService.getNotes(forwardState));
    lastAngle = DirectionUtils.getLastAngle(geom);

    current.addEdge(edge);
  }

  /**
   * Determines whether a set of three consecutive instances of {@link WalkStepBuilder} refer to the same street.
   * The purposes of this check are (i) to give a separate instruction when crossing to the other side of the same street, if a crosswalk namer is iin use
   * (an instruction can be given to cross at a particular location because others may not be accessible, practical, etc.),
   * and (ii) to remove trivial turns when a given street briefly merges with another.
   * @return true if the walk steps refer to the same street, false otherwise.
   */
  public static boolean isOnSameStreet(
    WalkStepBuilder lastStep,
    WalkStepBuilder twoBack,
    WalkStepBuilder threeBack
  ) {
    String lastStepName = lastStep.directionTextNoParens();
    String twoBackStepName = twoBack.directionTextNoParens();
    String threeBackStepName = threeBack.directionTextNoParens();
    if (lastStepName == null || twoBackStepName == null || threeBackStepName == null) {
      return false;
    }

    return (
      (!lastStep.isCrossing() || lastStep.nameIsDerived()) &&
      (!twoBack.isCrossing() || twoBack.nameIsDerived()) &&
      lastStepName.equals(threeBackStepName)
    );
  }

  private static RelativeDirection relativeDirectionForTransitLink(StreetTransitEntranceLink link) {
    if (link.isExit()) {
      return EXIT_STATION;
    } else {
      return ENTER_STATION;
    }
  }

  private WalkStepBuilder addStep(WalkStepBuilder step) {
    current = step;
    steps.add(current);
    return step;
  }

  private void updateElevationProfile(State backState, Edge edge) {
    ElevationProfile p = encodeElevationProfile(
      edge,
      distance,
      backState.getRequest().geoidElevation() ? -ellipsoidToGeoidDifference : 0
    );
    current.addElevation(p);
  }

  /**
   * Merge two consecutive turns will be into a singe walk step.
   * <pre>
   *      | a
   *      |
   *  ____/
   * / ^ this is a zag between walk steps a and b. If it is less than 30 meters, a and b will be
   * |   in the same walk step.
   * | b
   * </pre>
   */
  private void removeZag(WalkStepBuilder threeBack, WalkStepBuilder twoBack) {
    current = threeBack;
    current.addDistance(twoBack.distance());
    distance += current.distance();
    if (twoBack.elevationProfile() != null) {
      if (current.elevationProfile() == null) {
        current.addElevation(twoBack.elevationProfile());
      } else {
        current.addElevation(twoBack.elevationProfile().transformX(current.distance()));
      }
    }
  }

  private boolean canZagBeRemoved(WalkStepBuilder walkStepBuilder) {
    return (
      !walkStepBuilder.hasEntrance() &&
      !(walkStepBuilder.verticalTransportationUse() instanceof ElevatorUse) &&
      !(walkStepBuilder.verticalTransportationUse() instanceof EscalatorUse) &&
      !(walkStepBuilder.verticalTransportationUse() instanceof StairsUse)
    );
  }

  private void processUTurn(WalkStepBuilder lastStep, WalkStepBuilder twoBack) {
    // in this case, we have two left turns or two right turns in quick
    // succession; this is probably a U-turn.

    lastStep.addDistance(twoBack.distance());

    // A U-turn to the left, typical in the US.
    if (
      lastStep.relativeDirection() == RelativeDirection.LEFT ||
      lastStep.relativeDirection() == RelativeDirection.HARD_LEFT
    ) {
      lastStep.withRelativeDirection(RelativeDirection.UTURN_LEFT);
    } else {
      lastStep.withRelativeDirection(RelativeDirection.UTURN_RIGHT);
    }

    // in this case, we're definitely staying on the same street
    // (since it's zag removal, the street names are the same)
    lastStep.withStayOn(true);
  }

  /**
   * Update the walk step with the name of the motorway junction if set from OSM
   */
  private void setMotorwayExit(State backState) {
    State exitState = backState;
    Edge exitEdge = exitState.getBackEdge();
    while (exitEdge instanceof FreeEdge) {
      exitState = exitState.getBackState();
      exitEdge = exitState.getBackEdge();
    }
    if (exitState.getVertex() instanceof ExitVertex) {
      current.withExit(((ExitVertex) exitState.getVertex()).getExitName());
    }
  }

  /**
   * Is it possible to turn to another street from this previous state
   */
  private boolean isPossibleToTurnToOtherStreet(
    State backState,
    Edge edge,
    String streetName,
    double thisAngle
  ) {
    if (edge instanceof StreetEdge) {
      // the next edges will be PlainStreetEdges, we hope
      double angleDiff = getAbsoluteAngleDiff(thisAngle, lastAngle);
      for (StreetEdge alternative : backState.getVertex().getOutgoingStreetEdges()) {
        if (isTurnToOtherStreet(streetName, angleDiff, alternative)) {
          return true;
        }
      }
    } else {
      double angleDiff = getAbsoluteAngleDiff(lastAngle, thisAngle);
      // FIXME: this code might be wrong with the removal of the edge-based graph
      State twoStatesBack = backState.getBackState();
      Vertex backVertex = twoStatesBack.getVertex();
      for (StreetEdge alternative : backVertex.getOutgoingStreetEdges()) {
        for (StreetEdge innerAlternative : alternative.getToVertex().getOutgoingStreetEdges()) {
          if (isTurnToOtherStreet(streetName, angleDiff, innerAlternative)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * Is it possible to turn to another street from this alternative edge
   */
  private boolean isTurnToOtherStreet(String streetName, double angleDiff, Edge alternative) {
    if (alternative.getName().toString().equals(streetName)) {
      // alternatives that have the same name
      // are usually caused by street splits
      return false;
    }

    double altAngle = DirectionUtils.getFirstAngle(alternative.getGeometry());
    double altAngleDiff = getAbsoluteAngleDiff(altAngle, lastAngle);
    return angleDiff > Math.PI / 4 || altAngleDiff - angleDiff < Math.PI / 16;
  }

  private boolean continueOnSameStreet(Edge edge, String streetNameNoParens) {
    return !(
      current.directionText().toString() != null &&
      !(java.util.Objects.equals(current.directionTextNoParens(), streetNameNoParens)) &&
      (!current.nameIsDerived() || !edge.nameIsDerived())
    );
  }

  private static boolean multipleTurnOptionsInPreviousState(State state) {
    boolean foundAlternatePaths = false;
    TraverseMode requestedMode = state.currentMode();
    for (Edge out : state.getBackState().getVertex().getOutgoing()) {
      if (out == state.backEdge) {
        continue;
      }
      if (!(out instanceof StreetEdge)) {
        continue;
      }
      var outStates = out.traverse(state.getBackState());
      if (State.isEmpty(outStates)) {
        continue;
      }
      var outState = outStates[0];
      if (!outState.getBackMode().equals(requestedMode)) {
        //walking a bike, so, not really an exit
        continue;
      }
      // this section handles the case of an option which is only an option if you walk your
      // bike. It is complicated because you will not need to walk your bike until one
      // edge after the current edge.

      //now, from here, try a continuing path.
      Vertex tov = outState.getVertex();
      boolean found = false;
      for (Edge out2 : tov.getOutgoing()) {
        var outStates2 = out2.traverse(outState);
        if (
          !State.isEmpty(outStates2) && !Objects.equals(outStates2[0].getBackMode(), requestedMode)
        ) {
          // walking a bike, so, not really an exit
          continue;
        }
        found = true;
        break;
      }
      if (!found) {
        continue;
      }

      // there were paths we didn't take.
      foundAlternatePaths = true;
      break;
    }
    return foundAlternatePaths;
  }

  private void createFirstStep(State backState, State forwardState) {
    current = createWalkStep(forwardState, backState);

    Edge edge = forwardState.getBackEdge();
    double thisAngle = DirectionUtils.getFirstAngle(edge.getGeometry());
    if (previous == null) {
      current.withAbsoluteDirection(thisAngle);
      current.withRelativeDirection(RelativeDirection.DEPART);
    } else {
      current.withDirections(previous.getAngle(), thisAngle, false);
    }
    // new step, set distance to length of first edge
    distance = edge.getDistanceMeters();
    steps.add(current);
  }

  private void createAndSaveElevatorWalkStep(
    State backState,
    State forwardState,
    ElevatorAlightEdge elevatorAlightEdge
  ) {
    // don't care what came before or comes after
    addStep(
      createWalkStep(forwardState, backState)
        .withRelativeDirection(RelativeDirection.ELEVATOR)
        .withVerticalTransportationUse(
          verticalTransportationUseFactory.createElevatorUse(backState, elevatorAlightEdge)
        )
    );
  }

  private void createAndSaveStairsWalkStep(
    State backState,
    State forwardState,
    Edge edge,
    Geometry geom
  ) {
    addStep(
      createWalkStep(forwardState, backState)
        .withRelativeDirection(RelativeDirection.CONTINUE)
        .withAbsoluteDirection(DirectionUtils.getFirstAngle(geom))
        .addDistance(edge.getDistanceMeters())
        .withVerticalTransportationUse(verticalTransportationUseFactory.createStairsUse(edge))
    );

    lastAngle = DirectionUtils.getLastAngle(geom);
    distance = edge.getDistanceMeters();
    current.addEdge(edge);
  }

  private void createAndSaveEscalatorWalkStep(
    State backState,
    State forwardState,
    Edge edge,
    Geometry geom
  ) {
    addStep(
      createWalkStep(forwardState, backState)
        .withRelativeDirection(RelativeDirection.CONTINUE)
        .withAbsoluteDirection(DirectionUtils.getFirstAngle(geom))
        .addDistance(edge.getDistanceMeters())
        .withVerticalTransportationUse(verticalTransportationUseFactory.createEscalatorUse(edge))
    );

    lastAngle = DirectionUtils.getLastAngle(geom);
    distance = edge.getDistanceMeters();
    current.addEdge(edge);
  }

  private void createAndSaveStationEntranceWalkStep(
    State backState,
    State forwardState,
    StationEntranceVertex vertex
  ) {
    // don't care what came before or comes after
    addStep(
      createWalkStep(forwardState, backState)
        // There is not a way to definitively determine if a user is entering or exiting the
        // station, since the doors might be between or inside stations.
        .withRelativeDirection(RelativeDirection.ENTER_OR_EXIT_STATION)
        .withEntrance(getEntrance(vertex))
    );
  }

  private Entrance getEntrance(StationEntranceVertex vertex) {
    return Entrance.of(vertex.id())
      .withCode(vertex.code())
      .withCoordinate(new WgsCoordinate(vertex.getCoordinate()))
      .withWheelchairAccessibility(vertex.wheelchairAccessibility())
      .build();
  }

  private void createAndSaveStep(
    State backState,
    State forwardState,
    I18NString name,
    RelativeDirection direction,
    Edge edge,
    @Nullable Entrance entrance
  ) {
    addStep(
      createWalkStep(forwardState, backState)
        .withDirectionText(name)
        .withNameIsDerived(false)
        .withDirections(lastAngle, DirectionUtils.getFirstAngle(edge.getGeometry()), false)
        .withRelativeDirection(direction)
        .withEntrance(entrance)
        .addDistance(edge.getDistanceMeters())
    );

    lastAngle = DirectionUtils.getLastAngle(edge.getGeometry());
    distance = edge.getDistanceMeters();
    current.addEdge(edge);
  }

  private WalkStepBuilder createWalkStep(State forwardState, State backState) {
    Edge backEdge = forwardState.getBackEdge();

    return WalkStep.builder()
      .withDirectionText(backEdge.getName())
      .withStartLocation(new WgsCoordinate(backState.getVertex().getCoordinate()))
      .withNameIsDerived(backEdge.nameIsDerived())
      .withAngle(DirectionUtils.getFirstAngle(backEdge.getGeometry()))
      .withWalkingBike(forwardState.isBackWalkingBike())
      .withArea(backEdge instanceof AreaEdge)
      .withCrossing(backEdge.isCrossing())
      .addElevation(
        encodeElevationProfile(
          backEdge,
          0,
          forwardState.getRequest().geoidElevation() ? -ellipsoidToGeoidDifference : 0
        )
      )
      .addStreetNotes(streetNotesService.getNotes(forwardState));
  }
}
