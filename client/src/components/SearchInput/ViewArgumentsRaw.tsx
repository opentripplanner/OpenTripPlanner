import React from 'react';

interface ViewArgumentsRawProps {
  tripQueryVariables: any;
}

const ViewArgumentsRaw: React.FC<ViewArgumentsRawProps> = ({ tripQueryVariables }) => {

  return (
      <div className={'left-pane-container below-content'} style={{fontSize: "14px"}}>
        <div className="panel-header">
          Arguments raw
        </div>

        <pre>
            {JSON.stringify(tripQueryVariables, null, 2)}
          </pre>

      </div>
  );
};

export default ViewArgumentsRaw;
