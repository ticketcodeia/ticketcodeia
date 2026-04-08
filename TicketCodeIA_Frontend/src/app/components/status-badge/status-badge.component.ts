import { Component, input, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TicketStatus } from '../../models/ticket.model';

@Component({
  selector: 'app-status-badge',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './status-badge.component.html',
  styleUrls: ['./status-badge.component.css']
})
export class StatusBadgeComponent {
  status = input.required<TicketStatus>();

  statusClass = computed(() => {
    const statusMap: Record<TicketStatus, string> = {
      [TicketStatus.TODO]: 'todo',
      [TicketStatus.IN_PROGRESS]: 'in-progress',
      [TicketStatus.CODE_REVIEW]: 'code-review',
      [TicketStatus.TESTING]: 'testing',
      [TicketStatus.DONE]: 'done',
      [TicketStatus.ESCALATED]: 'escalated'
    };
    return statusMap[this.status()] || 'todo';
  });

  statusLabel = computed(() => {
    const labelMap: Record<TicketStatus, string> = {
      [TicketStatus.TODO]: 'To Do',
      [TicketStatus.IN_PROGRESS]: 'In Progress',
      [TicketStatus.CODE_REVIEW]: 'Code Review',
      [TicketStatus.TESTING]: 'Testing',
      [TicketStatus.DONE]: 'Done',
      [TicketStatus.ESCALATED]: 'Escalated'
    };
    return labelMap[this.status()] || this.status();
  });
}
