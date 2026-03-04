import { useState, useEffect, useRef } from 'react';
import { haversineDistance } from '../utils/geo';

export function useLocationTracking(isTracking: boolean) {
  const [distance, setDistance] = useState(0);
  const [currentPos, setCurrentPos] = useState<{lat: number, lng: number} | null>(null);
  const [error, setError] = useState<string | null>(null);
  const watchIdRef = useRef<number | null>(null);
  const lastPosRef = useRef<{lat: number, lng: number} | null>(null);

  useEffect(() => {
    if (isTracking) {
      if ('geolocation' in navigator) {
        setError(null);
        watchIdRef.current = navigator.geolocation.watchPosition(
          (position) => {
            const { latitude, longitude } = position.coords;
            const newPos = { lat: latitude, lng: longitude };
            
            if (lastPosRef.current) {
              const dist = haversineDistance(
                lastPosRef.current.lat,
                lastPosRef.current.lng,
                newPos.lat,
                newPos.lng
              );
              // Filter out GPS jitter (e.g., < 0.001 miles) and crazy jumps (> 10 miles between ticks)
              if (dist > 0.001 && dist < 10) { 
                setDistance((prev) => prev + dist);
              }
            }
            
            lastPosRef.current = newPos;
            setCurrentPos(newPos);
          },
          (err) => {
            console.error("GPS Error:", err);
            setError(err.message);
          },
          {
            enableHighAccuracy: true,
            maximumAge: 0,
            timeout: 5000,
          }
        );
      } else {
        setError("Geolocation is not supported by this browser.");
      }
    } else {
      if (watchIdRef.current !== null) {
        navigator.geolocation.clearWatch(watchIdRef.current);
        watchIdRef.current = null;
      }
      lastPosRef.current = null;
    }

    return () => {
      if (watchIdRef.current !== null) {
        navigator.geolocation.clearWatch(watchIdRef.current);
      }
    };
  }, [isTracking]);

  const resetDistance = () => {
    setDistance(0);
    lastPosRef.current = null;
  };

  // Expose a way to manually add distance for testing/demo purposes
  const addDemoDistance = (miles: number) => {
    setDistance(prev => prev + miles);
  };

  return { distance, currentPos, error, resetDistance, addDemoDistance };
}
