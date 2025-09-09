import React, { useState, ReactNode } from 'react';
import tripIcon from '../../static/img/route.svg';
import filterIcon from '../../static/img/filter.svg';
import jsonIcon from '../../static/img/json.svg';

interface SidebarProps {
  children: ReactNode | ReactNode[];
}

const Sidebar: React.FC<SidebarProps> = ({ children }) => {
  const [activeIndex, setActiveIndex] = useState<number>(0);

  // Function to return the appropriate image based on the index
  const getIconForIndex = (index: number) => {
    switch (index) {
      case 0:
        return <img src={tripIcon} alt="Itineray list" title="Itineray" />;
      case 1:
        return <img src={filterIcon} alt="Arguments" title="Arguments" />;
      case 2:
        return <img width="25" height="25" src={jsonIcon} alt="Arguments raw" title="Arguments raw" />;
      default:
        return null;
    }
  };

  // Ensure children is always an array and filter out invalid children (null, undefined)
  const childArray = React.Children.toArray(children).filter((child) => React.isValidElement(child));

  return (
    <div className="sidebar-container">
      {/* Sidebar Navigation Buttons */}
      <div className="sidebar">
        {childArray.map((_, index) => (
          <div
            key={index}
            className={`sidebar-button ${index === activeIndex ? 'active' : ''}`}
            onClick={() => setActiveIndex(index)}
          >
            {getIconForIndex(index)}
          </div>
        ))}
      </div>

      {/* Content Area */}
      <div className="sidebar-content">
        {childArray.map((child, index) => (index === activeIndex ? <div key={index}>{child}</div> : null))}
      </div>
    </div>
  );
};

export default Sidebar;
