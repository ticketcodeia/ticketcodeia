import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ConfigComponent } from './config.component';

describe('ConfigComponent', () => {
  let fixture: ComponentFixture<ConfigComponent>;
  let component: ConfigComponent;
  let httpTesting: HttpTestingController;

  const mockConfig = {
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

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ConfigComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideNoopAnimations()]
    }).compileComponents();

    fixture = TestBed.createComponent(ConfigComponent);
    component = fixture.componentInstance;
    httpTesting = TestBed.inject(HttpTestingController);
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load config on init', () => {
    fixture.detectChanges();

    const req = httpTesting.expectOne('http://localhost:8080/api/config/agents');
    req.flush(mockConfig);

    expect(component.loading()).toBe(false);
    expect(component.workspace).toBe('C:/tickcode-workspace');
    expect(component.cliModel).toBe('claude-opus-4-6');
    expect(component.maxTurns).toBe(40);
    expect(component.selectedTools).toEqual(['Read', 'Write', 'Edit', 'Bash']);
  });

  it('should toggle tool selection', () => {
    component.selectedTools = ['Read', 'Write'];
    component.toggleTool('Bash');
    expect(component.selectedTools).toContain('Bash');

    component.toggleTool('Read');
    expect(component.selectedTools).not.toContain('Read');
  });

  it('should reset to defaults', () => {
    component.maxTurns = 100;
    component.maxRetries = 10;
    component.cliModel = 'claude-haiku-4-5-20251001';
    component.resetDefaults();
    expect(component.maxTurns).toBe(40);
    expect(component.maxRetries).toBe(3);
    expect(component.cliModel).toBe('claude-opus-4-6');
  });

  it('should save config', () => {
    component.workspace = 'C:/test';
    component.cliModel = 'claude-opus-4-6';
    component.maxTurns = 50;
    component.maxRetries = 5;
    component.apiModel = 'claude-opus-4-6';
    component.maxTokens = 4096;
    component.allowedTools = 'Read,Write';
    component.timeoutMinutes = 10;
    component.dangerouslySkipPermissions = false;

    component.saveConfig();

    const req = httpTesting.expectOne('http://localhost:8080/api/config/agents');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body.maxTurns).toBe(50);
    expect(req.request.body.cliModel).toBe('claude-opus-4-6');
    req.flush({});

    expect(component.saving()).toBe(false);
  });

  it('should show default values', () => {
    expect(component.getDefault('maxTurns')).toBe('40');
    expect(component.getDefault('workspace')).toBe('C:/tickcode-workspace');
    expect(component.getDefault('cliModel')).toBe('claude-opus-4-6');
  });
});
