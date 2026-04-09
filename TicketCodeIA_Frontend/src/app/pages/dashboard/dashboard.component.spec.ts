import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { PLATFORM_ID } from '@angular/core';
import { DashboardComponent } from './dashboard.component';

describe('DashboardComponent', () => {
  let fixture: ComponentFixture<DashboardComponent>;
  let component: DashboardComponent;
  let httpTesting: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        provideHttpClient(), provideHttpClientTesting(), provideNoopAnimations(),
        { provide: PLATFORM_ID, useValue: 'server' }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    httpTesting = TestBed.inject(HttpTestingController);
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load stats on init', () => {
    fixture.detectChanges();

    const statsReq = httpTesting.expectOne('http://localhost:8080/api/tickets/stats');
    statsReq.flush({ total: 5, todo: 2, inProgress: 1, codeReview: 0, testing: 1, done: 1, escalated: 0, humanTodo: 0, humanDev: 0, humanReview: 0, humanTesting: 0 });

    const activityReq = httpTesting.expectOne('http://localhost:8080/api/sse/recent-activity');
    activityReq.flush([]);

    expect(component.stats().total).toBe(5);
    expect(component.stats().todo).toBe(2);
  });

  it('should display stat cards', () => {
    fixture.detectChanges();

    httpTesting.expectOne('http://localhost:8080/api/tickets/stats')
      .flush({ total: 10, todo: 3, inProgress: 2, codeReview: 1, testing: 1, done: 2, escalated: 1, humanTodo: 0, humanDev: 0, humanReview: 0, humanTesting: 0 });
    httpTesting.expectOne('http://localhost:8080/api/sse/recent-activity').flush([]);

    fixture.detectChanges();

    const statCards = fixture.nativeElement.querySelectorAll('.stat-card');
    expect(statCards.length).toBe(11);
  });

  it('should show "No recent activity" when empty', () => {
    fixture.detectChanges();

    httpTesting.expectOne('http://localhost:8080/api/tickets/stats')
      .flush({ total: 0, todo: 0, inProgress: 0, codeReview: 0, testing: 0, done: 0, escalated: 0, humanTodo: 0, humanDev: 0, humanReview: 0, humanTesting: 0 });
    httpTesting.expectOne('http://localhost:8080/api/sse/recent-activity').flush([]);

    fixture.detectChanges();

    const noActivity = fixture.nativeElement.querySelector('.no-activity');
    expect(noActivity.textContent.trim()).toBe('No recent activity');
  });

  it('should display h1 title', () => {
    fixture.detectChanges();
    httpTesting.expectOne('http://localhost:8080/api/tickets/stats').flush({ total: 0, todo: 0, inProgress: 0, codeReview: 0, testing: 0, done: 0, escalated: 0, humanTodo: 0, humanDev: 0, humanReview: 0, humanTesting: 0 });
    httpTesting.expectOne('http://localhost:8080/api/sse/recent-activity').flush([]);

    const h1 = fixture.nativeElement.querySelector('h1');
    expect(h1.textContent).toBe('Dashboard');
  });
});
