import { TripPattern } from '../../gql/graphql.ts';
import { TIME_BOX_WIDTH, useHeaderContentStyleCalculations } from './useHeaderContentStyleCalculations.ts';
import { ItineraryHeaderLegContent } from './ItineraryHeaderLegContent.tsx';
import { useMemo } from 'react';

export function ItineraryHeaderContent({
  tripPattern,
  itineraryIndex,
  containerWidth,
  earliestStartTime,
  latestEndTime,
}: {
  tripPattern: TripPattern;
  itineraryIndex: number;
  containerWidth: number;
  earliestStartTime: string | null;
  latestEndTime: string | null;
}) {
  const { maxSpan, pxSpan, startPx, widthPx, leftPx } = useHeaderContentStyleCalculations(
    tripPattern,
    containerWidth,
    earliestStartTime,
    latestEndTime,
  );

  const formattedStartTime = useMemo(
    () =>
      new Date(tripPattern.expectedStartTime).toLocaleTimeString('en-US', {
        timeStyle: 'short',
        hourCycle: 'h24',
      }),
    [tripPattern.expectedStartTime],
  );

  const formattedEndTime = useMemo(
    () =>
      new Date(tripPattern.expectedEndTime).toLocaleTimeString('en-US', {
        timeStyle: 'short',
        hourCycle: 'h24',
      }),
    [tripPattern.expectedEndTime],
  );

  return (
    <div className="itinerary-header-wrapper">
      <div className="itinerary-header-itinerary-number">{itineraryIndex + 1}.</div>
      <div
        className="itinerary-header-itinerary-line"
        style={{
          width: `${widthPx + 5}px`,
          left: `${leftPx - 2}px`,
        }}
      />
      <div
        className="itinerary-header-itinerary-time"
        style={{
          left: `${leftPx - TIME_BOX_WIDTH}px`,
        }}
      >
        {formattedStartTime}
      </div>

      {tripPattern.legs.map((leg, i) => (
        <ItineraryHeaderLegContent
          key={leg.id || `leg_${i}`}
          leg={leg}
          earliestStartTime={earliestStartTime}
          startPx={startPx}
          maxSpan={maxSpan}
          pxSpan={pxSpan}
        />
      ))}

      <div
        className="itinerary-header-itinerary-time"
        style={{
          left: `${leftPx + widthPx + 2}px`,
        }}
      >
        {formattedEndTime}
      </div>
    </div>
  );
}
