import { TestBed } from '@angular/core/testing';
import { PLATFORM_ID } from '@angular/core';
import { SseService } from './sse.service';
import type { SseEvent } from '../models/ticket.model';
import { TicketStatus, AgentType } from '../models/ticket.model';

describe('SseService', () => {
  let service: SseService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [{ provide: PLATFORM_ID, useValue: 'browser' }]
    });
    service = TestBed.inject(SseService);
  });

  afterEach(() => {
    service.disconnect();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should start disconnected', () => {
    expect(service.connected()).toBe(false);
  });

  it('should have no last event initially', () => {
    expect(service.lastEvent()).toBeNull();
  });

  it('should subscribe and unsubscribe listeners', () => {
    const callback = vi.fn();
    const unsubscribe = service.subscribe(callback);

    expect(typeof unsubscribe).toBe('function');
    unsubscribe();
  });

  it('should disconnect and set connected to false', () => {
    service.disconnect();
    expect(service.connected()).toBe(false);
  });
});
