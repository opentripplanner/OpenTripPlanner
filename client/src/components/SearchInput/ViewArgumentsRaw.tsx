import React, { useState } from 'react';
import { Button, Modal } from 'react-bootstrap';
import codeIcon from '../../static/img/code.svg';

interface ViewArgumentsRawProps {
    tripQueryVariables: any;
}

const ViewArgumentsRaw: React.FC<ViewArgumentsRawProps> = ({ tripQueryVariables }) => {
    const [isDialogOpen, setIsDialogOpen] = useState<boolean>(false);

    const openDialog = () => {
        setIsDialogOpen(true);
    };

    const closeDialog = () => {
        setIsDialogOpen(false);
    };

    return (
      <>
        <Button onClick={openDialog}>
          <img src={codeIcon} alt="Raw arguments" title="Raw arguments" style={{ width: '25px', height: '25px' , filter: 'invert(1)'}} />
        </Button>

        <Modal show={isDialogOpen} onHide={closeDialog} centered>
          <Modal.Header closeButton>
            <Modal.Title>Trip Query Variables</Modal.Title>
          </Modal.Header>
          <Modal.Body>
            <pre style={{ backgroundColor: '#f9f9f9', padding: '10px', maxHeight: '400px', overflowY: 'auto' }}>
              {JSON.stringify(tripQueryVariables, null, 2)}
            </pre>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={closeDialog}>
              Close
            </Button>
          </Modal.Footer>
        </Modal>
      </>
    );
};

export default ViewArgumentsRaw;
