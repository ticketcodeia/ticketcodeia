import { Component, OnInit, inject, signal } from '@angular/core';
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
import { Ticket, Project, Priority } from '../../models/ticket.model';
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
export class RequirementsComponent implements OnInit {
  private readonly ticketService = inject(TicketService);
  private readonly projectService = inject(ProjectService);
  private readonly snackBar = inject(MatSnackBar);

  projects = signal<Project[]>([]);
  selectedProjectId: number | null = null;

  showNewProject = false;
  newProjectName = '';
  newProjectDescription = '';
  creatingProject = signal(false);

  activeTab = 0;

  // AI generation
  requirements = '';
  generating = signal(false);
  generatedTickets = signal<Ticket[]>([]);

  // Manual creation
  manualTitle = '';
  manualDescription = '';
  manualPriority: Priority = Priority.MEDIUM;
  creatingTicket = signal(false);

  ngOnInit(): void {
    this.loadProjects();
  }

  loadProjects(): void {
    this.projectService.getAllProjects().subscribe({
      next: (projects) => this.projects.set(projects),
      error: (err) => console.error('Error loading projects:', err)
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
        this.snackBar.open(`Project "${project.name}" created!`, 'Close', { duration: 3000 });
      },
      error: (err) => {
        console.error('Error creating project:', err);
        this.creatingProject.set(false);
        this.snackBar.open('Error creating project', 'Close', { duration: 3000 });
      }
    });
  }

  generateTickets(): void {
    if (!this.requirements.trim()) return;

    if (this.showNewProject && this.newProjectName.trim() && !this.selectedProjectId) {
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
          this.snackBar.open(`Project "${project.name}" created!`, 'Close', { duration: 2000 });
          this.doGenerateTickets();
        },
        error: (err) => {
          console.error('Error creating project:', err);
          this.creatingProject.set(false);
          this.snackBar.open('Error creating project', 'Close', { duration: 3000 });
        }
      });
    } else {
      this.doGenerateTickets();
    }
  }

  private doGenerateTickets(): void {
    this.generating.set(true);
    this.generatedTickets.set([]);

    this.ticketService.generateTickets(this.requirements, this.selectedProjectId).subscribe({
      next: (tickets) => {
        this.generatedTickets.set(tickets);
        this.generating.set(false);
        this.snackBar.open(`Generated ${tickets.length} tickets!`, 'Close', { duration: 3000 });
      },
      error: (err) => {
        console.error('Error generating tickets:', err);
        this.generating.set(false);
        this.snackBar.open('Error generating tickets', 'Close', { duration: 5000 });
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
}
