import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ConfigService, AgentConfigData } from '../../services/config.service';

/** Defaults matching application.properties */
const DEFAULTS: AgentConfigData = {
  workspace: 'C:/tickcode-workspace',
  cliModel: 'claude-opus-4-6',
  maxTurns: 40,
  maxRetries: 3,
  apiModel: 'claude-sonnet-4-5',
  maxTokens: 8192,
  allowedTools: 'Read,Write,Edit,Bash',
  timeoutMinutes: 5,
  dangerouslySkipPermissions: true
};

@Component({
  selector: 'app-config',
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
    MatSlideToggleModule,
    MatSnackBarModule,
    MatDividerModule,
    MatTooltipModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './config.component.html',
  styleUrls: ['./config.component.css']
})
export class ConfigComponent implements OnInit {
  private readonly configService = inject(ConfigService);
  private readonly snackBar = inject(MatSnackBar);

  loading = signal(true);
  saving = signal(false);

  // Form fields
  workspace = '';
  cliModel = '';
  maxTurns = 40;
  maxRetries = 3;
  apiModel = '';
  maxTokens = 8192;
  allowedTools = '';
  timeoutMinutes = 5;
  dangerouslySkipPermissions = true;

  // Tool chips
  allTools = ['Read', 'Write', 'Edit', 'Bash', 'Glob', 'Grep', 'WebSearch', 'WebFetch', 'NotebookEdit'];
  selectedTools: string[] = [];

  // Model options
  cliModels = [
    { value: 'claude-opus-4-6', label: 'Claude Opus 4.6' },
    { value: 'claude-sonnet-4-6', label: 'Claude Sonnet 4.6' },
    { value: 'claude-sonnet-4-5-20250514', label: 'Claude Sonnet 4.5' },
    { value: 'claude-haiku-4-5-20251001', label: 'Claude Haiku 4.5' }
  ];

  apiModels = [
    { value: 'claude-opus-4-6', label: 'Claude Opus 4.6' },
    { value: 'claude-sonnet-4-6', label: 'Claude Sonnet 4.6' },
    { value: 'claude-sonnet-4-5-20250514', label: 'Claude Sonnet 4.5' },
    { value: 'claude-haiku-4-5-20251001', label: 'Claude Haiku 4.5' }
  ];

  ngOnInit(): void {
    this.loadConfig();
  }

  loadConfig(): void {
    this.loading.set(true);
    this.configService.getAgentConfig().subscribe({
      next: (config) => this.applyConfig(config),
      error: (err) => {
        console.error('Error loading config:', err);
        this.loading.set(false);
        this.snackBar.open('Error loading configuration', 'Close', { duration: 5000 });
      }
    });
  }

  toggleTool(tool: string): void {
    const idx = this.selectedTools.indexOf(tool);
    if (idx >= 0) {
      this.selectedTools.splice(idx, 1);
    } else {
      this.selectedTools.push(tool);
    }
    this.allowedTools = this.selectedTools.join(',');
  }

  isToolSelected(tool: string): boolean {
    return this.selectedTools.includes(tool);
  }

  saveConfig(): void {
    this.saving.set(true);
    const config: AgentConfigData = {
      workspace: this.workspace,
      cliModel: this.cliModel,
      maxTurns: this.maxTurns,
      maxRetries: this.maxRetries,
      apiModel: this.apiModel,
      maxTokens: this.maxTokens,
      allowedTools: this.allowedTools,
      timeoutMinutes: this.timeoutMinutes,
      dangerouslySkipPermissions: this.dangerouslySkipPermissions
    };

    this.configService.updateAgentConfig(config).subscribe({
      next: () => {
        this.saving.set(false);
        this.snackBar.open('Configuration saved!', 'Close', { duration: 3000 });
      },
      error: (err) => {
        console.error('Error saving config:', err);
        this.saving.set(false);
        this.snackBar.open('Error saving configuration', 'Close', { duration: 5000 });
      }
    });
  }

  resetDefaults(): void {
    this.applyConfig(DEFAULTS);
    this.snackBar.open('Defaults restored (not saved yet)', 'Close', { duration: 3000 });
  }

  isDefault(field: keyof AgentConfigData): boolean {
    return (this as any)[field] === (DEFAULTS as any)[field];
  }

  getDefault(field: keyof AgentConfigData): string {
    return String(DEFAULTS[field]);
  }

  private applyConfig(config: AgentConfigData): void {
    this.workspace = config.workspace;
    this.cliModel = config.cliModel;
    this.maxTurns = config.maxTurns;
    this.maxRetries = config.maxRetries;
    this.apiModel = config.apiModel;
    this.maxTokens = config.maxTokens;
    this.allowedTools = config.allowedTools;
    this.timeoutMinutes = config.timeoutMinutes;
    this.dangerouslySkipPermissions = config.dangerouslySkipPermissions;
    this.selectedTools = config.allowedTools.split(',').map(t => t.trim()).filter(t => t);
    this.loading.set(false);
  }
}
