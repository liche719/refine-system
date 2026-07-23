import { CartesianGrid, Line, LineChart, XAxis } from 'recharts';
import { Card, CardContent } from '@/components/ui/card';
import {
  ChartConfig,
  ChartContainer,
  ChartTooltip,
  ChartTooltipContent,
} from '@/components/ui/chart';

interface ChartData {
  month: string;
  total: number;
  reviewed: number;
}

interface ChartLineMultipleProps {
  data?: ChartData[];
}

const chartConfig = {
  total: {
    label: '错题总数',
    color: 'var(--chart-1)',
  },
  reviewed: {
    label: '已复习',
    color: 'var(--chart-2)',
  },
} satisfies ChartConfig;

export function ChartLineMultiple({ data }: ChartLineMultipleProps) {
  const chartData = data || [];

  return (
    <Card className="h-full shadow-none">
      <CardContent className="h-full p-4">
        <ChartContainer
          config={chartConfig}
          className="h-full w-full aspect-auto"
        >
          <LineChart
            accessibilityLayer
            data={chartData}
            margin={{
              left: 12,
              right: 12,
            }}
          >
            <CartesianGrid vertical={false} />
            <XAxis
              dataKey="month"
              tickLine={false}
              axisLine={false}
              tickMargin={8}
            />
            <ChartTooltip cursor={false} content={<ChartTooltipContent />} />
            <Line
              dataKey="total"
              type="monotone"
              stroke="var(--color-total)"
              strokeWidth={2}
              dot={false}
            />
            <Line
              dataKey="reviewed"
              type="monotone"
              stroke="var(--color-reviewed)"
              strokeWidth={2}
              dot={false}
            />
          </LineChart>
        </ChartContainer>
      </CardContent>
    </Card>
  );
}
