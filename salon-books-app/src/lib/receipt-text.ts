import type { OcrLine } from '../../modules/receipt-ocr';

/**
 * Vision returns a receipt's left column ("FOILS 500CT") and right column ("15.00")
 * as separate pieces. Put pieces that sit at the same height back on one row,
 * left to right, so the server's parser sees "FOILS 500CT  15.00".
 */
export function linesToText(lines: OcrLine[]): string {
  const pieces = lines
    .filter((l) => l.text.trim())
    .map((l) => ({ ...l, cy: l.y + l.height / 2 }))
    .sort((a, b) => a.cy - b.cy);

  const rows: { cy: number; h: number; items: typeof pieces }[] = [];
  for (const p of pieces) {
    const row = rows[rows.length - 1];
    // Same row if the centers are within ~60% of a line's height (allows for a slightly tilted photo).
    if (row && Math.abs(p.cy - row.cy) < 0.6 * Math.max(row.h, p.height)) {
      row.items.push(p);
      const n = row.items.length;
      row.cy = (row.cy * (n - 1) + p.cy) / n;
      row.h = Math.max(row.h, p.height);
    } else {
      rows.push({ cy: p.cy, h: p.height, items: [p] });
    }
  }

  return rows
    .map((r) => r.items.sort((a, b) => a.x - b.x).map((i) => i.text.trim()).join('  '))
    .join('\n');
}
