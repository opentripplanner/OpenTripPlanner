import { Table } from 'react-bootstrap';
import { TripPattern, Leg } from '../../static/query/tripQueryTypes.js';
import { formatTime } from '../../util/formatTime.ts';
import { formatDuration } from '../../util/formatDuration.ts';
import { formatDistance } from '../../util/distanceUtils.ts';

// Create a unique, content-based key for each leg
const createLegKey = (leg: Leg, itineraryIdx: number): string => {
  if (!leg) return `itinerary_${itineraryIdx}_leg_invalid`;
  // Primary: Use leg ID if available
  if (leg.id) {
    return `itinerary_${itineraryIdx}_leg_${leg.id}`;
  }

  // Fallback: Create key from leg content
  const legSignature = `${leg.mode}_${leg.aimedStartTime}_${leg.aimedEndTime}_${leg.fromPlace?.name || 'unknown'}_${leg.toPlace?.name || 'unknown'}`;
  return `itinerary_${itineraryIdx}_leg_${legSignature}`;
};

interface SelectedLegsComparisonTableProps {
  itinerary1: TripPattern;
  itinerary2: TripPattern;
  selectedLegIds: string[];
  selectedIndexes: number[];
  timeZone: string;
}

export function SelectedLegsComparisonTable({
  itinerary1,
  itinerary2,
  selectedLegIds,
  selectedIndexes,
  timeZone,
}: SelectedLegsComparisonTableProps) {
  if (selectedLegIds.length === 0) {
    return null;
  }

  return (
    <div className="compare-selected-legs-section">
      <h6 className="compare-selected-legs-title">Selected Legs Comparison</h6>
      <Table striped bordered hover>
        <thead>
          <tr>
            <th>Itinerary</th>
            <th>From</th>
            <th>To</th>
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
              .filter(
                (leg: Leg) =>
                  leg && selectedLegIds.includes(leg.id || `leg${itineraryIdx + 1}_${tripPattern.legs.indexOf(leg)}`),
              )
              .map((leg: Leg) => (
                <tr key={createLegKey(leg, itineraryIdx)}>
                  <td>
                    <strong>#{selectedIndexes[itineraryIdx] + 1}</strong>
                  </td>
                  <td>{leg.fromPlace?.name || '-'}</td>
                  <td>{leg.toPlace?.name || '-'}</td>
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
  );
}
