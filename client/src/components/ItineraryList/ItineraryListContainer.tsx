import { QueryType } from '../../gql/graphql.ts';
import { Accordion } from 'react-bootstrap';
import { useContainerWidth } from './useContainerWidth.ts';
import { ItineraryHeaderContent } from './ItineraryHeaderContent.tsx';
import { useEarliestAndLatestTimes } from './useEarliestAndLatestTimes.ts';
import { ItineraryDetails } from './ItineraryDetails.tsx';
import { ItineraryPaginationControl } from './ItineraryPaginationControl.tsx';
import { useContext } from 'react';
import { TimeZoneContext } from '../../hooks/TimeZoneContext.ts';

export function ItineraryListContainer({
  tripQueryResult,
  selectedTripPatternIndex,
  setSelectedTripPatternIndex,
  pageResults,
  loading,
}: {
  tripQueryResult: QueryType | null;
  selectedTripPatternIndex: number;
  setSelectedTripPatternIndex: (selectedTripPatterIndex: number) => void;
  pageResults: (cursor: string) => void;
  loading: boolean;
}) {
  const [earliestStartTime, latestEndTime] = useEarliestAndLatestTimes(tripQueryResult);
  const { containerRef, containerWidth } = useContainerWidth();
  const timeZone = useContext(TimeZoneContext);

  return (
    <section className="left-pane-container below-content" ref={containerRef}>
      <>
        <div className="panel-header">Itinerary results</div>
        <div className="pagination-controls">
          <ItineraryPaginationControl
            onPagination={pageResults}
            previousPageCursor={tripQueryResult?.trip.previousPageCursor}
            nextPageCursor={tripQueryResult?.trip.nextPageCursor}
            loading={loading}
          />
        </div>
        <Accordion
          activeKey={`${selectedTripPatternIndex}`}
          onSelect={(eventKey) => setSelectedTripPatternIndex(parseInt(eventKey as string))}
        >
          {tripQueryResult &&
            tripQueryResult.trip.tripPatterns.map((tripPattern, itineraryIndex) => (
              <Accordion.Item
                eventKey={`${itineraryIndex}`}
                key={`${itineraryIndex}`}
                bsPrefix={tripPattern.systemNotices.length === 0 ? '' : 'accordion-item-filtered'}
              >
                <Accordion.Header>
                  <ItineraryHeaderContent
                    containerWidth={containerWidth}
                    tripPattern={tripPattern}
                    itineraryIndex={itineraryIndex}
                    earliestStartTime={earliestStartTime}
                    latestEndTime={latestEndTime}
                  />
                </Accordion.Header>
                <Accordion.Body>
                  <ItineraryDetails tripPattern={tripPattern} />
                </Accordion.Body>
              </Accordion.Item>
            ))}
        </Accordion>
      </>

      {/* Time Zone Info */}
      <div className="time-zone-info">
        All times in <code>{timeZone}</code>
      </div>
    </section>
  );
}
