import { Location } from '../../gql/graphql.ts';
import { useQuayCoordinateQuery } from '../../hooks/useQuayCoordinateQuery.ts';

interface Coordinates {
  latitude: number;
  longitude: number;
}

export function useCoordinateResolver(location: Location): Coordinates | undefined {
  const quay = useQuayCoordinateQuery(location);

  if (quay) {
    const { latitude, longitude } = quay;

    if (latitude && longitude) {
      return {
        latitude,
        longitude,
      };
    }
  }

  if (location.coordinates) {
    return {
      ...location.coordinates,
    };
  }

  return undefined;
}
