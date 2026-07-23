import { PieChart, Pie, Cell } from 'recharts';
import { useMemo } from 'react';

import { ChartContainer } from '../ui/chart';
import { ChartTooltip, ChartTooltipContent } from '@/components/ui/chart';

interface PieDataItem {
  [key: string]: number;
}

interface ChartPieDonutProps {
  data?: PieDataItem[];
}

const COLORS = [
  'var(--chart-1)',
  'var(--chart-2)',
  'var(--chart-3)',
  'var(--chart-4)',
  'var(--chart-5)',
];

const chartConfig = {};

export function ChartPieDonut({ data }: ChartPieDonutProps) {
  const chartData = useMemo(() => {
    if (!data || data.length === 0) return [];

    return data.map((item, index) => {
      const name = Object.keys(item)[0];
      const value = item[name];

      return {
        name: name,
        value: value,
        color: COLORS[index % COLORS.length],
      };
    });
  }, [data]);
  return (
    <ChartContainer config={chartConfig} className="h-[200px]">
      <PieChart>
        <Pie
          data={chartData}
          cx="50%"
          cy="50%"
          innerRadius={40}
          outerRadius={60}
          dataKey="value"
        >
          {chartData.map((entry, index) => (
            <Cell key={`cell-${index}`} fill={entry.color} />
          ))}
        </Pie>
        <ChartTooltip content={<ChartTooltipContent />} />
      </PieChart>
    </ChartContainer>
  );
}
