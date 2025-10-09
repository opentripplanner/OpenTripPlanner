import { TripQuery } from '../../gql/graphql.ts';
import { Accordion } from 'react-bootstrap';
import { useContainerWidth } from './useContainerWidth.ts';
import { ItineraryHeaderContent } from './ItineraryHeaderContent.tsx';
import { useEarliestAndLatestTimes } from './useEarliestAndLatestTimes.ts';
import { ItineraryDetails } from './ItineraryDetails.tsx';
import { ItineraryPaginationControl } from './ItineraryPaginationControl.tsx';
import { useContext, Dispatch, SetStateAction } from 'react';
import { TimeZoneContext } from '../../hooks/TimeZoneContext.ts';
import { ErrorDisplay } from './ErrorDisplay.tsx';
import { NoResultsDisplay } from './NoResultsDisplay.tsx';

export function ItineraryListContainer({
  tripQueryResult,
  selectedTripPatternIndexes,
  setSelectedTripPatternIndexes,
  pageResults,
  loading,
  comparisonSelectedIndexes,
  setComparisonSelectedIndexes,
  onCompare,
  error,
}: {
  tripQueryResult: TripQuery | null;
  selectedTripPatternIndexes: number[];
  setSelectedTripPatternIndexes: Dispatch<SetStateAction<number[]>>;
  pageResults: (cursor: string) => void;
  loading: boolean;
  comparisonSelectedIndexes: number[];
  setComparisonSelectedIndexes: (indexes: number[]) => void;
  onCompare: () => void;
  error?: unknown;
}) {
  const [earliestStartTime, latestEndTime] = useEarliestAndLatestTimes(tripQueryResult);
  const { containerRef, containerWidth } = useContainerWidth();
  const timeZone = useContext(TimeZoneContext);

  const hasNoResults = Boolean(tripQueryResult && tripQueryResult.trip.tripPatterns.length === 0);
  const hasSearched = tripQueryResult !== null;
  const showErrorOrNoResults = error || (hasSearched && !error && hasNoResults);

  return (
    <section className="left-pane-container below-content" ref={containerRef}>
      <>
        <div className="panel-header">Itinerary results</div>
        <ErrorDisplay error={error} />
        <NoResultsDisplay hasSearched={hasSearched && !error && hasNoResults} tripQueryResult={tripQueryResult} />
        {!showErrorOrNoResults && (
          <div className="pagination-controls">
            <ItineraryPaginationControl
              onPagination={pageResults}
              previousPageCursor={tripQueryResult?.trip.previousPageCursor}
              nextPageCursor={tripQueryResult?.trip.nextPageCursor}
              loading={loading}
            />
          </div>
        )}
        <div className="pagination-controls">
          <ItineraryPaginationControl
            onPagination={pageResults}
            previousPageCursor={tripQueryResult?.trip.previousPageCursor}
            nextPageCursor={tripQueryResult?.trip.nextPageCursor}
            loading={loading}
            comparisonSelectedIndexes={comparisonSelectedIndexes}
            onCompare={onCompare}
          />
        </div>
        <Accordion
          activeKey={selectedTripPatternIndexes.map(String)}
          onSelect={(eventKey) => {
            const index = parseInt(eventKey as string);
            setSelectedTripPatternIndexes((prev: number[]) => {
              if (prev.includes(index)) {
                return prev.filter((i: number) => i !== index);
              } else {
                const newArray = [...prev, index];
                return newArray.length > 2 ? newArray.slice(-2) : newArray;
              }
            });
          }}
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
                    comparisonSelectedIndexes={comparisonSelectedIndexes}
                    setComparisonSelectedIndexes={setComparisonSelectedIndexes}
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
