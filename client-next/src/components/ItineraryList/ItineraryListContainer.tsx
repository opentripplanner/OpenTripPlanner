import { QueryType } from '../../gql/graphql.ts';
import { Accordion } from 'react-bootstrap';
import { useContainerWidth } from './useContainerWidth.ts';
import { ItineraryHeaderContent } from './ItineraryHeaderContent.tsx';
import { useEarliestAndLatestTimes } from './useEarliestAndLatestTimes.ts';

// TODO itinerary (accordion) selection should propagate to map view
export function ItineraryListContainer({ tripQueryResult }: { tripQueryResult: QueryType | null }) {
  const [earliestStartTime, latestEndTime] = useEarliestAndLatestTimes(tripQueryResult);
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
