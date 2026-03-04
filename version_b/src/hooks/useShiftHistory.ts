import { useState, useEffect } from 'react';
import { Shift } from '../types';

export function useShiftHistory() {
  const [shifts, setShifts] = useState<Shift[]>([]);

  useEffect(() => {
    const stored = localStorage.getItem('driver_das_shifts');
    if (stored) {
      try {
        setShifts(JSON.parse(stored));
      } catch (e) {
        console.error("Failed to parse shifts", e);
      }
    }
  }, []);

  const addShift = (shift: Shift) => {
    const updated = [shift, ...shifts];
    setShifts(updated);
    localStorage.setItem('driver_das_shifts', JSON.stringify(updated));
  };

  const clearHistory = () => {
    setShifts([]);
    localStorage.removeItem('driver_das_shifts');
  };

  return { shifts, addShift, clearHistory };
}
