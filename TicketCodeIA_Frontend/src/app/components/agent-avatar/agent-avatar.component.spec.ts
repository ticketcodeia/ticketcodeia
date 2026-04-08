import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Component } from '@angular/core';
import { AgentAvatarComponent } from './agent-avatar.component';
import { AgentType } from '../../models/ticket.model';

@Component({
  standalone: true,
  imports: [AgentAvatarComponent],
  template: `<app-agent-avatar [agent]="agent" />`
})
class TestHostComponent {
  agent: AgentType | null = null;
}

describe('AgentAvatarComponent', () => {
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
    const avatar = fixture.nativeElement.querySelector('.agent-avatar');
    expect(avatar).toBeTruthy();
  });

  it('should show question mark for null agent', () => {
    host.agent = null;
    fixture.detectChanges();
    const avatar = fixture.nativeElement.querySelector('.agent-avatar');
    expect(avatar.textContent.trim()).toContain('❓');
  });

  it('should show developer emoji', () => {
    host.agent = AgentType.DEVELOPER;
    fixture.detectChanges();
    const avatar = fixture.nativeElement.querySelector('.agent-avatar');
    expect(avatar.textContent.trim()).toContain('💻');
    expect(avatar.getAttribute('title')).toBe('Developer');
  });

  it('should show reviewer emoji', () => {
    host.agent = AgentType.REVIEWER;
    fixture.detectChanges();
    const avatar = fixture.nativeElement.querySelector('.agent-avatar');
    expect(avatar.textContent.trim()).toContain('🔍');
    expect(avatar.getAttribute('title')).toBe('Reviewer');
  });

  it('should show tester emoji', () => {
    host.agent = AgentType.TESTER;
    fixture.detectChanges();
    const avatar = fixture.nativeElement.querySelector('.agent-avatar');
    expect(avatar.textContent.trim()).toContain('🧪');
  });

  it('should show PO emoji', () => {
    host.agent = AgentType.PO;
    fixture.detectChanges();
    const avatar = fixture.nativeElement.querySelector('.agent-avatar');
    expect(avatar.textContent.trim()).toContain('📋');
    expect(avatar.getAttribute('title')).toBe('Product Owner');
  });

  it('should show human emoji', () => {
    host.agent = AgentType.HUMAN;
    fixture.detectChanges();
    const avatar = fixture.nativeElement.querySelector('.agent-avatar');
    expect(avatar.textContent.trim()).toContain('👤');
  });
});
