import { TripPattern, TripQuery } from '../../gql/graphql.ts';
import { Accordion } from 'react-bootstrap';
import { useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react';
import { useContainerWidth } from './useContainerWidth.ts';

function ItineraryHeaderContent({
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
  // var maxSpan = itin.tripPlan.latestEndTime - itin.tripPlan.earliestStartTime;
  const maxSpan = useMemo(
    () => new Date(latestEndTime!).getTime() - new Date(earliestStartTime!).getTime(),
    [earliestStartTime, latestEndTime],
  );

  // var startPct = (itin.itinData.startTime - itin.tripPlan.earliestStartTime) / maxSpan;
  const startPct = useMemo(
    () => (new Date(tripPattern.aimedStartTime).getTime() - new Date(earliestStartTime!).getTime()) / maxSpan,
    [tripPattern.aimedStartTime, earliestStartTime],
  );

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

export function ItineraryListContainer({
  tripQueryResult, //selectedTripPatternIndex,
} //setSelectedTripPatternIndex,
: {
  tripQueryResult: TripQuery | null;
  //selectedTripPatternIndex: number;
  //setSelectedTripPatternIndex: (selectedTripPatternIndex: number) => void;
}) {
  const earliestStartTime = useMemo(() => {
    return (
      tripQueryResult?.trip.tripPatterns.reduce((acc, current) => {
        if (acc === null) {
          return current?.aimedStartTime;
        } else {
          return new Date(current?.aimedStartTime) < new Date(acc) ? current.aimedStartTime : acc;
        }
      }, null) || null
    );
  }, [tripQueryResult?.trip]);

  const latestEndTime = useMemo<string | null>(() => {
    return (
      tripQueryResult?.trip.tripPatterns.reduce((acc, current) => {
        if (acc === null) {
          return current?.aimedEndTime;
        } else {
          return new Date(current?.aimedEndTime) > new Date(acc) ? current.aimedEndTime : acc;
        }
      }, null) || null
    );
  }, [tripQueryResult?.trip]);

  const { containerRef, containerWidth } = useContainerWidth();

  return (
    <section className="itinerary-list-container" ref={containerRef}>
      <Accordion defaultActiveKey="0">
        {tripQueryResult &&
          tripQueryResult.trip.tripPatterns.map((tripPattern, itineraryIndex) => (
            <Accordion.Item eventKey={`${itineraryIndex}`} key={`${itineraryIndex}`}>
              <Accordion.Header>
                <ItineraryHeaderContent
                  containerWidth={containerWidth}
                  tripPattern={tripPattern}
                  itineraryIndex={itineraryIndex}
                  earliestStartTime={earliestStartTime}
                  latestEndTime={latestEndTime}
                />
              </Accordion.Header>
              <Accordion.Body>TODO</Accordion.Body>
            </Accordion.Item>
          ))}
      </Accordion>
    </section>
  );
}
