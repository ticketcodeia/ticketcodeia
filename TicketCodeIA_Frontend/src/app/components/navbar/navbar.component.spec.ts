import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { NavbarComponent } from './navbar.component';

describe('NavbarComponent', () => {
  let fixture: ComponentFixture<NavbarComponent>;
  let component: NavbarComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NavbarComponent],
      providers: [provideRouter([]), provideHttpClient()]
    }).compileComponents();

    fixture = TestBed.createComponent(NavbarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display the logo', () => {
    const logo = fixture.nativeElement.querySelector('.logo');
    expect(logo.textContent).toBe('TickCode');
  });

  it('should have navigation links', () => {
    const links = fixture.nativeElement.querySelectorAll('.nav-links a');
    expect(links.length).toBe(4);
    expect(links[0].textContent.trim()).toContain('Dashboard');
    expect(links[1].textContent.trim()).toContain('Kanban Board');
    expect(links[2].textContent.trim()).toContain('Requirements');
    expect(links[3].textContent.trim()).toContain('Escalations');
  });

  it('should show connection status', () => {
    const status = fixture.nativeElement.querySelector('.connection-status');
    expect(status).toBeTruthy();
    expect(status.textContent.trim()).toBe('Disconnected');
  });
});
