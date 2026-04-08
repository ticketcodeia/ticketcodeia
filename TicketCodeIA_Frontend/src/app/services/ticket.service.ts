import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Ticket,
  TicketRequest,
  TicketStats,
  TicketStatus,
  RequirementsRequest,
  AgentLog
} from '../models/ticket.model';

@Injectable({
  providedIn: 'root'
})
export class TicketService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api';

  createTicket(request: TicketRequest): Observable<Ticket> {
    return this.http.post<Ticket>(`${this.apiUrl}/tickets`, request);
  }

  getAllTickets(projectId?: number, status?: TicketStatus): Observable<Ticket[]> {
    let params = new HttpParams();
    if (projectId) params = params.set('projectId', projectId.toString());
    if (status) params = params.set('status', status);
    return this.http.get<Ticket[]>(`${this.apiUrl}/tickets`, { params });
  }

  getTicketById(id: number): Observable<Ticket> {
    return this.http.get<Ticket>(`${this.apiUrl}/tickets/${id}`);
  }

  updateTicket(id: number, request: TicketRequest): Observable<Ticket> {
    return this.http.put<Ticket>(`${this.apiUrl}/tickets/${id}`, request);
  }

  processTicket(id: number, enableCodeReview = true, enableTesting = true): Observable<Ticket> {
    return this.http.post<Ticket>(`${this.apiUrl}/tickets/${id}/process`, {
      enableCodeReview,
      enableTesting
    });
  }

  getStats(): Observable<TicketStats> {
    return this.http.get<TicketStats>(`${this.apiUrl}/tickets/stats`);
  }

  getTicketLogs(id: number): Observable<AgentLog[]> {
    return this.http.get<AgentLog[]>(`${this.apiUrl}/tickets/${id}/logs`);
  }

  generateTickets(requirements: string, projectId: number | null): Observable<Ticket[]> {
    const request: RequirementsRequest = { requirements, projectId };
    return this.http.post<Ticket[]>(`${this.apiUrl}/agents/generate-tickets`, request);
  }

  getRecentActivity(): Observable<AgentLog[]> {
    return this.http.get<AgentLog[]>(`${this.apiUrl}/sse/recent-activity`);
  }
}
