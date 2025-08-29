import { TripQueryVariables } from '../../gql/graphql.ts';
import { excludedArguments } from './excluded-arguments.ts';
import { getNestedValue, setNestedValue } from './nestedUtils.tsx';
import React from 'react';

interface ResetButtonProps {
  tripQueryVariables: TripQueryVariables;
  setTripQueryVariables: (tripQueryVariables: TripQueryVariables) => void;
  setExpandedArguments?: (expandedArguments: Record<string, boolean>) => void;
}

const ResetButton: React.FC<ResetButtonProps> = ({
  tripQueryVariables,
  setTripQueryVariables,
  setExpandedArguments,
}) => {
  function handleReset(): void {
    // Start with an empty object (or partially typed)
    let newVars: TripQueryVariables = {} as TripQueryVariables;

    // For each path in our excluded set, copy over that value (if any)
    excludedArguments.forEach((excludedPath) => {
      const value = getNestedValue(tripQueryVariables, excludedPath);
      if (value !== undefined) {
        newVars = setNestedValue(newVars, excludedPath, value) as TripQueryVariables;
      }
    });

    setTripQueryVariables(newVars);

    // Also reset the expansion state when reset button is pressed
    if (setExpandedArguments) {
      setExpandedArguments({});
    }
  }

  return (
    <button className="reset-button" onClick={handleReset}>
      Reset
    </button>
  );
};

export default ResetButton;
