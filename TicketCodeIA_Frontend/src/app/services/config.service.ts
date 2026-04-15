import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface AgentConfigData {
  workspace: string;
  cliModel: string;
  maxTurns: number;
  maxRetries: number;
  apiModel: string;
  maxTokens: number;
  allowedTools: string;
  timeoutMinutes: number;
  dangerouslySkipPermissions: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class ConfigService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/config';

  getAgentConfig(): Observable<AgentConfigData> {
    return this.http.get<AgentConfigData>(`${this.apiUrl}/agents`);
  }

  updateAgentConfig(updates: Partial<AgentConfigData>): Observable<AgentConfigData> {
    return this.http.put<AgentConfigData>(`${this.apiUrl}/agents`, updates);
  }
}
