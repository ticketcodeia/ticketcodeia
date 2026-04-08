import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ProjectService } from './project.service';
import type { Project } from '../models/ticket.model';

describe('ProjectService', () => {
  let service: ProjectService;
  let httpTesting: HttpTestingController;

  const mockProject: Project = {
    id: 1,
    name: 'MyProject',
    description: 'A project',
    createdAt: '2026-01-01T00:00:00'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(ProjectService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should get all projects', () => {
    service.getAllProjects().subscribe(projects => {
      expect(projects.length).toBe(2);
      expect(projects[0].name).toBe('MyProject');
    });

    const req = httpTesting.expectOne('http://localhost:8080/api/projects');
    expect(req.request.method).toBe('GET');
    req.flush([mockProject, { ...mockProject, id: 2, name: 'Another' }]);
  });

  it('should return empty array when no projects', () => {
    service.getAllProjects().subscribe(projects => {
      expect(projects.length).toBe(0);
    });

    const req = httpTesting.expectOne('http://localhost:8080/api/projects');
    req.flush([]);
  });

  it('should create a project', () => {
    const request = { name: 'New Project', description: 'desc' };

    service.createProject(request).subscribe(project => {
      expect(project.id).toBe(1);
      expect(project.name).toBe('MyProject');
    });

    const req = httpTesting.expectOne('http://localhost:8080/api/projects');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(mockProject);
  });
});
