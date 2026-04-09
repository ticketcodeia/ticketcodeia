import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { TicketService } from '../../services/ticket.service';
import { SseService } from '../../services/sse.service';
import { TicketStats, AgentLog, SseEvent } from '../../models/ticket.model';
import { AgentAvatarComponent } from '../../components/agent-avatar/agent-avatar.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatListModule,
    AgentAvatarComponent
  ],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit, OnDestroy {
  private readonly ticketService = inject(TicketService);
  private readonly sseService = inject(SseService);

  stats = signal<TicketStats>({
    total: 0,
    todo: 0,
    inProgress: 0,
    codeReview: 0,
    testing: 0,
    done: 0,
    escalated: 0,
    humanTodo: 0,
    humanDev: 0,
    humanReview: 0,
    humanTesting: 0
  });

  recentActivity = signal<AgentLog[]>([]);

  private unsubscribe?: () => void;

  ngOnInit(): void {
    this.loadStats();
    this.loadRecentActivity();
    this.sseService.connect();

    this.unsubscribe = this.sseService.subscribe((event: SseEvent) => {
      this.loadStats();
      this.loadRecentActivity();
    });
  }

  ngOnDestroy(): void {
    if (this.unsubscribe) {
      this.unsubscribe();
    }
  }

  private loadStats(): void {
    this.ticketService.getStats().subscribe({
      next: (stats) => this.stats.set(stats),
      error: (err) => console.error('Error loading stats:', err)
    });
  }

  private loadRecentActivity(): void {
    this.ticketService.getRecentActivity().subscribe({
      next: (activity) => this.recentActivity.set(activity),
      error: (err) => console.error('Error loading activity:', err)
    });
  }
}
