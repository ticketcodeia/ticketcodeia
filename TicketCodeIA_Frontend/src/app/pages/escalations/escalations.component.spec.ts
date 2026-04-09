import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { PLATFORM_ID } from '@angular/core';
import { EscalationsComponent } from './escalations.component';
import { TicketStatus, Priority, AgentType } from '../../models/ticket.model';
import type { Ticket } from '../../models/ticket.model';

describe('EscalationsComponent', () => {
  let fixture: ComponentFixture<EscalationsComponent>;
  let component: EscalationsComponent;
  let httpTesting: HttpTestingController;

  const escalatedTicket: Ticket = {
    id: 1, title: 'Broken Ticket', description: 'Something broke',
    status: TicketStatus.ESCALATED, priority: Priority.CRITICAL,
    assignedAgent: AgentType.HUMAN, agentLogs: ['Escalated: Max retries exceeded'],
    branchName: null, enableCodeReview: true, enableTesting: true,
    projectId: null, projectName: null,
    createdAt: '2026-01-01T00:00:00', updatedAt: '2026-01-01T00:00:00'
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EscalationsComponent],
      providers: [
        provideHttpClient(), provideHttpClientTesting(), provideNoopAnimations(),
        { provide: PLATFORM_ID, useValue: 'server' }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(EscalationsComponent);
    component = fixture.componentInstance;
    httpTesting = TestBed.inject(HttpTestingController);
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load escalated tickets on init', () => {
    fixture.detectChanges();

    const req = httpTesting.expectOne(r => r.url === 'http://localhost:8080/api/tickets');
    expect(req.request.params.get('status')).toBe('ESCALATED');
    req.flush([escalatedTicket]);

    expect(component.escalatedTickets().length).toBe(1);
  });

  it('should show empty state when no escalations', () => {
    fixture.detectChanges();
    httpTesting.expectOne(r => r.url === 'http://localhost:8080/api/tickets').flush([]);
    fixture.detectChanges();

    const empty = fixture.nativeElement.querySelector('.empty-state');
    expect(empty).toBeTruthy();
    expect(empty.textContent).toContain('No Escalated Tickets');
  });

  it('should display escalated ticket cards', () => {
    fixture.detectChanges();
    httpTesting.expectOne(r => r.url === 'http://localhost:8080/api/tickets').flush([escalatedTicket]);
    fixture.detectChanges();

    const cards = fixture.nativeElement.querySelectorAll('.escalation-card');
    expect(cards.length).toBe(1);
  });

  it('should extract escalation reason from logs', () => {
    const reason = component.getEscalationReason(escalatedTicket);
    expect(reason).toContain('Escalated');
  });

  it('should return default when no agent logs', () => {
    const ticket = { ...escalatedTicket, agentLogs: [] };
    expect(component.getEscalationReason(ticket)).toBe('No reason provided');
  });

  it('should display page title', () => {
    fixture.detectChanges();
    httpTesting.expectOne(r => r.url === 'http://localhost:8080/api/tickets').flush([]);

    const h1 = fixture.nativeElement.querySelector('h1');
    expect(h1.textContent).toBe('Escalated Tickets');
  });

  it('should move ticket to human board', () => {
    fixture.detectChanges();
    httpTesting.expectOne(r => r.url === 'http://localhost:8080/api/tickets').flush([escalatedTicket]);

    component.moveToHumanBoard(escalatedTicket);

    const req = httpTesting.expectOne('http://localhost:8080/api/tickets/1/move-to-human-board');
    expect(req.request.method).toBe('POST');
    req.flush({ ...escalatedTicket, status: TicketStatus.HUMAN_TODO });

    // Should reload escalated tickets
    httpTesting.expectOne(r => r.url === 'http://localhost:8080/api/tickets').flush([]);
  });
});
