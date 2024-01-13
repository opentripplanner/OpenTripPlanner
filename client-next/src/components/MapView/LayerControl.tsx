import type { ControlPosition } from 'react-map-gl';
import { useControl } from 'react-map-gl';
import { Map } from 'maplibre-gl';

type LayerControlProps = {
  position?: ControlPosition;
};

class LayerList {
  private map: Map | null = null;
  private _container: HTMLDivElement;

  onAdd(map: Map) {
    this.map = map;
    this._container = document.createElement('div');
    this._container.className = 'maplibregl-ctrl maplibregl-ctrl-group layer-select';

    map?.on('load', () => {
      while (this._container.firstChild) {
        this._container.removeChild(this._container.firstChild);
      }

      const h3 = document.createElement('h6');
      h3.textContent = 'Debug layers';
      this._container.appendChild(h3);

      map
        ?.getLayersOrder()
        .map((l) => map.getLayer(l))
        .filter((s) => s?.type !== 'raster')
        .reverse()
        .forEach((layer) => {
          const div = document.createElement('div');
          const input = document.createElement('input');
          input.type = 'checkbox';
          input.value = layer?.id;
          input.onchange = (e) => {
            e.preventDefault();
            e.stopPropagation();

            if (this.layerVisible(layer)) {
              map.setLayoutProperty(layer.id, 'visibility', 'none');
            } else {
              map.setLayoutProperty(layer.id, 'visibility', 'visible');
            }
          };
          const visible = map.getLayoutProperty(layer.id, 'visibility') !== 'none';
          input.checked = visible;
          const label = document.createElement('label');
          label.textContent = layer.id;
          div.appendChild(input);
          div.appendChild(label);
          this._container.appendChild(div);
        });
    });

    return this._container;
  }

  private layerVisible(layer: { id: string }) {
    return this.map?.getLayoutProperty(layer.id, 'visibility') !== 'none';
  }

  onCreate() {}

  onRemove() {
    this._container.parentNode.removeChild(this._container);
    this.map = undefined;
  }
}

export default function LayerListControl(props: LayerControlProps) {
  useControl(() => new LayerList(), {
    position: props.position,
  });

  return null;
}
