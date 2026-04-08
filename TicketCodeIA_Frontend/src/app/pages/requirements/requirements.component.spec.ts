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
    expect(h1.textContent).toBe('Requirements Input');
  });

  it('should start with empty requirements', () => {
    expect(component.requirements).toBe('');
    expect(component.generating()).toBe(false);
    expect(component.generatedTickets().length).toBe(0);
  });

  it('should not generate when requirements is empty', () => {
    component.requirements = '   ';
    component.generateTickets();
    httpTesting.expectNone('http://localhost:8080/api/agents/generate-tickets');
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
    expect(subtitle.textContent).toContain('PO Agent');
  });
});
