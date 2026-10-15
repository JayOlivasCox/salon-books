import { useState } from 'react';
import { View } from 'react-native';
import Svg, { Circle, G, Line, Rect, Text as SvgText } from 'react-native-svg';

import { moneyShort } from '@/lib/format';
import { C } from '@/lib/theme';

/** Vertical bars with labels underneath. Optional ghost series drawn behind (e.g. projection). */
export function BarChart({ values, labels, height = 160, color = C.toner, ghost, highlight }: {
  values: number[]; labels: string[]; height?: number; color?: string; ghost?: number[]; highlight?: number;
}) {
  const [width, setWidth] = useState(0);
  const max = Math.max(1, ...values, ...(ghost ?? []));
  const labelH = 18;
  const chartH = height - labelH;
  const slot = width / Math.max(values.length, 1);
  const barW = Math.min(28, slot * 0.6);

  return (
    <View onLayout={(e) => setWidth(e.nativeEvent.layout.width)} style={{ height }}>
      {width > 0 && (
        <Svg width={width} height={height}>
          <Line x1={0} x2={width} y1={chartH} y2={chartH} stroke={C.line} strokeWidth={1} />
          <SvgText x={0} y={11} fontSize={11} fill={C.inkFaint}>{moneyShort(max)}</SvgText>
          {values.map((v, i) => {
            const x = i * slot + (slot - barW) / 2;
            const h = (Math.max(v, 0) / max) * (chartH - 16);
            const g = ghost ? (Math.max(ghost[i] ?? 0, 0) / max) * (chartH - 16) : 0;
            return (
              <G key={i}>
                {ghost && <Rect x={x} y={chartH - g} width={barW} height={g} rx={4} fill={C.tonerSoft} />}
                <Rect x={x} y={chartH - h} width={barW} height={h} rx={4}
                  fill={highlight === undefined || highlight === i ? color : C.chairSoft} />
                <SvgText x={i * slot + slot / 2} y={height - 4} fontSize={11} fill={C.inkSoft} textAnchor="middle">
                  {labels[i]}
                </SvgText>
              </G>
            );
          })}
        </Svg>
      )}
    </View>
  );
}

/** Ring chart. Slices are drawn as dashed strokes on one circle. */
export function Donut({ slices, size = 150, thickness = 22 }: { slices: { value: number; color: string }[]; size?: number; thickness?: number }) {
  const r = (size - thickness) / 2;
  const circ = 2 * Math.PI * r;
  const total = slices.reduce((s, x) => s + Math.max(x.value, 0), 0);
  let offset = 0;
  return (
    <Svg width={size} height={size}>
      <G rotation={-90} origin={`${size / 2}, ${size / 2}`}>
        <Circle cx={size / 2} cy={size / 2} r={r} stroke={C.line} strokeWidth={thickness} fill="none" />
        {total > 0 && slices.map((s, i) => {
          const len = (Math.max(s.value, 0) / total) * circ;
          const el = (
            <Circle key={i} cx={size / 2} cy={size / 2} r={r} fill="none" stroke={s.color} strokeWidth={thickness}
              strokeDasharray={`${len} ${circ - len}`} strokeDashoffset={-offset} />
          );
          offset += len;
          return el;
        })}
      </G>
    </Svg>
  );
}
