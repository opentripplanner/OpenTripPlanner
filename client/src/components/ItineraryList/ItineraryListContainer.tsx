import { QueryType, TripQueryVariables } from '../../gql/graphql.ts';
import { Accordion } from 'react-bootstrap';
import { useContainerWidth } from './useContainerWidth.ts';
import { ItineraryHeaderContent } from './ItineraryHeaderContent.tsx';
import { useEarliestAndLatestTimes } from './useEarliestAndLatestTimes.ts';
import { ItineraryDetails } from './ItineraryDetails.tsx';
import { ItineraryPaginationControl } from './ItineraryPaginationControl.tsx';
import { useContext } from 'react';
import { TimeZoneContext } from '../../hooks/TimeZoneContext.ts';
import { useState } from 'react';
import ViewArgumentsRaw from '../SearchInput/ViewArgumentsRaw.tsx';
import TripQueryArguments from '../SearchInput/TripQueryArguments.tsx';

export function ItineraryListContainer({
  tripQueryResult,
  selectedTripPatternIndex,
  setSelectedTripPatternIndex,
  pageResults,
  loading,
  tripQueryVariables,
  setTripQueryVariables,
}: {
  tripQueryResult: QueryType | null;
  selectedTripPatternIndex: number;
  setSelectedTripPatternIndex: (selectedTripPatterIndex: number) => void;
  pageResults: (cursor: string) => void;
  loading: boolean;
  tripQueryVariables: TripQueryVariables;
  setTripQueryVariables: (variables: TripQueryVariables) => void;
}) {
  const [earliestStartTime, latestEndTime] = useEarliestAndLatestTimes(tripQueryResult);
  const { containerRef, containerWidth } = useContainerWidth();
  const timeZone = useContext(TimeZoneContext);

  // State for toggling between Accordion and new element
  const [showArguments, setShowArguments] = useState(false);

  return (
      <section className="itinerary-list-container below-content" ref={containerRef}>
          <div className="pagination-controls">
              <ItineraryPaginationControl
                  onPagination={pageResults}
                  previousPageCursor={tripQueryResult?.trip.previousPageCursor}
                  nextPageCursor={tripQueryResult?.trip.nextPageCursor}
                  loading={loading}
              />

          </div>
          <button className="toggle-view-button" onClick={() => setShowArguments((prev) => !prev)}>
              {showArguments ? 'Show Accordion' : 'Show Arguments'}
          </button>
          {showArguments ? (
              <div
                  style={{
                      display: 'flex',
                      flexDirection: 'column',
                      padding: '10px',
                      fontSize: '12px',
                      borderRadius: '4px',
                      overflowY: 'auto',
                  }}
              >
                  <h4>
                      Trip arguments <ViewArgumentsRaw
                      tripQueryVariables={tripQueryVariables}></ViewArgumentsRaw>
                  </h4>

                  <TripQueryArguments tripQueryVariables={tripQueryVariables}
                                      setTripQueryVariables={setTripQueryVariables}/>
              </div>
          ) : (
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
                                  <ItineraryDetails tripPattern={tripPattern}/>
                              </Accordion.Body>
                          </Accordion.Item>
                      ))}
              </Accordion>
          )}

          <div className="time-zone-info">
              All times in <code>{timeZone}</code>
          </div>
      </section>
  );
}
