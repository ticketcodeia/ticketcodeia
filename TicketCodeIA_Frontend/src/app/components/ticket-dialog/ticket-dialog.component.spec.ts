import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { TicketDialogComponent } from './ticket-dialog.component';
import { TicketStatus, Priority, AgentType } from '../../models/ticket.model';
import type { Ticket } from '../../models/ticket.model';

describe('TicketDialogComponent', () => {
  let fixture: ComponentFixture<TicketDialogComponent>;
  let component: TicketDialogComponent;

  const mockTicket: Ticket = {
    id: 1,
    title: 'Test Ticket',
    description: 'A description',
    status: TicketStatus.TODO,
    priority: Priority.HIGH,
    assignedAgent: AgentType.DEVELOPER,
    agentLogs: ['Log 1', 'Log 2'],
    branchName: 'feature/ticket-1',
    enableCodeReview: true,
    enableTesting: false,
    projectId: 1,
    projectName: 'MyProject',
    createdAt: '2026-01-01T00:00:00',
    updatedAt: '2026-01-01T00:00:00'
  };

  const mockDialogRef = { close: vi.fn() };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TicketDialogComponent],
      providers: [
        provideHttpClient(),
        provideNoopAnimations(),
        { provide: MAT_DIALOG_DATA, useValue: mockTicket },
        { provide: MatDialogRef, useValue: mockDialogRef }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(TicketDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display ticket title and id', () => {
    const title = fixture.nativeElement.querySelector('.title-row span');
    expect(title.textContent).toContain('#1');
    expect(title.textContent).toContain('Test Ticket');
  });

  it('should display description', () => {
    const desc = fixture.nativeElement.querySelector('.section p');
    expect(desc.textContent).toBe('A description');
  });

  it('should display priority', () => {
    const priority = fixture.nativeElement.querySelector('.priority');
    expect(priority.textContent.trim()).toBe('HIGH');
  });

  it('should display agent logs', () => {
    const logs = fixture.nativeElement.querySelectorAll('.logs-list li');
    expect(logs.length).toBe(2);
    expect(logs[0].textContent).toBe('Log 1');
  });

  it('should initialize checkbox values from ticket data', () => {
    expect(component.enableCodeReview).toBe(true);
    expect(component.enableTesting).toBe(false);
  });

  it('should be editable when status is TODO', () => {
    expect(component.isEditable()).toBe(true);
  });

  it('should show pipeline summary', () => {
    const summary = fixture.nativeElement.querySelector('.pipeline-summary');
    expect(summary.textContent).toContain('Pipeline:');
  });

  it('should show process button for non-final tickets', () => {
    const button = fixture.nativeElement.querySelector('button[color="primary"]');
    expect(button.textContent.trim()).toContain('Start Agent Pipeline');
  });
});

describe('TicketDialogComponent (DONE ticket)', () => {
  const doneTicket: Ticket = {
    id: 2, title: 'Done Ticket', description: '', status: TicketStatus.DONE,
    priority: Priority.LOW, assignedAgent: null, agentLogs: [], branchName: null,
    enableCodeReview: false, enableTesting: false, projectId: null, projectName: null,
    createdAt: '2026-01-01T00:00:00', updatedAt: '2026-01-01T00:00:00'
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TicketDialogComponent],
      providers: [
        provideHttpClient(),
        provideNoopAnimations(),
        { provide: MAT_DIALOG_DATA, useValue: doneTicket },
        { provide: MatDialogRef, useValue: { close: vi.fn() } }
      ]
    }).compileComponents();
  });

  it('should not be editable when status is DONE', () => {
    const fixture = TestBed.createComponent(TicketDialogComponent);
    const component = fixture.componentInstance;
    expect(component.isEditable()).toBe(false);
  });

  it('should not show process button for DONE ticket', () => {
    const fixture = TestBed.createComponent(TicketDialogComponent);
    fixture.detectChanges();
    const buttons = fixture.nativeElement.querySelectorAll('button[color="primary"]');
    expect(buttons.length).toBe(0);
  });
});
