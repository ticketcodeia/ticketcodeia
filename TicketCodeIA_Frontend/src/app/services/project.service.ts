import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Project, ProjectRequest } from '../models/ticket.model';

@Injectable({
  providedIn: 'root'
})
export class ProjectService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api';

  getAllProjects(): Observable<Project[]> {
    return this.http.get<Project[]>(`${this.apiUrl}/projects`);
  }

  createProject(request: ProjectRequest): Observable<Project> {
    return this.http.post<Project>(`${this.apiUrl}/projects`, request);
  }
}
