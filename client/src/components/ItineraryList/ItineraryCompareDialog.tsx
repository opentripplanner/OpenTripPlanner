import { Modal, Table } from 'react-bootstrap';
import { TripQuery, Mode } from '../../gql/graphql.ts';
import { useContainerWidth } from './useContainerWidth.ts';
import { useContext, useState } from 'react';
import { TimeZoneContext } from '../../hooks/TimeZoneContext.ts';
import { formatTime } from '../../util/formatTime.ts';
import { formatDuration } from '../../util/formatDuration.ts';
import { formatDistance } from '../../util/formatDistance.ts';

export function ItineraryCompareDialog({
  show,
  onHide,
  tripQueryResult,
  selectedIndexes,
}: {
  show: boolean;
  onHide: () => void;
  tripQueryResult: TripQuery | null;
  selectedIndexes: number[];
}) {
  const { containerRef, containerWidth } = useContainerWidth();
  const timeZone = useContext(TimeZoneContext);
  const [selectedLegIds, setSelectedLegIds] = useState<string[]>([]);
  const [showLegSelection, setShowLegSelection] = useState<boolean>(false);

  if (!tripQueryResult || selectedIndexes.length !== 2) {
    return null;
  }

  const itinerary1 = tripQueryResult.trip.tripPatterns[selectedIndexes[0]];
  const itinerary2 = tripQueryResult.trip.tripPatterns[selectedIndexes[1]];

  const handleLegToggle = (legId: string) => {
    setSelectedLegIds((prev) => (prev.includes(legId) ? prev.filter((id) => id !== legId) : [...prev, legId]));
  };

  const getItinerarySummary = (tripPattern: any) => {
    const startTime = formatTime(tripPattern.expectedStartTime, timeZone, 'short');
    const endTime = formatTime(tripPattern.expectedEndTime, timeZone, 'short');
    const duration = formatDuration(tripPattern.duration);
    const generalizedCost = tripPattern.generalizedCost;

    const walkingDistance = tripPattern.legs
      .filter((leg: any) => leg.mode === Mode.Foot)
      .reduce((sum: number, leg: any) => sum + leg.distance, 0);

    const totalLegs = tripPattern.legs.length;

    return {
      startTime,
      endTime,
      duration,
      generalizedCost,
      walkingDistance: formatDistance(walkingDistance),
      totalLegs,
    };
  };

  const summary1 = getItinerarySummary(itinerary1);
  const summary2 = getItinerarySummary(itinerary2);

  return (
    <Modal show={show} onHide={onHide} size="xl" centered>
      <Modal.Header closeButton>
        <Modal.Title>Compare Itineraries</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <Table striped bordered hover>
          <thead>
            <tr>
              <th>Itinerary</th>
              <th>Start Time</th>
              <th>End Time</th>
              <th>Duration</th>
              <th>Cost</th>
              <th>Total Legs</th>
              <th>Walking Distance</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>
                <strong>#{selectedIndexes[0] + 1}</strong>
              </td>
              <td>{summary1.startTime}</td>
              <td>{summary1.endTime}</td>
              <td>{summary1.duration}</td>
              <td>¢{summary1.generalizedCost}</td>
              <td>{summary1.totalLegs}</td>
              <td>{summary1.walkingDistance}</td>
            </tr>
            <tr>
              <td>
                <strong>#{selectedIndexes[1] + 1}</strong>
              </td>
              <td>{summary2.startTime}</td>
              <td>{summary2.endTime}</td>
              <td>{summary2.duration}</td>
              <td>¢{summary2.generalizedCost}</td>
              <td>{summary2.totalLegs}</td>
              <td>{summary2.walkingDistance}</td>
            </tr>
          </tbody>
        </Table>

        <div style={{ marginTop: '20px' }}>
          <button
            onClick={() => setShowLegSelection(!showLegSelection)}
            style={{
              background: 'none',
              border: '1px solid #dee2e6',
              borderRadius: '4px',
              padding: '8px 12px',
              cursor: 'pointer',
              fontSize: '14px',
              marginBottom: '10px',
            }}
          >
            {showLegSelection ? '▼' : '▶'} Select Individual Legs to Compare
          </button>

          {showLegSelection && (
            <div style={{ display: 'flex', gap: '20px' }}>
              <div style={{ flex: 1 }}>
                <h6>Itinerary {selectedIndexes[0] + 1} - Legs</h6>
                {itinerary1.legs.map((leg: any, i: number) => (
                  <div
                    key={leg.id || `leg1_${i}`}
                    style={{ padding: '4px', border: '1px solid #dee2e6', marginBottom: '2px', borderRadius: '2px' }}
                  >
                    <label style={{ display: 'flex', alignItems: 'center', cursor: 'pointer', fontSize: '13px' }}>
                      <input
                        type="checkbox"
                        checked={selectedLegIds.includes(leg.id || `leg1_${i}`)}
                        onChange={() => handleLegToggle(leg.id || `leg1_${i}`)}
                        style={{ marginRight: '6px' }}
                      />
                      <span>
                        <strong>{leg.mode}</strong> {leg.line?.publicCode && `(${leg.line.publicCode})`} -
                        {formatTime(leg.aimedStartTime, timeZone, 'short')} to{' '}
                        {formatTime(leg.aimedEndTime, timeZone, 'short')}({formatDuration(leg.duration)})
                      </span>
                    </label>
                  </div>
                ))}
              </div>
              <div style={{ flex: 1 }}>
                <h6>Itinerary {selectedIndexes[1] + 1} - Legs</h6>
                {itinerary2.legs.map((leg: any, i: number) => (
                  <div
                    key={leg.id || `leg2_${i}`}
                    style={{ padding: '4px', border: '1px solid #dee2e6', marginBottom: '2px', borderRadius: '2px' }}
                  >
                    <label style={{ display: 'flex', alignItems: 'center', cursor: 'pointer', fontSize: '13px' }}>
                      <input
                        type="checkbox"
                        checked={selectedLegIds.includes(leg.id || `leg2_${i}`)}
                        onChange={() => handleLegToggle(leg.id || `leg2_${i}`)}
                        style={{ marginRight: '6px' }}
                      />
                      <span>
                        <strong>{leg.mode}</strong> {leg.line?.publicCode && `(${leg.line.publicCode})`} -
                        {formatTime(leg.aimedStartTime, timeZone, 'short')} to{' '}
                        {formatTime(leg.aimedEndTime, timeZone, 'short')}({formatDuration(leg.duration)})
                      </span>
                    </label>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        {selectedLegIds.length > 0 && (
          <div style={{ marginTop: '20px' }}>
            <h5>Selected Legs Comparison</h5>
            <Table striped bordered hover>
              <thead>
                <tr>
                  <th>Itinerary</th>
                  <th>Mode</th>
                  <th>Line</th>
                  <th>Start Time</th>
                  <th>End Time</th>
                  <th>Duration</th>
                  <th>Distance</th>
                  <th>Cost</th>
                </tr>
              </thead>
              <tbody>
                {[itinerary1, itinerary2].map((tripPattern, itineraryIdx) =>
                  tripPattern.legs
                    .filter((leg: any) =>
                      selectedLegIds.includes(leg.id || `leg${itineraryIdx + 1}_${tripPattern.legs.indexOf(leg)}`),
                    )
                    .map((leg: any, legIdx: number) => (
                      <tr key={`${itineraryIdx}_${legIdx}`}>
                        <td>
                          <strong>#{selectedIndexes[itineraryIdx] + 1}</strong>
                        </td>
                        <td>{leg.mode}</td>
                        <td>{leg.line?.publicCode || leg.line?.name || '-'}</td>
                        <td>{formatTime(leg.aimedStartTime, timeZone, 'short')}</td>
                        <td>{formatTime(leg.aimedEndTime, timeZone, 'short')}</td>
                        <td>{formatDuration(leg.duration)}</td>
                        <td>{formatDistance(leg.distance)}</td>
                        <td>¢{leg.generalizedCost}</td>
                      </tr>
                    )),
                )}
              </tbody>
            </Table>
          </div>
        )}
      </Modal.Body>
    </Modal>
  );
}
