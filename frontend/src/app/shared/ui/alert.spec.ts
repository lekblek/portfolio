import { Component, signal, viewChild } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { Alert, AlertTone } from './alert';

@Component({
  imports: [Alert],
  template: `<app-alert [tone]="tone()" title="Issue"><p>Détail.</p></app-alert>`,
})
class Host {
  readonly tone = signal<AlertTone>('danger');
  readonly alert = viewChild.required(Alert);
}

describe('Alert', () => {
  it('announces a failure as soon as it appears', () => {
    const fixture = TestBed.createComponent(Host);
    fixture.detectChanges();

    const alert = (fixture.nativeElement as HTMLElement).querySelector('app-alert')!;
    expect(alert.getAttribute('role')).toBe('alert');
    expect(alert.textContent).toContain('Issue');
    expect(alert.textContent).toContain('Détail.');
  });

  it('lets a success take the focus instead', () => {
    const fixture = TestBed.createComponent(Host);
    fixture.componentInstance.tone.set('success');
    fixture.detectChanges();
    document.body.appendChild(fixture.nativeElement);

    fixture.componentInstance.alert().focus();

    const alert = (fixture.nativeElement as HTMLElement).querySelector('app-alert')!;
    expect(alert.hasAttribute('role')).toBe(false);
    expect(document.activeElement).toBe(alert);
    fixture.nativeElement.remove();
  });
});
