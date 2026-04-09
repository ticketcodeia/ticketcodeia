import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { PLATFORM_ID } from '@angular/core';
import { BoardComponent } from './board.component';
import { TicketStatus, Priority } from '../../models/ticket.model';
import type { Ticket } from '../../models/ticket.model';

describe('BoardComponent', () => {
  let fixture: ComponentFixture<BoardComponent>;
  let component: BoardComponent;
  let httpTesting: HttpTestingController;

  const mockTickets: Ticket[] = [
    {
      id: 1, title: 'Task 1', description: 'Desc', status: TicketStatus.TODO,
      priority: Priority.HIGH, assignedAgent: null, agentLogs: [], branchName: null,
      enableCodeReview: true, enableTesting: true, projectId: null, projectName: null,
      createdAt: '2026-01-01T00:00:00', updatedAt: '2026-01-01T00:00:00'
    },
    {
      id: 2, title: 'Task 2', description: 'Desc 2', status: TicketStatus.IN_PROGRESS,
      priority: Priority.MEDIUM, assignedAgent: null, agentLogs: [], branchName: null,
      enableCodeReview: false, enableTesting: false, projectId: null, projectName: null,
      createdAt: '2026-01-01T00:00:00', updatedAt: '2026-01-01T00:00:00'
    }
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BoardComponent],
      providers: [
        provideHttpClient(), provideHttpClientTesting(), provideNoopAnimations(),
        { provide: PLATFORM_ID, useValue: 'server' }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(BoardComponent);
    component = fixture.componentInstance;
    httpTesting = TestBed.inject(HttpTestingController);
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have 5 columns', () => {
    expect(component.columns.length).toBe(5);
    expect(component.columns[0].title).toBe('To Do');
    expect(component.columns[4].title).toBe('Done');
  });

  it('should load tickets and projects on init', () => {
    fixture.detectChanges();

    httpTesting.expectOne('http://localhost:8080/api/projects').flush([]);
    httpTesting.expectOne('http://localhost:8080/api/tickets').flush(mockTickets);

    expect(component.tickets().length).toBe(2);
  });

  it('should filter tickets by status', () => {
    component.tickets.set(mockTickets);
    expect(component.getTicketsByStatus(TicketStatus.TODO).length).toBe(1);
    expect(component.getTicketsByStatus(TicketStatus.IN_PROGRESS).length).toBe(1);
    expect(component.getTicketsByStatus(TicketStatus.DONE).length).toBe(0);
  });

  it('should render columns in template', () => {
    fixture.detectChanges();
    httpTesting.expectOne('http://localhost:8080/api/projects').flush([]);
    httpTesting.expectOne('http://localhost:8080/api/tickets').flush([]);
    fixture.detectChanges();

    const columns = fixture.nativeElement.querySelectorAll('.column');
    expect(columns.length).toBe(5);
  });

  it('should display board header with title', () => {
    fixture.detectChanges();
    httpTesting.expectOne('http://localhost:8080/api/projects').flush([]);
    httpTesting.expectOne('http://localhost:8080/api/tickets').flush([]);

    const h1 = fixture.nativeElement.querySelector('h1');
    expect(h1.textContent).toBe('Agent Board');
  });

  it('should reload tickets on project change', () => {
    fixture.detectChanges();
    httpTesting.expectOne('http://localhost:8080/api/projects').flush([]);
    httpTesting.expectOne('http://localhost:8080/api/tickets').flush([]);

    component.selectedProjectId = 5;
    component.onProjectChange();

    const req = httpTesting.expectOne(r => r.url === 'http://localhost:8080/api/tickets');
    expect(req.request.params.get('projectId')).toBe('5');
    req.flush([]);
  });

  it('should not allow start project without project selected', () => {
    component.selectedProjectId = null;
    expect(component.canStartProject()).toBe(false);
  });

  it('should not allow start project when no TODO tickets', () => {
    component.selectedProjectId = 1;
    component.tickets.set([]);
    expect(component.canStartProject()).toBe(false);
  });

  it('should not allow start project when active tickets exist', () => {
    component.selectedProjectId = 1;
    component.tickets.set(mockTickets); // has IN_PROGRESS
    expect(component.canStartProject()).toBe(false);
  });

  it('should allow start project with TODO tickets and no active ones', () => {
    component.selectedProjectId = 1;
    component.tickets.set([mockTickets[0]]); // only TODO
    expect(component.canStartProject()).toBe(true);
  });

  it('should call processProject on startProject', () => {
    fixture.detectChanges();
    httpTesting.expectOne('http://localhost:8080/api/projects').flush([]);
    httpTesting.expectOne('http://localhost:8080/api/tickets').flush([]);

    component.selectedProjectId = 5;
    component.startProject();

    const req = httpTesting.expectOne(r =>
      r.url.includes('process-project') && r.url.includes('projectId=5')
    );
    expect(req.request.method).toBe('POST');
    req.flush(null);

    expect(component.processingProject()).toBe(true);
  });
});
