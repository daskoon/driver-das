import React from 'react';
import { Shift } from '../types';
import { Clock, MapPin, DollarSign, Trash2 } from 'lucide-react';

interface ShiftHistoryProps {
  shifts: Shift[];
  onClear: () => void;
}

export function ShiftHistory({ shifts, onClear }: ShiftHistoryProps) {
  if (shifts.length === 0) {
    return (
      <div className="mt-8 text-center p-8 border border-white/5 rounded-2xl bg-white/5">
        <p className="text-gray-500 font-mono text-sm">No shifts recorded yet.</p>
        <p className="text-gray-600 text-xs mt-2">Start tracking to build your history.</p>
      </div>
    );
  }

  const formatDate = (timestamp: number) => {
    return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit' }).format(new Date(timestamp));
  };

  const formatTime = (timestamp: number) => {
    return new Intl.DateTimeFormat('en-US', { hour: 'numeric', minute: '2-digit' }).format(new Date(timestamp));
  };

  return (
    <div className="mt-8 w-full max-w-md mx-auto">
      <div className="flex justify-between items-center mb-4">
        <h3 className="text-white font-sans font-semibold tracking-tight">Shift History</h3>
        <button 
          onClick={onClear}
          className="text-gray-500 hover:text-red-400 transition-colors flex items-center gap-1 text-xs uppercase tracking-wider font-semibold"
        >
          <Trash2 size={14} />
          Clear
        </button>
      </div>
      
      <div className="space-y-3">
        {shifts.map((shift) => (
          <div key={shift.id} className="bg-[#151619] border border-white/5 rounded-xl p-4 flex flex-col gap-3">
            <div className="flex justify-between items-center border-b border-white/5 pb-2">
              <div className="flex items-center gap-2 text-gray-400 text-xs font-mono">
                <Clock size={14} />
                <span>{formatDate(shift.startTime)} - {formatTime(shift.endTime)}</span>
              </div>
            </div>
            <div className="flex justify-between items-end">
              <div className="flex flex-col">
                <span className="text-gray-500 text-[10px] uppercase tracking-widest font-bold mb-1">Distance</span>
                <div className="flex items-center gap-1 text-white font-mono">
                  <MapPin size={14} className="text-blue-400" />
                  <span className="text-lg">{shift.totalMiles.toFixed(2)} mi</span>
                </div>
              </div>
              <div className="flex flex-col items-end">
                <span className="text-gray-500 text-[10px] uppercase tracking-widest font-bold mb-1">Deduction</span>
                <div className="flex items-center gap-1 text-emerald-400 font-mono font-bold">
                  <DollarSign size={14} />
                  <span className="text-lg">{shift.taxDeduction.toFixed(2)}</span>
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
