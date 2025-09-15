import { Table } from 'react-bootstrap';
import { TripPatternType, LegType } from './compareUtils.ts';
import { formatTime } from '../../util/formatTime.ts';
import { formatDuration } from '../../util/formatDuration.ts';
import { formatDistance } from '../../util/formatDistance.ts';

interface SelectedLegsComparisonTableProps {
  itinerary1: TripPatternType;
  itinerary2: TripPatternType;
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
              .filter((leg: LegType) =>
                selectedLegIds.includes(leg.id || `leg${itineraryIdx + 1}_${tripPattern.legs.indexOf(leg)}`),
              )
              .map((leg: LegType, legIdx: number) => (
                <tr key={`${itineraryIdx}_${legIdx}`}>
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