import { Component, OnInit, OnDestroy, inject, signal, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDividerModule } from '@angular/material/divider';
import { MatTabsModule } from '@angular/material/tabs';
import { TicketService } from '../../services/ticket.service';
import { ProjectService } from '../../services/project.service';
import { Ticket, Project, Priority, ChatMessage } from '../../models/ticket.model';
import { StatusBadgeComponent } from '../../components/status-badge/status-badge.component';

@Component({
  selector: 'app-requirements',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatDividerModule,
    MatTabsModule,
    StatusBadgeComponent
  ],
  templateUrl: './requirements.component.html',
  styleUrls: ['./requirements.component.css']
})
export class RequirementsComponent implements OnInit, OnDestroy {
  private readonly ticketService = inject(TicketService);
  private readonly projectService = inject(ProjectService);
  private readonly snackBar = inject(MatSnackBar);

  @ViewChild('chatMessagesContainer') chatMessagesContainer!: ElementRef;

  projects = signal<Project[]>([]);
  selectedProjectId: number | null = null;

  showNewProject = false;
  newProjectName = '';
  newProjectDescription = '';
  creatingProject = signal(false);

  activeTab = 0;

  // Manual creation
  generatedTickets = signal<Ticket[]>([]);
  manualTitle = '';
  manualDescription = '';
  manualPriority: Priority = Priority.MEDIUM;
  creatingTicket = signal(false);

  // Expert Agent chat
  chatMessages = signal<ChatMessage[]>([]);
  chatInput = '';
  chatLoading = signal(false);
  chatSessionId = this.generateSessionId();
  chatHistoryLoading = signal(false);

  ngOnInit(): void {
    this.loadProjects();
  }

  ngOnDestroy(): void {
    // Don't clear session on destroy — history is persisted in DB
  }

  loadProjects(): void {
    this.projectService.getAllProjects().subscribe({
      next: (projects) => this.projects.set(projects),
      error: (err) => console.error('Error loading projects:', err)
    });
  }

  onProjectChange(): void {
    if (this.selectedProjectId) {
      this.loadChatHistory(this.selectedProjectId);
    } else {
      // No project selected — start fresh
      this.chatMessages.set([]);
      this.chatSessionId = this.generateSessionId();
    }
  }

  private loadChatHistory(projectId: number): void {
    this.chatHistoryLoading.set(true);
    this.ticketService.getProjectChatHistory(projectId).subscribe({
      next: (messages) => {
        if (messages.length > 0) {
          // Restore session ID from existing history
          this.chatSessionId = messages[0].sessionId;
          this.chatMessages.set(messages.map(m => ({
            role: m.role,
            content: m.content,
            timestamp: new Date(m.createdAt)
          })));
          this.scrollChatToBottom();
        } else {
          // No history for this project — start fresh
          this.chatMessages.set([]);
          this.chatSessionId = this.generateSessionId();
        }
        this.chatHistoryLoading.set(false);
      },
      error: (err) => {
        console.error('Error loading chat history:', err);
        this.chatHistoryLoading.set(false);
      }
    });
  }

  createProject(): void {
    if (!this.newProjectName.trim()) return;
    this.creatingProject.set(true);
    this.projectService.createProject({
      name: this.newProjectName.trim(),
      description: this.newProjectDescription.trim()
    }).subscribe({
      next: (project) => {
        this.projects.update(list => [...list, project]);
        this.selectedProjectId = project.id;
        this.newProjectName = '';
        this.newProjectDescription = '';
        this.showNewProject = false;
        this.creatingProject.set(false);
        this.chatMessages.set([]);
        this.chatSessionId = this.generateSessionId();
        this.snackBar.open(`Project "${project.name}" created!`, 'Close', { duration: 3000 });
      },
      error: (err) => {
        console.error('Error creating project:', err);
        this.creatingProject.set(false);
        this.snackBar.open('Error creating project', 'Close', { duration: 3000 });
      }
    });
  }

  createManualTicket(): void {
    if (!this.manualTitle.trim()) return;

    this.creatingTicket.set(true);
    this.ticketService.createTicket({
      title: this.manualTitle.trim(),
      description: this.manualDescription.trim(),
      priority: this.manualPriority,
      projectId: this.selectedProjectId
    }).subscribe({
      next: (ticket) => {
        this.generatedTickets.update(list => [...list, ticket]);
        this.manualTitle = '';
        this.manualDescription = '';
        this.manualPriority = Priority.MEDIUM;
        this.creatingTicket.set(false);
        this.snackBar.open('Ticket created!', 'Close', { duration: 3000 });
      },
      error: (err) => {
        console.error('Error creating ticket:', err);
        this.creatingTicket.set(false);
        this.snackBar.open('Error creating ticket', 'Close', { duration: 5000 });
      }
    });
  }

  // Expert Agent chat methods
  sendChatMessage(): void {
    const message = this.chatInput.trim();
    if (!message || this.chatLoading()) return;

    this.chatMessages.update(msgs => [...msgs, {
      role: 'user' as const,
      content: message,
      timestamp: new Date()
    }]);
    this.chatInput = '';
    this.chatLoading.set(true);
    this.scrollChatToBottom();

    this.ticketService.expertChat({
      sessionId: this.chatSessionId,
      message: message,
      projectId: this.selectedProjectId
    }).subscribe({
      next: (response) => {
        this.chatMessages.update(msgs => [...msgs, {
          role: 'assistant' as const,
          content: response.message,
          timestamp: new Date()
        }]);
        this.chatLoading.set(false);
        this.scrollChatToBottom();
      },
      error: (err) => {
        console.error('Expert chat error:', err);
        this.chatMessages.update(msgs => [...msgs, {
          role: 'assistant' as const,
          content: 'Sorry, an error occurred. Please try again.',
          timestamp: new Date()
        }]);
        this.chatLoading.set(false);
        this.scrollChatToBottom();
      }
    });
  }

  clearChat(): void {
    this.ticketService.clearExpertSession(this.chatSessionId).subscribe();
    this.chatMessages.set([]);
    this.chatSessionId = this.generateSessionId();
  }

  onChatKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendChatMessage();
    }
  }

  private scrollChatToBottom(): void {
    setTimeout(() => {
      if (this.chatMessagesContainer) {
        const el = this.chatMessagesContainer.nativeElement;
        el.scrollTop = el.scrollHeight;
      }
    }, 50);
  }

  private generateSessionId(): string {
    return 'expert-' + Date.now() + '-' + Math.random().toString(36).substring(2, 9);
  }
}
