import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Entry, EntryInput, Task, User, Week, WeekTotal } from './models';

@Injectable({ providedIn: 'root' })
export class Api {
  private readonly http = inject(HttpClient);

  me(): Observable<User> {
    return this.http.get<User>('/api/me');
  }

  /** Spring's form login expects a form-encoded body, not JSON. */
  login(email: string, password: string): Observable<unknown> {
    const body = new HttpParams().set('username', email).set('password', password);
    return this.http.post('/api/login', body.toString(), {
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    });
  }

  logout(): Observable<unknown> {
    return this.http.post('/api/logout', null);
  }

  tasks(): Observable<Task[]> {
    return this.http.get<Task[]>('/api/tasks');
  }

  week(date: string): Observable<Week> {
    return this.http.get<Week>('/api/week', { params: { date } });
  }

  submitWeek(date: string): Observable<Week> {
    return this.http.post<Week>('/api/week/submit', null, { params: { date } });
  }

  createEntry(input: EntryInput): Observable<Entry> {
    return this.http.post<Entry>('/api/entries', input);
  }

  updateEntry(id: number, input: EntryInput): Observable<Entry> {
    return this.http.put<Entry>(`/api/entries/${id}`, input);
  }

  deleteEntry(id: number): Observable<void> {
    return this.http.delete<void>(`/api/entries/${id}`);
  }

  adminWeeks(from: string, to: string): Observable<WeekTotal[]> {
    return this.http.get<WeekTotal[]>('/api/admin/weeks', { params: { from, to } });
  }

  reopenWeek(userId: number, date: string): Observable<void> {
    return this.http.post<void>(`/api/admin/users/${userId}/weeks/reopen`, null, { params: { date } });
  }

  csvUrl(from: string, to: string): string {
    return `/api/admin/weeks.csv?from=${from}&to=${to}`;
  }
}