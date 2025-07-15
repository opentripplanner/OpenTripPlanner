import { TIME_BOX_WIDTH, useHeaderContentStyleCalculations } from './useHeaderContentStyleCalculations.ts';
import { ItineraryHeaderLegContent } from './ItineraryHeaderLegContent.tsx';
import { useContext, useMemo } from 'react';
import { formatTime } from '../../util/formatTime.ts';
import { TimeZoneContext } from '../../hooks/TimeZoneContext.ts';
import { TripPattern } from '../../static/query/tripQueryTypes';

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

  const timeZone = useContext(TimeZoneContext);

  const formattedStartTime = useMemo(
    () => formatTime(tripPattern.expectedStartTime, timeZone, 'short'),
    [tripPattern.expectedStartTime, timeZone],
  );

  const formattedEndTime = useMemo(
    () => formatTime(tripPattern.expectedEndTime, timeZone, 'short'),
    [tripPattern.expectedEndTime, timeZone],
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
        title={tripPattern.expectedStartTime}
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
        title={tripPattern.expectedEndTime}
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
