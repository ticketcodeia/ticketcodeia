import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Component } from '@angular/core';
import { StatusBadgeComponent } from './status-badge.component';
import { TicketStatus } from '../../models/ticket.model';

@Component({
  standalone: true,
  imports: [StatusBadgeComponent],
  template: `<app-status-badge [status]="status" />`
})
class TestHostComponent {
  status: TicketStatus = TicketStatus.TODO;
}

describe('StatusBadgeComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let host: TestHostComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    host = fixture.componentInstance;
  });

  it('should create', () => {
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.status-badge');
    expect(badge).toBeTruthy();
  });

  it('should display "To Do" for TODO status', () => {
    host.status = TicketStatus.TODO;
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.status-badge');
    expect(badge.textContent.trim()).toBe('To Do');
    expect(badge.classList.contains('todo')).toBe(true);
  });

  it('should display "In Progress" for IN_PROGRESS status', () => {
    host.status = TicketStatus.IN_PROGRESS;
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.status-badge');
    expect(badge.textContent.trim()).toBe('In Progress');
    expect(badge.classList.contains('in-progress')).toBe(true);
  });

  it('should display "Code Review" for CODE_REVIEW status', () => {
    host.status = TicketStatus.CODE_REVIEW;
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.status-badge');
    expect(badge.textContent.trim()).toBe('Code Review');
    expect(badge.classList.contains('code-review')).toBe(true);
  });

  it('should display "Testing" for TESTING status', () => {
    host.status = TicketStatus.TESTING;
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.status-badge');
    expect(badge.textContent.trim()).toBe('Testing');
    expect(badge.classList.contains('testing')).toBe(true);
  });

  it('should display "Done" for DONE status', () => {
    host.status = TicketStatus.DONE;
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.status-badge');
    expect(badge.textContent.trim()).toBe('Done');
    expect(badge.classList.contains('done')).toBe(true);
  });

  it('should display "Escalated" for ESCALATED status', () => {
    host.status = TicketStatus.ESCALATED;
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.status-badge');
    expect(badge.textContent.trim()).toBe('Escalated');
    expect(badge.classList.contains('escalated')).toBe(true);
  });
});
