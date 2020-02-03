package org.opentripplanner.api.mapping;

import org.opentripplanner.api.model.ApiRelativeDirection;
import org.opentripplanner.model.plan.RelativeDirection;

public class RelativeDirectionMapper {
    public static ApiRelativeDirection mapRelativeDirection(RelativeDirection domain) {
        if(domain == null) { return null; }
        switch (domain) {
            case DEPART: return ApiRelativeDirection.DEPART;
            case HARD_LEFT: return ApiRelativeDirection.HARD_LEFT;
            case LEFT: return ApiRelativeDirection.LEFT;
            case SLIGHTLY_LEFT: return ApiRelativeDirection.SLIGHTLY_LEFT;
            case CONTINUE: return ApiRelativeDirection.CONTINUE;
            case SLIGHTLY_RIGHT: return ApiRelativeDirection.SLIGHTLY_RIGHT;
            case RIGHT: return ApiRelativeDirection.RIGHT;
            case HARD_RIGHT: return ApiRelativeDirection.HARD_RIGHT;
            case CIRCLE_CLOCKWISE: return ApiRelativeDirection.CIRCLE_CLOCKWISE;
            case CIRCLE_COUNTERCLOCKWISE: return ApiRelativeDirection.CIRCLE_COUNTERCLOCKWISE;
            case ELEVATOR: return ApiRelativeDirection.ELEVATOR;
            case UTURN_LEFT: return ApiRelativeDirection.UTURN_LEFT;
            case UTURN_RIGHT: return ApiRelativeDirection.UTURN_RIGHT;
            default:
                throw new IllegalArgumentException(domain.toString());
        }
    }
}
