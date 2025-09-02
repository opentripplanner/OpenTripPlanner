import { useQuayCoordinateQuery } from '../../hooks/useQuayCoordinateQuery.ts';
import { GeometryPropertyPopup } from './GeometryPropertyPopup.tsx';

export function HoveredQuayPopup({ hoveredQuayInfo }: { hoveredQuayInfo: { id: string; name: string } | null }) {
  // Use the quay coordinate query to fetch coordinates
  const quay = useQuayCoordinateQuery({ place: hoveredQuayInfo?.id || '' });

  if (!hoveredQuayInfo || !quay?.latitude || !quay?.longitude) {
    return null;
  }

  return (
    <GeometryPropertyPopup
      coordinates={
        {
          lng: quay.longitude,
          lat: quay.latitude,
          wrap: () => ({ lng: quay.longitude, lat: quay.latitude }),
          toArray: () => [quay.longitude, quay.latitude],
          distanceTo: () => 0,
        } as {
          lng: number;
          lat: number;
          wrap: () => { lng: number; lat: number };
          toArray: () => number[];
          distanceTo: () => number;
        }
      }
      properties={{
        'Quay ID': hoveredQuayInfo.id,
        Name: hoveredQuayInfo.name,
        Type: 'Stop Place',
      }}
      onClose={() => {}} // No close button for hover popup
    />
  );
}
