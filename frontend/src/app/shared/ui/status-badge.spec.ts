import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { ContentStatus, StatusBadge } from './status-badge';

@Component({
  imports: [StatusBadge],
  template: `<app-status-badge [status]="status()" [feminine]="feminine()" />`,
})
class Host {
  readonly status = signal<ContentStatus>('PUBLISHED');
  readonly feminine = signal(false);
}

describe('StatusBadge', () => {
  it('says the status in words, agreed with the content', async () => {
    const fixture = TestBed.createComponent(Host);
    await fixture.whenStable();
    const badge = (fixture.nativeElement as HTMLElement).querySelector('app-status-badge')!;

    expect(badge.textContent?.trim()).toBe('Publié');
    expect(badge.getAttribute('data-status')).toBe('PUBLISHED');
    expect(badge.querySelector('[aria-hidden="true"]')).not.toBeNull();

    fixture.componentInstance.feminine.set(true);
    fixture.componentInstance.status.set('SCHEDULED');
    await fixture.whenStable();
    expect(badge.textContent?.trim()).toBe('Programmée');

    fixture.componentInstance.status.set('DRAFT');
    await fixture.whenStable();
    expect(badge.textContent?.trim()).toBe('Brouillon');
  });
});
