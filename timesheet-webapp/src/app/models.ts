export type Role = 'USER' | 'ADMIN';
export type Status = 'OPEN' | 'SUBMITTED';

export interface User {
  id: number;
  name: string;
  email: string;
  role: Role;
}

export interface Task {
  id: number;
  name: string;
  billable: boolean;
  projectCode: string;
  projectName: string;
  clientName: string;
}

export interface Entry {
  id: number;
  taskId: number;
  taskName: string;
  projectCode: string;
  projectName: string;
  workDate: string; // 2026-10-07
  minutes: number;
  note: string | null;
}

export interface Week {
  weekStart: string;
  status: Status;
  submittedAt: string | null;
  totalMinutes: number;
  entries: Entry[];
}

export interface EntryInput {
  taskId: number;
  workDate: string;
  minutes: number;
  note: string | null;
}

export interface WeekTotal {
  userId: number;
  userName: string;
  weekStart: string;
  status: Status;
  totalMinutes: number;
}