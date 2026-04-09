import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { PLATFORM_ID } from '@angular/core';
import { HumanBoardComponent } from './human-board.component';
import { TicketStatus, Priority, AgentType } from '../../models/ticket.model';
import type { Ticket } from '../../models/ticket.model';

describe('HumanBoardComponent', () => {
  let fixture: ComponentFixture<HumanBoardComponent>;
  let component: HumanBoardComponent;
  let httpTesting: HttpTestingController;

  const mockTickets: Ticket[] = [
    {
      id: 1, title: 'Human Task 1', description: 'Desc', status: TicketStatus.HUMAN_TODO,
      priority: Priority.HIGH, assignedAgent: AgentType.HUMAN, agentLogs: [], branchName: null,
      enableCodeReview: true, enableTesting: true, projectId: 1, projectName: 'Project A',
      createdAt: '2026-01-01T00:00:00', updatedAt: '2026-01-01T00:00:00'
    },
    {
      id: 2, title: 'Human Task 2', description: 'Desc 2', status: TicketStatus.HUMAN_DEV,
      priority: Priority.MEDIUM, assignedAgent: AgentType.HUMAN, agentLogs: [], branchName: null,
      enableCodeReview: false, enableTesting: false, projectId: null, projectName: null,
      createdAt: '2026-01-01T00:00:00', updatedAt: '2026-01-01T00:00:00'
    },
    {
      id: 3, title: 'Non-human Task', description: 'Desc 3', status: TicketStatus.TODO,
      priority: Priority.LOW, assignedAgent: null, agentLogs: [], branchName: null,
      enableCodeReview: false, enableTesting: false, projectId: null, projectName: null,
      createdAt: '2026-01-01T00:00:00', updatedAt: '2026-01-01T00:00:00'
    }
  ];

  const mockProjects = [
    { id: 1, name: 'Project A', description: 'Desc A', createdAt: '2026-01-01T00:00:00' },
    { id: 2, name: 'Project B', description: 'Desc B', createdAt: '2026-01-01T00:00:00' }
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HumanBoardComponent],
      providers: [
        provideHttpClient(), provideHttpClientTesting(), provideNoopAnimations(),
        { provide: PLATFORM_ID, useValue: 'server' }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(HumanBoardComponent);
    component = fixture.componentInstance;
    httpTesting = TestBed.inject(HttpTestingController);
  });

  function flushInit(tickets: Ticket[] = [], projects = mockProjects) {
    fixture.detectChanges();
    httpTesting.expectOne('http://localhost:8080/api/projects').flush(projects);
    httpTesting.expectOne('http://localhost:8080/api/tickets').flush(tickets);
  }

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have 4 human columns', () => {
    expect(component.columns.length).toBe(4);
    expect(component.columns[0].title).toBe('Human Todo');
    expect(component.columns[1].title).toBe('Human Dev');
    expect(component.columns[2].title).toBe('Human Review');
    expect(component.columns[3].title).toBe('Human Testing');
  });

  it('should load projects and filter only human tickets on init', () => {
    flushInit(mockTickets);

    expect(component.projects().length).toBe(2);
    expect(component.tickets().length).toBe(2);
    expect(component.tickets().every(t => t.status.startsWith('HUMAN_'))).toBe(true);
  });

  it('should filter tickets by status', () => {
    component.tickets.set(mockTickets.filter(t => t.status.startsWith('HUMAN_')));
    expect(component.getTicketsByStatus(TicketStatus.HUMAN_TODO).length).toBe(1);
    expect(component.getTicketsByStatus(TicketStatus.HUMAN_DEV).length).toBe(1);
    expect(component.getTicketsByStatus(TicketStatus.HUMAN_REVIEW).length).toBe(0);
  });

  it('should render 4 columns in template', () => {
    flushInit();
    fixture.detectChanges();

    const columns = fixture.nativeElement.querySelectorAll('.column');
    expect(columns.length).toBe(4);
  });

  it('should display board header', () => {
    flushInit();

    const h1 = fixture.nativeElement.querySelector('h1');
    expect(h1.textContent).toBe('Human Board');
  });

  it('should advance ticket status', () => {
    flushInit(mockTickets);

    const ticket = mockTickets[0];
    component.advanceTicket(ticket, TicketStatus.HUMAN_DEV);

    const req = httpTesting.expectOne(
      `http://localhost:8080/api/tickets/${ticket.id}/human-board-status?status=HUMAN_DEV`
    );
    expect(req.request.method).toBe('PUT');
    req.flush({ ...ticket, status: TicketStatus.HUMAN_DEV });

    // Should reload tickets after advance
    httpTesting.expectOne('http://localhost:8080/api/tickets').flush([]);
  });

  it('should have correct next status mappings', () => {
    expect(component.columns[0].nextStatus).toBe(TicketStatus.HUMAN_DEV);
    expect(component.columns[1].nextStatus).toBe(TicketStatus.HUMAN_REVIEW);
    expect(component.columns[2].nextStatus).toBe(TicketStatus.HUMAN_TESTING);
    expect(component.columns[3].nextStatus).toBe(TicketStatus.DONE);
  });

  it('should reload tickets on project change', () => {
    flushInit(mockTickets);

    component.selectedProjectId = 1;
    component.onProjectChange();

    const req = httpTesting.expectOne(r => r.url === 'http://localhost:8080/api/tickets');
    expect(req.request.params.get('projectId')).toBe('1');
    req.flush([mockTickets[0]]);

    expect(component.tickets().length).toBe(1);
  });
});
