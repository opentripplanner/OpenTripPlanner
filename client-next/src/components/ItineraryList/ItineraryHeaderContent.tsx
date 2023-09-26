import { TripPattern } from '../../gql/graphql.ts';
import { useMemo } from 'react';

export function ItineraryHeaderContent({
  containerWidth,
  tripPattern,
  itineraryIndex,
  earliestStartTime,
  latestEndTime,
}: {
  containerWidth: number;
  tripPattern: TripPattern;
  itineraryIndex: number;
  earliestStartTime: string | null;
  latestEndTime: string | null;
}) {
  // TODO are we using the correct values
  // var maxSpan = itin.tripPlan.latestEndTime - itin.tripPlan.earliestStartTime;
  const maxSpan = useMemo(
    () => new Date(latestEndTime!).getTime() - new Date(earliestStartTime!).getTime(),
    [earliestStartTime, latestEndTime],
  );

  // TODO are we using the correct values
  // var startPct = (itin.itinData.startTime - itin.tripPlan.earliestStartTime) / maxSpan;
  const startPct = useMemo(
    () => (new Date(tripPattern.aimedStartTime).getTime() - new Date(earliestStartTime!).getTime()) / maxSpan,
    [tripPattern.aimedStartTime, earliestStartTime],
  );

  // TODO are we using the correct values
  // var itinSpan = itin.getEndTime() - itin.getStartTime();
  const itinSpan = useMemo(
    () => new Date(tripPattern.aimedEndTime).getTime() - new Date(tripPattern.aimedStartTime).getTime(),
    [tripPattern.aimedStartTime, tripPattern.aimedEndTime],
  );

  const CONTAINER_WIDTH_PADDING = 90;
  const paddedContainerWidth = containerWidth - CONTAINER_WIDTH_PADDING;

  // var timeWidth = 40;
  const timeWidth = 40;
  // var startPx = 20 + timeWidth,
  const startPx = 20 + timeWidth;
  //   endPx = div.width() - timeWidth - (itin.groupSize ? 48 : 0);
  const endPx = paddedContainerWidth - timeWidth;

  // var pxSpan = endPx - startPx;
  const pxSpan = endPx - startPx;
  // var leftPx = startPx + startPct * pxSpan;
  const leftPx = startPx + startPct * pxSpan;
  // var widthPx = pxSpan * (itinSpan / maxSpan);
  const widthPx = pxSpan * (itinSpan / maxSpan);

  return (
    <div style={{ position: 'relative' }}>
      <div style={{ position: 'absolute' }}>{itineraryIndex + 1}.</div>
      <div
        style={{
          position: 'absolute',
          width: `${widthPx + 5}px`,
          height: '2px',
          left: `${leftPx - 2}px`,
          top: '9px',
          background: 'black',
        }}
      />
      <div
        style={{
          position: 'absolute',
          left: `${leftPx - timeWidth}px`,
          background: 'black',
          color: 'white',
        }}
      >
        {new Date(tripPattern.aimedStartTime).toLocaleTimeString('en-US', {
          timeStyle: 'short',
          hourCycle: 'h24',
        })}
      </div>

      <div
        style={{
          position: 'absolute',
          left: `${leftPx + widthPx + 2}px`,
          background: 'black',
          color: 'white',
        }}
      >
        {new Date(tripPattern.aimedEndTime).toLocaleTimeString('en-US', {
          timeStyle: 'short',
          hourCycle: 'h24',
        })}
      </div>
    </div>
  );
}
