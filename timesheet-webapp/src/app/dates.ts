const pad = (n: number) => String(n).padStart(2, '0');

export const toIso = (d: Date) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;

export const fromIso = (iso: string) => {
  const [y, m, d] = iso.split('-').map(Number);
  return new Date(y, m - 1, d);
};

export const todayIso = () => toIso(new Date());

export const addDays = (d: Date, n: number) => new Date(d.getFullYear(), d.getMonth(), d.getDate() + n);

/** Weeks run Monday to Sunday, matching the backend. */
export const mondayOf = (d: Date) => addDays(d, -((d.getDay() + 6) % 7));

const monthName = new Intl.DateTimeFormat('en-US', { month: 'short' });
const weekdayName = new Intl.DateTimeFormat('en-US', { weekday: 'short' });

export function formatDay(iso: string) {
  const d = fromIso(iso);
  return { weekday: weekdayName.format(d), day: d.getDate() };
}

/** "Oct 5 – 11, 2026" or "Sep 28 – Oct 4, 2026". */
export function formatRange(startIso: string): string {
  const start = fromIso(startIso);
  const end = addDays(start, 6);
  const from = `${monthName.format(start)} ${start.getDate()}`;
  const to = start.getMonth() === end.getMonth() ? `${end.getDate()}` : `${monthName.format(end)} ${end.getDate()}`;
  return `${from} – ${to}, ${end.getFullYear()}`;
}

/** 165 -> "2.75", 90 -> "1.5", 120 -> "2". */
export const hoursText = (minutes: number) => String(Number((minutes / 60).toFixed(2)));