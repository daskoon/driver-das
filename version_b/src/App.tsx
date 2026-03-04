import React, { useState, useEffect } from 'react';
import { motion } from 'motion/react';
import { Play, Square, AlertCircle, Zap } from 'lucide-react';
import { useLocationTracking } from './hooks/useLocationTracking';
import { useShiftHistory } from './hooks/useShiftHistory';
import { TaxBubble } from './components/TaxBubble';
import { ShiftHistory } from './components/ShiftHistory';
import { TAX_RATE_PER_MILE } from './types';

export default function App() {
  const [isTracking, setIsTracking] = useState(false);
  const [shiftStartTime, setShiftStartTime] = useState<number | null>(null);
  
  const { distance, currentPos, error, resetDistance, addDemoDistance } = useLocationTracking(isTracking);
  const { shifts, addShift, clearHistory } = useShiftHistory();

  const taxDeduction = distance * TAX_RATE_PER_MILE;

  const startDelivery = () => {
    if (isTracking) return;
    resetDistance();
    setShiftStartTime(Date.now());
    setIsTracking(true);
  };

  const endDelivery = () => {
    if (!isTracking) return;
    if (shiftStartTime && distance > 0) {
      addShift({
        id: crypto.randomUUID(),
        startTime: shiftStartTime,
        endTime: Date.now(),
        totalMiles: distance,
        taxDeduction: taxDeduction
      });
    }
    setIsTracking(false);
    setShiftStartTime(null);
  };

  // Prevent accidental reloads while tracking
  useEffect(() => {
    const handleBeforeUnload = (e: BeforeUnloadEvent) => {
      if (isTracking) {
        e.preventDefault();
        e.returnValue = '';
      }
    };
    window.addEventListener('beforeunload', handleBeforeUnload);
    return () => window.removeEventListener('beforeunload', handleBeforeUnload);
  }, [isTracking]);

  return (
    <div className="min-h-screen bg-[#050505] text-white font-sans selection:bg-emerald-500/30">
      <TaxBubble distance={distance} taxDeduction={taxDeduction} isTracking={isTracking} />

      <main className="max-w-md mx-auto px-4 py-8 flex flex-col items-center">
        {/* Header */}
        <header className="w-full flex justify-between items-center mb-12">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-emerald-400 to-blue-600 flex items-center justify-center shadow-lg shadow-emerald-500/20">
              <Zap size={18} className="text-white" />
            </div>
            <h1 className="font-sans font-bold tracking-tight text-xl">Driver DAS</h1>
          </div>
          <div className="px-3 py-1 rounded-full bg-white/5 border border-white/10 text-xs font-mono text-gray-400">
            $0.67/mi
          </div>
        </header>

        {/* Main Dashboard Gauge */}
        <div className="relative w-64 h-64 flex items-center justify-center mb-10">
          {/* Outer ring */}
          <svg className="absolute inset-0 w-full h-full -rotate-90" viewBox="0 0 100 100">
            <circle
              cx="50"
              cy="50"
              r="46"
              fill="none"
              stroke="#1a1a1a"
              strokeWidth="2"
              strokeDasharray="2 4"
            />
            <motion.circle
              cx="50"
              cy="50"
              r="46"
              fill="none"
              stroke="url(#gradient)"
              strokeWidth="4"
              strokeLinecap="round"
              initial={{ pathLength: 0 }}
              animate={{ pathLength: isTracking ? (distance % 10) / 10 : 0 }}
              transition={{ duration: 0.5, ease: "easeOut" }}
            />
            <defs>
              <linearGradient id="gradient" x1="0%" y1="0%" x2="100%" y2="0%">
                <stop offset="0%" stopColor="#34d399" />
                <stop offset="100%" stopColor="#3b82f6" />
              </linearGradient>
            </defs>
          </svg>

          {/* Inner Content */}
          <div className="flex flex-col items-center text-center z-10">
            <span className="text-gray-500 text-[10px] uppercase tracking-[0.2em] font-bold mb-2">Tax Deduction</span>
            <div className="flex items-start justify-center text-emerald-400 font-mono">
              <span className="text-2xl mt-1">$</span>
              <motion.span 
                className="text-6xl font-light tracking-tighter"
                key={taxDeduction}
                initial={{ opacity: 0.8, y: -2 }}
                animate={{ opacity: 1, y: 0 }}
              >
                {taxDeduction.toFixed(2)}
              </motion.span>
            </div>
            <div className="mt-4 flex flex-col items-center">
              <span className="text-gray-500 text-[10px] uppercase tracking-[0.2em] font-bold mb-1">Distance</span>
              <span className="text-white font-mono text-xl">{distance.toFixed(2)} <span className="text-gray-500 text-sm">mi</span></span>
            </div>
          </div>
        </div>

        {/* Error State */}
        {error && (
          <div className="mb-6 w-full bg-red-500/10 border border-red-500/20 rounded-xl p-3 flex items-start gap-3 text-red-400 text-sm">
            <AlertCircle size={18} className="shrink-0 mt-0.5" />
            <p>{error}</p>
          </div>
        )}

        {/* Controls */}
        <div className="w-full flex flex-col gap-4">
          <div className="grid grid-cols-2 gap-4">
            <button
              onClick={startDelivery}
              disabled={isTracking}
              className={`relative py-4 rounded-2xl font-bold text-sm tracking-wide uppercase transition-all overflow-hidden flex items-center justify-center gap-2 ${
                isTracking 
                  ? 'bg-white/5 text-gray-500 cursor-not-allowed' 
                  : 'bg-emerald-500 text-black hover:bg-emerald-400 shadow-[0_0_30px_rgba(52,211,153,0.2)]'
              }`}
            >
              <Play size={18} fill="currentColor" />
              Start Delivery
            </button>
            
            <button
              onClick={endDelivery}
              disabled={!isTracking}
              className={`relative py-4 rounded-2xl font-bold text-sm tracking-wide uppercase transition-all overflow-hidden flex items-center justify-center gap-2 ${
                !isTracking 
                  ? 'bg-white/5 text-gray-500 cursor-not-allowed' 
                  : 'bg-red-500/10 text-red-500 border border-red-500/30 hover:bg-red-500/20 shadow-[0_0_30px_rgba(239,68,68,0.1)]'
              }`}
            >
              <Square size={18} fill="currentColor" />
              End Delivery
            </button>
          </div>

          {/* Demo button for testing since real GPS movement is hard to simulate sitting at a desk */}
          {isTracking && (
            <button
              onClick={() => addDemoDistance(0.5)}
              className="w-full py-3 rounded-xl bg-white/5 text-gray-400 text-sm font-mono border border-white/10 hover:bg-white/10 transition-colors"
            >
              + Simulate 0.5 Miles
            </button>
          )}
        </div>

        {/* GPS Status */}
        <div className="mt-6 flex items-center justify-center gap-2 text-xs font-mono">
          <div className={`w-2 h-2 rounded-full ${isTracking ? 'bg-emerald-500 animate-pulse' : 'bg-gray-600'}`} />
          <span className={isTracking ? 'text-emerald-500' : 'text-gray-600'}>
            {isTracking ? 'GPS ACTIVE - LOGGING' : 'SYSTEM IDLE'}
          </span>
        </div>

        {/* History */}
        <ShiftHistory shifts={shifts} onClear={clearHistory} />
      </main>
    </div>
  );
}
