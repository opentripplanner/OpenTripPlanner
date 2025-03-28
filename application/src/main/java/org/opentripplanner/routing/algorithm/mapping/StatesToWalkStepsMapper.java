package org.opentripplanner.routing.algorithm.mapping;

import static org.opentripplanner.model.plan.RelativeDirection.ENTER_STATION;
import static org.opentripplanner.model.plan.RelativeDirection.EXIT_STATION;
import static org.opentripplanner.model.plan.RelativeDirection.FOLLOW_SIGNS;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.framework.geometry.DirectionUtils;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.model.plan.ElevationProfile;
import org.opentripplanner.model.plan.RelativeDirection;
import org.opentripplanner.model.plan.WalkStep;
import org.opentripplanner.model.plan.WalkStepBuilder;
import org.opentripplanner.routing.services.notes.StreetNotesService;
import org.opentripplanner.street.model.edge.AreaEdge;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.ElevatorAlightEdge;
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

  private final List<State> states;
  private final WalkStep previous;
  private final List<WalkStepBuilder> steps = new ArrayList<>();

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
    double ellipsoidToGeoidDifference
  ) {
    this.states = states;
    this.previous = previousStep;
    this.streetNotesService = streetNotesService;
    this.ellipsoidToGeoidDifference = ellipsoidToGeoidDifference;
  }

  public static String getNormalizedName(String streetName) {
    if (streetName == null) {
      return null; //Avoid null reference exceptions with pathways which don't have names
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
      createAndSaveStep(backState, forwardState, link.getName(), direction, edge, link.entrance());
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
    if (edge instanceof ElevatorAlightEdge) {
      addStep(createElevatorWalkStep(backState, forwardState, edge));
      return;
    } else if (backState.getVertex() instanceof StationEntranceVertex stationEntranceVertex) {
      addStep(createStationEntranceWalkStep(backState, forwardState, stationEntranceVertex));
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
        boolean isOnSameStreet = lastStep
          .directionTextNoParens()
          .equals(threeBack.directionTextNoParens());
        if (twoBack.distance() < MAX_ZAG_DISTANCE && isOnSameStreet && !twoBack.hasEntrance()) {
          if (isUTurn(twoBack, lastStep)) {
            steps.remove(lastIndex - 1);
            processUTurn(lastStep, twoBack);
          } else if (!lastStep.hasEntrance()) {
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
      backState.getPreferences().system().geoidElevation() ? -ellipsoidToGeoidDifference : 0
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

  private WalkStepBuilder createElevatorWalkStep(State backState, State forwardState, Edge edge) {
    // don't care what came before or comes after
    var step = createWalkStep(forwardState, backState);

    // tell the user where to get off the elevator using the exit notation, so the
    // i18n interface will say 'Elevator to <exit>'
    // what happens is that the webapp sees name == null and ignores that, and it sees
    // exit != null and uses to <exit>
    // the floor name is the AlightEdge name
    // reset to avoid confusion with 'Elevator on floor 1 to floor 1'
    step.withDirectionText(edge.getName());

    step.withRelativeDirection(RelativeDirection.ELEVATOR);

    return step;
  }

  private WalkStepBuilder createStationEntranceWalkStep(
    State backState,
    State forwardState,
    StationEntranceVertex vertex
  ) {
    Entrance entrance = Entrance.of(vertex.id())
      .withCode(vertex.code())
      .withCoordinate(new WgsCoordinate(vertex.getCoordinate()))
      .withWheelchairAccessibility(vertex.wheelchairAccessibility())
      .build();

    // don't care what came before or comes after
    return createWalkStep(forwardState, backState)
      // There is not a way to definitively determine if a user is entering or exiting the station,
      // since the doors might be between or inside stations.
      .withRelativeDirection(RelativeDirection.ENTER_OR_EXIT_STATION)
      .withEntrance(entrance);
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
    Edge en = forwardState.getBackEdge();

    return WalkStep.builder()
      .withDirectionText(en.getName())
      .withStartLocation(new WgsCoordinate(backState.getVertex().getCoordinate()))
      .withNameIsDerived(en.nameIsDerived())
      .withAngle(DirectionUtils.getFirstAngle(forwardState.getBackEdge().getGeometry()))
      .withWalkingBike(forwardState.isBackWalkingBike())
      .withArea(forwardState.getBackEdge() instanceof AreaEdge)
      .addElevation(
        encodeElevationProfile(
          forwardState.getBackEdge(),
          0,
          forwardState.getPreferences().system().geoidElevation() ? -ellipsoidToGeoidDifference : 0
        )
      )
      .addStreetNotes(streetNotesService.getNotes(forwardState));
  }
}
