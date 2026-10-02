import { TestBed } from '@angular/core/testing';

import { ToastRegion, Toaster } from './toast';

describe('Toaster', () => {
  afterEach(() => vi.useRealTimers());

  it('withdraws a confirmation after a while, but keeps a failure until closed', () => {
    vi.useFakeTimers();
    const toaster = TestBed.inject(Toaster);

    toaster.show('Tag « Java » créé.');
    toaster.show('La suppression a échoué.', 'danger');
    expect(toaster.toasts().map((toast) => toast.tone)).toEqual(['success', 'danger']);

    vi.advanceTimersByTime(6000);
    expect(toaster.toasts().map((toast) => toast.message)).toEqual(['La suppression a échoué.']);

    toaster.dismiss(toaster.toasts()[0].id);
    expect(toaster.toasts()).toEqual([]);
  });
});

describe('ToastRegion', () => {
  it('announces the notifications politely, each with a way to close it', async () => {
    const fixture = TestBed.createComponent(ToastRegion);
    const toaster = TestBed.inject(Toaster);
    toaster.show('La suppression a échoué.', 'danger');
    await fixture.whenStable();
    const element = fixture.nativeElement as HTMLElement;

    const live = element.querySelector('[aria-live="polite"]')!;
    expect(live.textContent).toContain('La suppression a échoué.');
    expect(element.querySelector('[role="alert"]')).toBeNull();

    element
      .querySelector<HTMLButtonElement>('button[aria-label="Fermer la notification"]')!
      .click();
    await fixture.whenStable();
    expect(live.textContent?.trim()).toBe('');
  });
});
