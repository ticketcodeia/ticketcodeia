import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TicketService } from './ticket.service';
import { TicketStatus, Priority, AgentType } from '../models/ticket.model';
import type { Ticket, TicketStats, AgentLog } from '../models/ticket.model';

describe('TicketService', () => {
  let service: TicketService;
  let httpTesting: HttpTestingController;

  const mockTicket: Ticket = {
    id: 1,
    title: 'Test Ticket',
    description: 'Description',
    status: TicketStatus.TODO,
    priority: Priority.MEDIUM,
    assignedAgent: null,
    agentLogs: [],
    branchName: null,
    enableCodeReview: false,
    enableTesting: false,
    projectId: null,
    projectName: null,
    createdAt: '2026-01-01T00:00:00',
    updatedAt: '2026-01-01T00:00:00'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(TicketService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should create a ticket', () => {
    const request = { title: 'Test', description: 'Desc' };
    service.createTicket(request).subscribe(ticket => {
      expect(ticket.id).toBe(1);
    });

    const req = httpTesting.expectOne('http://localhost:8080/api/tickets');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(mockTicket);
  });

  it('should get all tickets without params', () => {
    service.getAllTickets().subscribe(tickets => {
      expect(tickets.length).toBe(1);
    });

    const req = httpTesting.expectOne('http://localhost:8080/api/tickets');
    expect(req.request.method).toBe('GET');
    req.flush([mockTicket]);
  });

  it('should get all tickets with projectId filter', () => {
    service.getAllTickets(5).subscribe();

    const req = httpTesting.expectOne(r => r.url === 'http://localhost:8080/api/tickets');
    expect(req.request.params.get('projectId')).toBe('5');
    req.flush([]);
  });

  it('should get all tickets with status filter', () => {
    service.getAllTickets(undefined, TicketStatus.DONE).subscribe();

    const req = httpTesting.expectOne(r => r.url === 'http://localhost:8080/api/tickets');
    expect(req.request.params.get('status')).toBe('DONE');
    req.flush([]);
  });

  it('should get ticket by id', () => {
    service.getTicketById(1).subscribe(ticket => {
      expect(ticket.title).toBe('Test Ticket');
    });

    const req = httpTesting.expectOne('http://localhost:8080/api/tickets/1');
    expect(req.request.method).toBe('GET');
    req.flush(mockTicket);
  });

  it('should update a ticket', () => {
    const request = { title: 'Updated', description: 'Updated desc' };
    service.updateTicket(1, request).subscribe(ticket => {
      expect(ticket.id).toBe(1);
    });

    const req = httpTesting.expectOne('http://localhost:8080/api/tickets/1');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(request);
    req.flush(mockTicket);
  });

  it('should process a ticket with default flags', () => {
    service.processTicket(1).subscribe();

    const req = httpTesting.expectOne('http://localhost:8080/api/tickets/1/process');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ enableCodeReview: true, enableTesting: true });
    req.flush(mockTicket);
  });

  it('should process a ticket with custom flags', () => {
    service.processTicket(1, false, true).subscribe();

    const req = httpTesting.expectOne('http://localhost:8080/api/tickets/1/process');
    expect(req.request.body).toEqual({ enableCodeReview: false, enableTesting: true });
    req.flush(mockTicket);
  });

  it('should move ticket to human board', () => {
    service.moveToHumanBoard(1).subscribe(ticket => {
      expect(ticket.status).toBe(TicketStatus.HUMAN_TODO);
    });

    const req = httpTesting.expectOne('http://localhost:8080/api/tickets/1/move-to-human-board');
    expect(req.request.method).toBe('POST');
    req.flush({ ...mockTicket, status: TicketStatus.HUMAN_TODO });
  });

  it('should advance human board status', () => {
    service.advanceHumanBoardStatus(1, TicketStatus.HUMAN_DEV).subscribe(ticket => {
      expect(ticket.status).toBe(TicketStatus.HUMAN_DEV);
    });

    const req = httpTesting.expectOne(
      'http://localhost:8080/api/tickets/1/human-board-status?status=HUMAN_DEV'
    );
    expect(req.request.method).toBe('PUT');
    req.flush({ ...mockTicket, status: TicketStatus.HUMAN_DEV });
  });

  it('should get stats', () => {
    const mockStats: TicketStats = {
      total: 10, todo: 3, inProgress: 2, codeReview: 1, testing: 1, done: 2, escalated: 1,
      humanTodo: 0, humanDev: 0, humanReview: 0, humanTesting: 0
    };

    service.getStats().subscribe(stats => {
      expect(stats.total).toBe(10);
      expect(stats.done).toBe(2);
    });

    const req = httpTesting.expectOne('http://localhost:8080/api/tickets/stats');
    expect(req.request.method).toBe('GET');
    req.flush(mockStats);
  });

  it('should get ticket logs', () => {
    const mockLogs: AgentLog[] = [{
      id: 1, ticketId: 1, agentType: AgentType.DEVELOPER,
      action: 'STARTED', message: 'Working', timestamp: '2026-01-01T00:00:00'
    }];

    service.getTicketLogs(1).subscribe(logs => {
      expect(logs.length).toBe(1);
      expect(logs[0].action).toBe('STARTED');
    });

    const req = httpTesting.expectOne('http://localhost:8080/api/tickets/1/logs');
    expect(req.request.method).toBe('GET');
    req.flush(mockLogs);
  });

  it('should get recent activity', () => {
    service.getRecentActivity().subscribe(activity => {
      expect(activity.length).toBe(0);
    });

    const req = httpTesting.expectOne('http://localhost:8080/api/sse/recent-activity');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });
});
