import { Modal } from 'react-bootstrap';
import { TripQuery } from '../../gql/graphql.ts';
import { useContext, useState } from 'react';
import { TimeZoneContext } from '../../hooks/TimeZoneContext.ts';
import { getItinerarySummary } from './compareUtils.ts';
import { ItinerarySummaryTable } from './ItinerarySummaryTable.tsx';
import { LegSelectionDropdowns } from './LegSelectionDropdowns.tsx';
import { SelectedLegsComparisonTable } from './SelectedLegsComparisonTable.tsx';

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
  const timeZone = useContext(TimeZoneContext);
  const [selectedLeg1Ids, setSelectedLeg1Ids] = useState<string[]>([]);
  const [selectedLeg2Ids, setSelectedLeg2Ids] = useState<string[]>([]);

  const selectedLegIds = [...selectedLeg1Ids, ...selectedLeg2Ids];

  if (!tripQueryResult || selectedIndexes.length !== 2) {
    return null;
  }

  const itinerary1 = tripQueryResult.trip.tripPatterns[selectedIndexes[0]];
  const itinerary2 = tripQueryResult.trip.tripPatterns[selectedIndexes[1]];

  const summary1 = getItinerarySummary(itinerary1, timeZone);
  const summary2 = getItinerarySummary(itinerary2, timeZone);

  return (
    <Modal show={show} onHide={onHide} centered className="full-width-modal">
      <Modal.Header closeButton>
        <Modal.Title>Compare Itineraries</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <ItinerarySummaryTable
          itinerary1={itinerary1}
          itinerary2={itinerary2}
          summary1={summary1}
          summary2={summary2}
          selectedIndexes={selectedIndexes}
        />

        <LegSelectionDropdowns
          itinerary1Legs={itinerary1.legs}
          itinerary2Legs={itinerary2.legs}
          selectedLeg1Ids={selectedLeg1Ids}
          selectedLeg2Ids={selectedLeg2Ids}
          setSelectedLeg1Ids={setSelectedLeg1Ids}
          setSelectedLeg2Ids={setSelectedLeg2Ids}
          selectedIndexes={selectedIndexes}
          timeZone={timeZone}
        />

        <SelectedLegsComparisonTable
          itinerary1={itinerary1}
          itinerary2={itinerary2}
          selectedLegIds={selectedLegIds}
          selectedIndexes={selectedIndexes}
          timeZone={timeZone}
        />
      </Modal.Body>
    </Modal>
  );
}
