import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { TicketService } from '../../services/ticket.service';
import { SseService } from '../../services/sse.service';
import { Ticket, TicketStatus, SseEvent } from '../../models/ticket.model';
import { StatusBadgeComponent } from '../../components/status-badge/status-badge.component';
import { AgentAvatarComponent } from '../../components/agent-avatar/agent-avatar.component';
import { TicketDialogComponent } from '../../components/ticket-dialog/ticket-dialog.component';

@Component({
  selector: 'app-escalations',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    MatFormFieldModule,
    MatSnackBarModule,
    MatDialogModule,
    StatusBadgeComponent,
    AgentAvatarComponent
  ],
  templateUrl: './escalations.component.html',
  styleUrls: ['./escalations.component.css']
})
export class EscalationsComponent implements OnInit, OnDestroy {
  private readonly ticketService = inject(TicketService);
  private readonly sseService = inject(SseService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);

  escalatedTickets = signal<Ticket[]>([]);

  private unsubscribe?: () => void;

  ngOnInit(): void {
    this.loadEscalatedTickets();
    this.sseService.connect();

    this.unsubscribe = this.sseService.subscribe((event: SseEvent) => {
      this.loadEscalatedTickets();
    });
  }

  ngOnDestroy(): void {
    if (this.unsubscribe) {
      this.unsubscribe();
    }
  }

  getEscalationReason(ticket: Ticket): string {
    if (!ticket.agentLogs || ticket.agentLogs.length === 0) {
      return 'No reason provided';
    }

    const escalationLog = ticket.agentLogs.find(log =>
      log.toLowerCase().includes('escalat') ||
      log.toLowerCase().includes('failed') ||
      log.toLowerCase().includes('error')
    );

    return escalationLog || ticket.agentLogs[ticket.agentLogs.length - 1];
  }

  viewDetails(ticket: Ticket): void {
    this.dialog.open(TicketDialogComponent, {
      width: '600px',
      data: ticket
    });
  }

  moveToHumanBoard(ticket: Ticket): void {
    this.ticketService.moveToHumanBoard(ticket.id).subscribe({
      next: () => {
        this.snackBar.open(`Ticket #${ticket.id} moved to Human Board`, 'Close', {
          duration: 3000
        });
        this.loadEscalatedTickets();
      },
      error: (err) => {
        console.error('Error moving ticket to human board:', err);
        this.snackBar.open('Error moving ticket to human board', 'Close', {
          duration: 3000
        });
      }
    });
  }

  resolveTicket(ticket: Ticket, newStatus: string): void {
    this.ticketService.updateTicket(ticket.id, {
      title: ticket.title,
      description: ticket.description,
      priority: ticket.priority,
      status: newStatus as TicketStatus
    }).subscribe({
      next: () => {
        this.snackBar.open(`Ticket #${ticket.id} moved to ${newStatus}`, 'Close', {
          duration: 3000
        });
        this.loadEscalatedTickets();
      },
      error: (err) => {
        console.error('Error resolving ticket:', err);
        this.snackBar.open('Error resolving ticket', 'Close', {
          duration: 3000
        });
      }
    });
  }

  private loadEscalatedTickets(): void {
    this.ticketService.getAllTickets(undefined, TicketStatus.ESCALATED).subscribe({
      next: (tickets) => this.escalatedTickets.set(tickets),
      error: (err) => console.error('Error loading escalated tickets:', err)
    });
  }
}
