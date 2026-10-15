// Porcelain and aubergine, with a violet "toner" accent. Money is the loudest thing on screen.
export const C = {
  bg: '#F4F2F7',
  surface: '#FFFFFF',
  ink: '#22172A',
  inkSoft: '#6E6377',
  inkFaint: '#A79EAE',
  line: '#E2DCE8',
  toner: '#6A3FA0',
  tonerSoft: '#ECE4F6',
  income: '#1D7A57',
  incomeSoft: '#E1F2EA',
  expense: '#B23A55',
  expenseSoft: '#F7E3E8',
  chair: '#2B1C33',
  chairRaised: '#3A2944',
  chairText: '#F6F1FA',
  chairSoft: '#B9A9C6',
};

export const S = { xs: 4, sm: 8, md: 12, lg: 16, xl: 24, xxl: 32 };

export const R = { sm: 8, md: 14, lg: 22, pill: 999 };

export const T = {
  display: { fontSize: 34, fontWeight: '800' as const, letterSpacing: -0.5 },
  title: { fontSize: 26, fontWeight: '700' as const, letterSpacing: -0.3 },
  heading: { fontSize: 18, fontWeight: '700' as const },
  body: { fontSize: 16, fontWeight: '400' as const },
  label: { fontSize: 14, fontWeight: '600' as const },
  small: { fontSize: 13, fontWeight: '400' as const },
  money: { fontVariant: ['tabular-nums' as const], fontWeight: '700' as const },
};
