package org.opentripplanner.api.parameter;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.routing.api.request.RequestModesBuilder;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.transit.model.basic.TransitMode;

/**
 * A set of qualified modes. The original intent was to allow a sequence of mode sets, but the shift
 * to "long distance mode" routing means that it will make more sense to specify access, egress, and
 * transit modes in separate parameters. So now this only contains one mode set rather than a
 * sequence of them.
 * <p>
 * This class and QualifiedMode are clearly somewhat inefficient and allow nonsensical combinations
 * like renting and parking a subway. They are not intended for use in routing. Rather, they simply
 * parse the language of mode specifications that may be given in the mode query parameter. They are
 * then converted into more efficient and useful representation in the routing request.
 */
public class QualifiedModeSet implements Serializable {

  public Set<QualifiedMode> qModes = new HashSet<>();

  public QualifiedModeSet(String[] modes) {
    for (String qMode : modes) {
      qModes.add(new QualifiedMode(qMode));
    }
  }

  public QualifiedModeSet(String s) {
    this(s.split(","));
  }

  public List<TransitMode> getTransitModes() {
    return qModes.stream().flatMap(qMode -> qMode.mode.getTransitModes().stream()).toList();
  }

  public RequestModes getRequestModes() {
    RequestModesBuilder mBuilder = RequestModes.of();

    //  This is a best effort at mapping QualifiedModes to access/egress/direct StreetModes.
    //  It was unclear what exactly each combination of QualifiedModes should mean.
    //  TODO OTP2 This should either be updated with missing modes or the REST API should be
    //   redesigned to better reflect the mode structure used in RequestModes.
    //   Also, some StreetModes are implied by combination of QualifiedModes and are not covered
    //   in this mapping.
    QualifiedMode requestMode = null;

    List<QualifiedMode> filteredModes = qModes
      .stream()
      .filter(m ->
        m.mode == ApiRequestMode.WALK ||
        m.mode == ApiRequestMode.BICYCLE ||
        m.mode == ApiRequestMode.SCOOTER ||
        m.mode == ApiRequestMode.CAR
      )
      .toList();

    if (filteredModes.size() > 1) {
      List<QualifiedMode> filteredModesWithoutWalk = filteredModes
        .stream()
        .filter(Predicate.not(m -> m.mode == ApiRequestMode.WALK))
        .toList();

      if (filteredModesWithoutWalk.size() > 1) {
        throw new IllegalStateException(
          "Multiple non-walk modes provided " + filteredModesWithoutWalk
        );
      } else if (filteredModesWithoutWalk.isEmpty()) {
        requestMode = filteredModes.get(0);
      } else {
        requestMode = filteredModesWithoutWalk.get(0);
      }
    } else if (!filteredModes.isEmpty()) {
      requestMode = filteredModes.get(0);
    }

    if (requestMode != null) {
      switch (requestMode.mode) {
        case WALK -> mBuilder.withAllStreetModes(StreetMode.WALK);
        case BICYCLE -> {
          if (requestMode.qualifiers.contains(Qualifier.RENT)) {
            mBuilder.withAllStreetModes(StreetMode.BIKE_RENTAL);
          } else if (requestMode.qualifiers.contains(Qualifier.PARK)) {
            mBuilder.withAccessMode(StreetMode.BIKE_TO_PARK);
            mBuilder.withEgressMode(StreetMode.WALK);
            mBuilder.withDirectMode(StreetMode.BIKE_TO_PARK);
            mBuilder.withTransferMode(StreetMode.WALK);
          } else {
            mBuilder.withAllStreetModes(StreetMode.BIKE);
          }
        }
        case SCOOTER -> {
          if (requestMode.qualifiers.contains(Qualifier.RENT)) {
            mBuilder.withAllStreetModes(StreetMode.SCOOTER_RENTAL);
          } else {
            // Only supported as rental mode
            throw new IllegalArgumentException();
          }
        }
        case CAR -> {
          if (requestMode.qualifiers.contains(Qualifier.RENT)) {
            mBuilder.withAllStreetModes(StreetMode.CAR_RENTAL);
          } else if (requestMode.qualifiers.contains(Qualifier.PARK)) {
            mBuilder.withAccessMode(StreetMode.CAR_TO_PARK);
            mBuilder.withTransferMode(StreetMode.WALK);
            mBuilder.withEgressMode(StreetMode.WALK);
            mBuilder.withDirectMode(StreetMode.CAR_TO_PARK);
          } else if (requestMode.qualifiers.contains(Qualifier.PICKUP)) {
            mBuilder.withAccessMode(StreetMode.WALK);
            mBuilder.withTransferMode(StreetMode.WALK);
            mBuilder.withEgressMode(StreetMode.CAR_PICKUP);
            mBuilder.withDirectMode(StreetMode.CAR_PICKUP);
          } else if (requestMode.qualifiers.contains(Qualifier.DROPOFF)) {
            mBuilder.withAccessMode(StreetMode.CAR_PICKUP);
            mBuilder.withTransferMode(StreetMode.WALK);
            mBuilder.withEgressMode(StreetMode.WALK);
            mBuilder.withDirectMode(StreetMode.CAR_PICKUP);
          } else if (requestMode.qualifiers.contains(Qualifier.HAIL)) {
            mBuilder.withAccessMode(StreetMode.CAR_HAILING);
            mBuilder.withTransferMode(StreetMode.WALK);
            mBuilder.withEgressMode(StreetMode.CAR_HAILING);
            mBuilder.withDirectMode(StreetMode.WALK);
          } else {
            // Cars can use transit, for example, with car ferries.
            mBuilder.withAccessMode(StreetMode.CAR);
            mBuilder.withTransferMode(StreetMode.CAR);
            mBuilder.withEgressMode(StreetMode.CAR);
            mBuilder.withDirectMode(StreetMode.CAR);
          }
        }
      }
    }

    // These modes are set last in order to take precedence over other modes
    for (QualifiedMode qMode : qModes) {
      if (qMode.mode.equals(ApiRequestMode.FLEX)) {
        if (qMode.qualifiers.contains(Qualifier.ACCESS)) {
          mBuilder.withAccessMode(StreetMode.FLEXIBLE);
        } else if (qMode.qualifiers.contains(Qualifier.EGRESS)) {
          mBuilder.withEgressMode(StreetMode.FLEXIBLE);
        } else if (qMode.qualifiers.contains(Qualifier.DIRECT)) {
          mBuilder.withDirectMode(StreetMode.FLEXIBLE);
        }
      }
    }

    return mBuilder.build();
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (QualifiedMode qm : qModes) {
      sb.append(qm.toString());
      sb.append(" ");
    }
    return sb.toString();
  }
}
