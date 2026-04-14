import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { RequirementsComponent } from './requirements.component';

describe('RequirementsComponent', () => {
  let fixture: ComponentFixture<RequirementsComponent>;
  let component: RequirementsComponent;
  let httpTesting: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RequirementsComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideNoopAnimations()]
    }).compileComponents();

    fixture = TestBed.createComponent(RequirementsComponent);
    component = fixture.componentInstance;
    httpTesting = TestBed.inject(HttpTestingController);
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load projects on init', () => {
    fixture.detectChanges();

    const req = httpTesting.expectOne('http://localhost:8080/api/projects');
    req.flush([{ id: 1, name: 'Project1', description: 'desc', createdAt: '2026-01-01T00:00:00' }]);

    expect(component.projects().length).toBe(1);
  });

  it('should display page title', () => {
    fixture.detectChanges();
    httpTesting.expectOne('http://localhost:8080/api/projects').flush([]);

    const h1 = fixture.nativeElement.querySelector('h1');
    expect(h1.textContent).toBe('Create Tickets');
  });

  it('should start with empty generated tickets', () => {
    expect(component.generatedTickets().length).toBe(0);
  });

  it('should toggle new project form', () => {
    expect(component.showNewProject).toBe(false);
    component.showNewProject = !component.showNewProject;
    expect(component.showNewProject).toBe(true);
  });

  it('should not create project when name is empty', () => {
    component.newProjectName = '   ';
    component.createProject();
    httpTesting.expectNone('http://localhost:8080/api/projects');
  });

  it('should have no selected project initially', () => {
    expect(component.selectedProjectId).toBeNull();
  });

  it('should display subtitle', () => {
    fixture.detectChanges();
    httpTesting.expectOne('http://localhost:8080/api/projects').flush([]);

    const subtitle = fixture.nativeElement.querySelector('.subtitle');
    expect(subtitle.textContent).toContain('Expert Agent');
  });

  it('should start with empty chat', () => {
    expect(component.chatMessages().length).toBe(0);
    expect(component.chatLoading()).toBe(false);
    expect(component.chatInput).toBe('');
  });

  it('should not send chat when input is empty', () => {
    component.chatInput = '   ';
    component.sendChatMessage();
    httpTesting.expectNone('http://localhost:8080/api/agents/expert/chat');
  });

  it('should load chat history when project is selected', () => {
    component.selectedProjectId = 1;
    component.onProjectChange();

    const req = httpTesting.expectOne('http://localhost:8080/api/agents/expert/project/1/history');
    req.flush([
      { id: 1, sessionId: 'sess-1', projectId: 1, role: 'user', content: 'Hello', createdAt: '2026-04-14T10:00:00' },
      { id: 2, sessionId: 'sess-1', projectId: 1, role: 'assistant', content: 'Hi!', createdAt: '2026-04-14T10:00:01' }
    ]);

    expect(component.chatMessages().length).toBe(2);
    expect(component.chatSessionId).toBe('sess-1');
  });

  it('should reset chat when no project selected', () => {
    component.chatMessages.set([{ role: 'user', content: 'test', timestamp: new Date() }]);
    component.selectedProjectId = null;
    component.onProjectChange();

    expect(component.chatMessages().length).toBe(0);
  });

  it('should clear chat session', () => {
    component.chatMessages.set([{ role: 'user', content: 'hello', timestamp: new Date() }]);
    const oldSessionId = component.chatSessionId;

    component.clearChat();

    const req = httpTesting.expectOne(`http://localhost:8080/api/agents/expert/session/${oldSessionId}`);
    req.flush(null);

    expect(component.chatMessages().length).toBe(0);
    expect(component.chatSessionId).not.toBe(oldSessionId);
  });
});
