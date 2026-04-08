import { Injectable, NgZone, PLATFORM_ID, inject, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { SseEvent } from '../models/ticket.model';

@Injectable({
  providedIn: 'root'
})
export class SseService {
  private readonly zone = inject(NgZone);
  private readonly platformId = inject(PLATFORM_ID);
  private eventSource: EventSource | null = null;
  private readonly apiUrl = 'http://localhost:8080/api';

  readonly connected = signal(false);
  readonly lastEvent = signal<SseEvent | null>(null);

  private eventListeners: ((event: SseEvent) => void)[] = [];

  connect(): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    if (this.eventSource) {
      return;
    }

    this.zone.runOutsideAngular(() => {
      this.eventSource = new EventSource(`${this.apiUrl}/sse/subscribe`);

      this.eventSource.onopen = () => {
        this.zone.run(() => {
          this.connected.set(true);
          console.log('SSE connected');
        });
      };

      this.eventSource.addEventListener('connected', (event) => {
        console.log('SSE connection confirmed:', event.data);
      });

      this.eventSource.addEventListener('message', (event) => {
        this.zone.run(() => {
          try {
            const sseEvent: SseEvent = JSON.parse(event.data);
            this.lastEvent.set(sseEvent);
            this.notifyListeners(sseEvent);
          } catch (e) {
            console.error('Error parsing SSE event:', e);
          }
        });
      });

      this.eventSource.onerror = () => {
        this.zone.run(() => {
          this.connected.set(false);
          this.reconnect();
        });
      };
    });
  }

  disconnect(): void {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
      this.connected.set(false);
    }
  }

  private reconnect(): void {
    this.disconnect();
    setTimeout(() => this.connect(), 3000);
  }

  subscribe(callback: (event: SseEvent) => void): () => void {
    this.eventListeners.push(callback);
    return () => {
      const index = this.eventListeners.indexOf(callback);
      if (index > -1) {
        this.eventListeners.splice(index, 1);
      }
    };
  }

  private notifyListeners(event: SseEvent): void {
    this.eventListeners.forEach(listener => listener(event));
  }
}
