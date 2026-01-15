import { Table } from 'react-bootstrap';
import { Popup, LngLat, MapGeoJSONFeature } from 'react-map-gl/maplibre';

export function FeatureSelectPopup({
  coordinates,
  features,
  onClose,
  setShowPropsPopup,
}: {
  coordinates: LngLat;
  features: MapGeoJSONFeature[];
  onClose: () => void;
  setShowPropsPopup: ({ coordinates, feature }: { coordinates: LngLat; feature: MapGeoJSONFeature }) => void;
}) {
  return (
    <Popup maxWidth="1000px" latitude={coordinates.lat} longitude={coordinates.lng} onClose={onClose}>
      <strong>Overlapping features</strong>
      <Table bordered>
        <thead>
          <tr>
            <th scope="col">layer id</th>
            <th scope="col">class</th>
            <th scope="col">name/label/vertex labels connected to edge</th>
            <th scope="col">level/levels</th>
          </tr>
        </thead>
        <tbody>
          {features.map((feature, index) => {
            let labelInfo = feature.properties.name || feature.properties.label || 'N/A';
            if (feature.properties.fromVertexLabel && feature.properties.toVertexLabel) {
              labelInfo = `${feature.properties.fromVertexLabel} -> ${feature.properties.toVertexLabel}`;
            }

            let levelInfo = feature.properties.levels || 'N/A';
            if (feature.properties.levelValue) {
              levelInfo = `${feature.properties.levelValue} "${feature.properties.levelName}"`;
            } else if (feature.properties.lowerLevelValue && feature.properties.upperLevelValue) {
              if (feature.properties.lowerVertexLabel === feature.properties.fromVertexLabel) {
                levelInfo = `${feature.properties.lowerLevelValue} "${feature.properties.lowerLevelName}" -> ${feature.properties.upperLevelValue} "${feature.properties.upperLevelName}"`;
              } else {
                levelInfo = `${feature.properties.upperLevelValue} "${feature.properties.upperLevelName}" -> ${feature.properties.lowerLevelValue} "${feature.properties.lowerLevelName}"`;
              }
            }
            return (
              <tr
                onClick={() => {
                  onClose();
                  setShowPropsPopup({ coordinates, feature });
                }}
                className="feature-select-item"
                key={index}
              >
                <td>{feature.layer.id}</td>
                <td>{feature.properties.class}</td>
                <td>{labelInfo}</td>
                <td>{levelInfo}</td>
              </tr>
            );
          })}
        </tbody>
      </Table>
    </Popup>
  );
}
