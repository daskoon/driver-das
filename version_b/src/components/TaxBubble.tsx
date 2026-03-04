import React from 'react';
import { motion } from 'motion/react';
import { DollarSign, Navigation } from 'lucide-react';

interface TaxBubbleProps {
  distance: number;
  taxDeduction: number;
  isTracking: boolean;
}

export function TaxBubble({ distance, taxDeduction, isTracking }: TaxBubbleProps) {
  if (!isTracking && distance === 0) return null;

  return (
    <motion.div
      drag
      dragMomentum={false}
      className="fixed z-50 top-20 right-4 cursor-grab active:cursor-grabbing"
      initial={{ opacity: 0, scale: 0.8 }}
      animate={{ opacity: 1, scale: 1 }}
      exit={{ opacity: 0, scale: 0.8 }}
    >
      <div className="bg-black/60 backdrop-blur-xl border border-white/10 p-3 rounded-2xl shadow-2xl flex flex-col items-center gap-1 w-24">
        <div className="flex items-center gap-1 text-emerald-400 font-mono text-sm font-bold">
          <DollarSign size={14} />
          <span>{taxDeduction.toFixed(2)}</span>
        </div>
        <div className="flex items-center gap-1 text-gray-400 font-mono text-xs">
          <Navigation size={10} />
          <span>{distance.toFixed(2)} mi</span>
        </div>
        {isTracking && (
          <div className="absolute -top-1 -right-1 w-3 h-3 bg-red-500 rounded-full animate-pulse border-2 border-black" />
        )}
      </div>
    </motion.div>
  );
}
