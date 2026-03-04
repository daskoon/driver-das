export interface Shift {
  id: string;
  startTime: number;
  endTime: number;
  totalMiles: number;
  taxDeduction: number;
}

export const TAX_RATE_PER_MILE = 0.67; // 2024 IRS Standard Rate
