import { Component } from 'react';
import DebugLayerControl from './LayerControl';
import { ControlPosition } from 'react-map-gl';
import debugLayerIcon from '../../static/img/graph.svg';
import { MapRef } from 'react-map-gl/maplibre';

interface RightMenuProps {
  setInteractiveLayerIds: (interactiveLayerIds: string[]) => void;
  position: ControlPosition;
  mapRef: MapRef | null;
}

interface RightMenuState {
  isSidebarOpen: boolean;
  activeContent: 'debugLayer' | 'tripFilters' | null; // Track the active content
}

class RightMenu extends Component<RightMenuProps, RightMenuState> {
  constructor(props: RightMenuProps) {
    super(props);
    this.state = {
      isSidebarOpen: false,
      activeContent: null,
    };
  }

  // Method to toggle the sidebar and set the active content
  toggleSidebar = (content: RightMenuState['activeContent']) => {
    this.setState((prevState) => ({
      isSidebarOpen: prevState.activeContent !== content || !prevState.isSidebarOpen,
      activeContent: prevState.activeContent === content ? null : content, // Toggle content
    }));
  };

  render() {
    const { isSidebarOpen, activeContent } = this.state;
    const { position, setInteractiveLayerIds, mapRef } = this.props;

    return (
      <div>
        {/* Buttons to control sidebar */}
        <button
          onClick={() => this.toggleSidebar('debugLayer')}
          className={`sidebar-button right ${activeContent === 'debugLayer' ? 'active' : ''} ${
            isSidebarOpen ? 'open' : ''
          }`}
          style={{
            top: '20px',
          }}
        >
          <img src={debugLayerIcon} alt="Debug layer" title="Debug layer" style={{ width: '25px', height: '25px' }} />
        </button>

        {/* Sidebar */}
        <div
          className="right-menu-container"
          style={{
            position: 'absolute',
            top: 0,
            right: 0,
            width: isSidebarOpen ? '270px' : '0',
            height: '100%',
            backgroundColor: '#f4f4f4',
            overflowX: 'hidden',
            transition: '0.3s',
            paddingTop: '20px',
            boxShadow: isSidebarOpen ? '-2px 0 5px rgba(0, 0, 0, 0.2)' : 'none',
          }}
        >
          {isSidebarOpen && activeContent === 'debugLayer' && (
            <DebugLayerControl position={position} setInteractiveLayerIds={setInteractiveLayerIds} mapRef={mapRef} />
          )}
        </div>
      </div>
    );
  }
}

export default RightMenu;
