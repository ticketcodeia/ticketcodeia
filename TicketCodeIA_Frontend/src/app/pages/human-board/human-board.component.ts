import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { TicketService } from '../../services/ticket.service';
import { ProjectService } from '../../services/project.service';
import { SseService } from '../../services/sse.service';
import { Ticket, TicketStatus, SseEvent, Project } from '../../models/ticket.model';
import { AgentAvatarComponent } from '../../components/agent-avatar/agent-avatar.component';
import { TicketDialogComponent } from '../../components/ticket-dialog/ticket-dialog.component';

interface HumanColumn {
  status: TicketStatus;
  title: string;
  color: string;
  nextStatus: TicketStatus | null;
  nextLabel: string;
}

@Component({
  selector: 'app-human-board',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatSelectModule,
    MatSnackBarModule,
    MatDialogModule,
    AgentAvatarComponent
  ],
  templateUrl: './human-board.component.html',
  styleUrls: ['./human-board.component.css']
})
export class HumanBoardComponent implements OnInit, OnDestroy {
  private readonly ticketService = inject(TicketService);
  private readonly projectService = inject(ProjectService);
  private readonly sseService = inject(SseService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);

  tickets = signal<Ticket[]>([]);
  projects = signal<Project[]>([]);
  selectedProjectId: number | null = null;

  columns: HumanColumn[] = [
    { status: TicketStatus.HUMAN_TODO, title: 'Human Todo', color: '#283593', nextStatus: TicketStatus.HUMAN_DEV, nextLabel: 'Start Development' },
    { status: TicketStatus.HUMAN_DEV, title: 'Human Dev', color: '#ad1457', nextStatus: TicketStatus.HUMAN_REVIEW, nextLabel: 'Complete Dev' },
    { status: TicketStatus.HUMAN_REVIEW, title: 'Human Review', color: '#6a1b9a', nextStatus: TicketStatus.HUMAN_TESTING, nextLabel: 'Approve Review' },
    { status: TicketStatus.HUMAN_TESTING, title: 'Human Testing', color: '#00695c', nextStatus: TicketStatus.DONE, nextLabel: 'Complete Testing' }
  ];

  private unsubscribe?: () => void;

  ngOnInit(): void {
    this.loadProjects();
    this.loadTickets();
    this.sseService.connect();

    this.unsubscribe = this.sseService.subscribe((event: SseEvent) => {
      this.loadTickets();
    });
  }

  ngOnDestroy(): void {
    if (this.unsubscribe) {
      this.unsubscribe();
    }
  }

  loadProjects(): void {
    this.projectService.getAllProjects().subscribe({
      next: (projects) => this.projects.set(projects),
      error: (err) => console.error('Error loading projects:', err)
    });
  }

  onProjectChange(): void {
    this.loadTickets();
  }

  getTicketsByStatus(status: TicketStatus): Ticket[] {
    return this.tickets().filter(t => t.status === status);
  }

  advanceTicket(ticket: Ticket, targetStatus: TicketStatus): void {
    this.ticketService.advanceHumanBoardStatus(ticket.id, targetStatus).subscribe({
      next: () => {
        const label = targetStatus === TicketStatus.DONE ? 'Done' : targetStatus;
        this.snackBar.open(`Ticket #${ticket.id} moved to ${label}`, 'Close', {
          duration: 3000
        });
        this.loadTickets();
      },
      error: (err) => {
        console.error('Error advancing ticket:', err);
        this.snackBar.open('Error advancing ticket', 'Close', {
          duration: 3000
        });
      }
    });
  }

  openTicketDialog(ticket: Ticket): void {
    this.dialog.open(TicketDialogComponent, {
      width: '600px',
      data: ticket
    });
  }

  private loadTickets(): void {
    this.ticketService.getAllTickets(this.selectedProjectId ?? undefined).subscribe({
      next: (tickets) => {
        const humanTickets = tickets.filter(t =>
          t.status === TicketStatus.HUMAN_TODO ||
          t.status === TicketStatus.HUMAN_DEV ||
          t.status === TicketStatus.HUMAN_REVIEW ||
          t.status === TicketStatus.HUMAN_TESTING
        );
        this.tickets.set(humanTickets);
      },
      error: (err) => console.error('Error loading tickets:', err)
    });
  }
}
