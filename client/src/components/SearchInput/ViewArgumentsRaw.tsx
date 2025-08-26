import React from 'react';
import { TripQueryVariables } from '../../gql/graphql.ts';
import ResetButton from './ResetButton.tsx';

interface ViewArgumentsRawProps {
  tripQueryVariables: TripQueryVariables;
  setTripQueryVariables: (tripQueryVariables: TripQueryVariables) => void;
  setExpandedArguments?: (expandedArguments: Record<string, boolean>) => void;
}

const ViewArgumentsRaw: React.FC<ViewArgumentsRawProps> = ({ tripQueryVariables, setTripQueryVariables, setExpandedArguments }) => {
  return (
    <div className={'left-pane-container below-content'} style={{ fontSize: '14px' }}>
      <div className="panel-header">
        Request raw
        <ResetButton 
          tripQueryVariables={tripQueryVariables} 
          setTripQueryVariables={setTripQueryVariables} 
          setExpandedArguments={setExpandedArguments}
        />
      </div>

      <pre>{JSON.stringify(tripQueryVariables, null, 2)}</pre>
    </div>
  );
};

export default ViewArgumentsRaw;
