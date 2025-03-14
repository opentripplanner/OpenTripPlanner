import { useMemo } from 'react';
import { TripPattern } from '../../static/query/tripQueryTypes';

const CONTAINER_WIDTH_PADDING = 70;
const START_PX_PADDING = 20;

// Width of time box
export const TIME_BOX_WIDTH = 40;

export function useHeaderContentStyleCalculations(
  tripPattern: TripPattern,
  containerWidth: number,
  earliestStartTime: string | null,
  latestEndTime: string | null,
) {
  const maxSpan = useMemo(
    () => new Date(latestEndTime!).getTime() - new Date(earliestStartTime!).getTime(),
    [earliestStartTime, latestEndTime],
  );

  const startPct = useMemo(
    () => (new Date(tripPattern.expectedStartTime).getTime() - new Date(earliestStartTime!).getTime()) / maxSpan,
    [tripPattern.expectedStartTime, earliestStartTime, maxSpan],
  );

  const itinSpan = useMemo(
    () => new Date(tripPattern.expectedEndTime).getTime() - new Date(tripPattern.expectedStartTime).getTime(),
    [tripPattern.expectedStartTime, tripPattern.expectedEndTime],
  );

  const startPx = START_PX_PADDING + TIME_BOX_WIDTH;
  const endPx = containerWidth - CONTAINER_WIDTH_PADDING - TIME_BOX_WIDTH;
  const pxSpan = endPx - startPx;
  const leftPx = startPx + startPct * pxSpan;
  const widthPx = pxSpan * (itinSpan / maxSpan);

  return {
    maxSpan,
    startPx,
    pxSpan,
    widthPx,
    leftPx,
  };
}
