import { TripQuery } from '../../gql/graphql.ts';
import { Accordion } from 'react-bootstrap';
import { useMemo } from 'react';
import { useContainerWidth } from './useContainerWidth.ts';
import { ItineraryHeaderContent } from './ItineraryHeaderContent.tsx';

// TODO itinerary (accordion) selection should propagate to map view
export function ItineraryListContainer({
  tripQueryResult, //selectedTripPatternIndex,
  //setSelectedTripPatternIndex,
}: {
  tripQueryResult: TripQuery | null;
  //selectedTripPatternIndex: number;
  //setSelectedTripPatternIndex: (selectedTripPatternIndex: number) => void;
}) {
  const earliestStartTime = useMemo(() => {
    return (
      tripQueryResult?.trip.tripPatterns.reduce((acc, current) => {
        if (acc === null) {
          return current?.expectedStartTime;
        } else {
          return new Date(current?.expectedStartTime) < new Date(acc) ? current.expectedStartTime : acc;
        }
      }, null) || null
    );
  }, [tripQueryResult?.trip]);

  const latestEndTime = useMemo<string | null>(() => {
    return (
      tripQueryResult?.trip.tripPatterns.reduce((acc, current) => {
        if (acc === null) {
          return current?.expectedEndTime;
        } else {
          return new Date(current?.expectedEndTime) > new Date(acc) ? current.expectedEndTime : acc;
        }
      }, null) || null
    );
  }, [tripQueryResult?.trip]);

  const { containerRef, containerWidth } = useContainerWidth();

  return (
    <section className="itinerary-list-container" ref={containerRef}>
      <Accordion>
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
              <Accordion.Body>Itinerary details</Accordion.Body>
            </Accordion.Item>
          ))}
      </Accordion>
    </section>
  );
}
