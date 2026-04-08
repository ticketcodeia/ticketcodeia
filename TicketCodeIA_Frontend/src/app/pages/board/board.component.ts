import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { TicketService } from '../../services/ticket.service';
import { ProjectService } from '../../services/project.service';
import { SseService } from '../../services/sse.service';
import { Ticket, TicketStatus, SseEvent, Project } from '../../models/ticket.model';
import { StatusBadgeComponent } from '../../components/status-badge/status-badge.component';
import { AgentAvatarComponent } from '../../components/agent-avatar/agent-avatar.component';
import { TicketDialogComponent } from '../../components/ticket-dialog/ticket-dialog.component';

interface Column {
  status: TicketStatus;
  title: string;
  color: string;
}

@Component({
  selector: 'app-board',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    MatFormFieldModule,
    MatSelectModule,
    StatusBadgeComponent,
    AgentAvatarComponent
  ],
  templateUrl: './board.component.html',
  styleUrls: ['./board.component.css']
})
export class BoardComponent implements OnInit, OnDestroy {
  private readonly ticketService = inject(TicketService);
  private readonly projectService = inject(ProjectService);
  private readonly sseService = inject(SseService);
  private readonly dialog = inject(MatDialog);

  tickets = signal<Ticket[]>([]);
  projects = signal<Project[]>([]);
  selectedProjectId: number | null = null;

  columns: Column[] = [
    { status: TicketStatus.TODO, title: 'To Do', color: '#1565c0' },
    { status: TicketStatus.IN_PROGRESS, title: 'In Progress', color: '#e65100' },
    { status: TicketStatus.CODE_REVIEW, title: 'Code Review', color: '#7b1fa2' },
    { status: TicketStatus.TESTING, title: 'Testing', color: '#2e7d32' },
    { status: TicketStatus.DONE, title: 'Done', color: '#00695c' },
    { status: TicketStatus.ESCALATED, title: 'Escalated', color: '#c62828' }
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

  openTicketDialog(ticket: Ticket): void {
    const dialogRef = this.dialog.open(TicketDialogComponent, {
      width: '600px',
      data: ticket
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result?.processed) {
        this.loadTickets();
      }
    });
  }

  private loadTickets(): void {
    this.ticketService.getAllTickets(this.selectedProjectId ?? undefined).subscribe({
      next: (tickets) => this.tickets.set(tickets),
      error: (err) => console.error('Error loading tickets:', err)
    });
  }
}
