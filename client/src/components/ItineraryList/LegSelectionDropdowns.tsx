import Select from 'react-select';
import { Leg } from '../../static/query/tripQueryTypes.js';
import { createLegOptions } from './compareUtils.ts';

interface LegSelectionDropdownsProps {
  itinerary1Legs: Leg[];
  itinerary2Legs: Leg[];
  selectedLeg1Ids: string[];
  selectedLeg2Ids: string[];
  setSelectedLeg1Ids: (ids: string[]) => void;
  setSelectedLeg2Ids: (ids: string[]) => void;
  selectedIndexes: number[];
  timeZone: string;
}

export function LegSelectionDropdowns({
  itinerary1Legs,
  itinerary2Legs,
  selectedLeg1Ids,
  selectedLeg2Ids,
  setSelectedLeg1Ids,
  setSelectedLeg2Ids,
  selectedIndexes,
  timeZone,
}: LegSelectionDropdownsProps) {
  const leg1Options = createLegOptions(itinerary1Legs, 1, timeZone);
  const leg2Options = createLegOptions(itinerary2Legs, 2, timeZone);

  return (
    <div className="compare-section-divider">
      <div className="compare-legs-select-container">
        <div className="compare-leg-select-column">
          <label className="compare-leg-select-label">Itinerary #{selectedIndexes[0] + 1} Legs:</label>
          <Select
            isMulti
            options={leg1Options}
            value={leg1Options.filter((option) => selectedLeg1Ids.includes(option.value))}
            onChange={(selectedOptions) => {
              const selectedIds = selectedOptions ? selectedOptions.map((option) => option.value) : [];
              setSelectedLeg1Ids(selectedIds);
            }}
            placeholder="Select legs to compare..."
            className="compare-leg-select"
            classNamePrefix="compare-leg-select"
          />
        </div>
        <div className="compare-leg-select-column">
          <label className="compare-leg-select-label">Itinerary #{selectedIndexes[1] + 1} Legs:</label>
          <Select
            isMulti
            options={leg2Options}
            value={leg2Options.filter((option) => selectedLeg2Ids.includes(option.value))}
            onChange={(selectedOptions) => {
              const selectedIds = selectedOptions ? selectedOptions.map((option) => option.value) : [];
              setSelectedLeg2Ids(selectedIds);
            }}
            placeholder="Select legs to compare..."
            className="compare-leg-select"
            classNamePrefix="compare-leg-select"
          />
        </div>
      </div>
    </div>
  );
}
