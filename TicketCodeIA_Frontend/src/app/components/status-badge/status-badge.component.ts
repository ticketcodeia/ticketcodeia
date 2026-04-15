import { Component, input, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TicketStatus, AgentType } from '../../models/ticket.model';

@Component({
  selector: 'app-status-badge',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './status-badge.component.html',
  styleUrls: ['./status-badge.component.css']
})
export class StatusBadgeComponent {
  status = input.required<TicketStatus>();
  assignedAgent = input<AgentType | null>(null);

  statusClass = computed(() => {
    const statusMap: Record<TicketStatus, string> = {
      [TicketStatus.TODO]: 'todo',
      [TicketStatus.IN_PROGRESS]: 'in-progress',
      [TicketStatus.CODE_REVIEW]: 'code-review',
      [TicketStatus.TESTING]: 'testing',
      [TicketStatus.DONE]: 'done',
      [TicketStatus.ESCALATED]: 'escalated',
      [TicketStatus.HUMAN_TODO]: 'human-todo',
      [TicketStatus.HUMAN_DEV]: 'human-dev',
      [TicketStatus.HUMAN_REVIEW]: 'human-review',
      [TicketStatus.HUMAN_TESTING]: 'human-testing'
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
      [TicketStatus.ESCALATED]: 'Escalated',
      [TicketStatus.HUMAN_TODO]: 'Human Todo',
      [TicketStatus.HUMAN_DEV]: 'Human Dev',
      [TicketStatus.HUMAN_REVIEW]: 'Human Review',
      [TicketStatus.HUMAN_TESTING]: 'Human Testing'
    };
    return labelMap[this.status()] || this.status();
  });

  /** Show animated agent icon when an AI agent is actively working */
  agentWorking = computed(() => {
    const s = this.status();
    const a = this.assignedAgent();
    if (s === TicketStatus.IN_PROGRESS && a === AgentType.DEVELOPER) return { emoji: '🤖', label: 'Coding...' };
    if (s === TicketStatus.CODE_REVIEW && a === AgentType.REVIEWER) return { emoji: '🔎', label: 'Reviewing...' };
    if (s === TicketStatus.TESTING && a === AgentType.TESTER) return { emoji: '🧪', label: 'Testing...' };
    return null;
  });
}
