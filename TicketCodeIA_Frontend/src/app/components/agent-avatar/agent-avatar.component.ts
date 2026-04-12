import { Component, input, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AgentType } from '../../models/ticket.model';

@Component({
  selector: 'app-agent-avatar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './agent-avatar.component.html',
  styleUrls: ['./agent-avatar.component.css']
})
export class AgentAvatarComponent {
  agent = input<AgentType | null>(null);

  agentEmoji = computed(() => {
    if (!this.agent()) return '❓';

    const emojiMap: Record<AgentType, string> = {
      [AgentType.PO]: '📋',
      [AgentType.DEVELOPER]: '💻',
      [AgentType.REVIEWER]: '🔍',
      [AgentType.TESTER]: '🧪',
      [AgentType.HUMAN]: '👤',
      [AgentType.EXPERT]: '🧠'
    };
    return emojiMap[this.agent()!] || '❓';
  });

  agentLabel = computed(() => {
    if (!this.agent()) return 'Unassigned';

    const labelMap: Record<AgentType, string> = {
      [AgentType.PO]: 'Product Owner',
      [AgentType.DEVELOPER]: 'Developer',
      [AgentType.REVIEWER]: 'Reviewer',
      [AgentType.TESTER]: 'Tester',
      [AgentType.HUMAN]: 'Human',
      [AgentType.EXPERT]: 'Expert'
    };
    return labelMap[this.agent()!] || 'Unknown';
  });
}
