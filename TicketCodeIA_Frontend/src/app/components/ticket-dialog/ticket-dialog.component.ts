import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Ticket } from '../../models/ticket.model';
import { StatusBadgeComponent } from '../status-badge/status-badge.component';
import { AgentAvatarComponent } from '../agent-avatar/agent-avatar.component';
import { TicketService } from '../../services/ticket.service';

@Component({
  selector: 'app-ticket-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatDividerModule,
    MatCheckboxModule,
    MatTooltipModule,
    StatusBadgeComponent,
    AgentAvatarComponent
  ],
  templateUrl: './ticket-dialog.component.html',
  styleUrls: ['./ticket-dialog.component.css']
})
export class TicketDialogComponent {
  readonly data = inject<Ticket>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<TicketDialogComponent>);
  private readonly ticketService = inject(TicketService);

  processing = signal(false);
  deleting = signal(false);
  enableCodeReview = this.data.enableCodeReview;
  enableTesting = this.data.enableTesting;

  isEditable(): boolean {
    return this.data.status === 'TODO';
  }

  pipelineSummary(): string {
    const editNote = this.isEditable() ? '' : ' (read-only)';
    if (this.enableCodeReview && this.enableTesting) return 'Pipeline: Dev → Code Review → Testing → Done' + editNote;
    if (this.enableCodeReview) return 'Pipeline: Dev → Code Review → Done (testing skipped)' + editNote;
    if (this.enableTesting) return 'Pipeline: Dev → Testing → Done (review skipped)' + editNote;
    return 'Pipeline: Dev → Done (review and testing skipped)' + editNote;
  }

  deleteTicket(): void {
    this.deleting.set(true);
    this.ticketService.deleteTicket(this.data.id).subscribe({
      next: () => {
        this.deleting.set(false);
        this.dialogRef.close({ deleted: true });
      },
      error: (err) => {
        console.error('Error deleting ticket:', err);
        this.deleting.set(false);
      }
    });
  }

  processTicket(): void {
    this.processing.set(true);
    this.ticketService.processTicket(this.data.id, this.enableCodeReview, this.enableTesting).subscribe({
      next: () => {
        this.processing.set(false);
        this.dialogRef.close({ processed: true });
      },
      error: (err) => {
        console.error('Error processing ticket:', err);
        this.processing.set(false);
      }
    });
  }
}
