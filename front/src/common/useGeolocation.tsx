import { useState, useEffect, useMemo } from 'react';

// interface locationType {
//   loaded: boolean;
//   coordinates?: { lat: number; lng: number };
//   error?: { code: number; message: string };
// }

const useGeolocation = () => {
  const [location, setLocation] = useState<
    { latitude: number; longitude: number } | string
  >('');

  useMemo(() => {
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(success, error);
    }

    function success(position: any) {
      setLocation({
        latitude: position.coords.latitude,
        longitude: position.coords.longitude,
      });
    }

    function error() {
      setLocation({
        latitude: 37.483034,
        longitude: 126.902435,
      });
      console.log('위치 받기 실패');
    }
  }, [navigator.geolocation.getCurrentPosition]);

  return location;
};

export default useGeolocation;
