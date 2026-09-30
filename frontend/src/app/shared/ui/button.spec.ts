import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { Button, ButtonSize, ButtonVariant } from './button';

@Component({
  imports: [Button],
  template: `
    <button
      appButton
      type="button"
      [variant]="variant()"
      [size]="size()"
      [loading]="loading()"
      [disabled]="disabled()"
      [attr.aria-disabled]="ariaDisabled() ? 'true' : null"
      (click)="clicks.set(clicks() + 1)"
    >
      Publier
    </button>
    <a appButton variant="secondary" href="/projects">Voir les projets</a>
  `,
})
class Host {
  readonly variant = signal<ButtonVariant>('primary');
  readonly size = signal<ButtonSize>('md');
  readonly loading = signal(false);
  readonly disabled = signal(false);
  readonly ariaDisabled = signal(false);
  readonly clicks = signal(0);
}

describe('Button', () => {
  function setUp() {
    const fixture = TestBed.createComponent(Host);
    fixture.detectChanges();
    const element: HTMLElement = fixture.nativeElement;
    return {
      fixture,
      host: fixture.componentInstance,
      button: element.querySelector('button') as HTMLButtonElement,
      link: element.querySelector('a') as HTMLAnchorElement,
    };
  }

  it('applies the classes of each variant and size', () => {
    const { fixture, host, button } = setUp();
    expect(button.className).toContain('bg-ink');
    expect(button.className).toContain('min-h-11');

    host.variant.set('quiet');
    host.size.set('sm');
    fixture.detectChanges();

    expect(button.className).toContain('text-accent');
    expect(button.className).not.toContain('bg-ink');
    expect(button.className).toContain('min-h-8');
  });

  it('keeps a styled link a real link', () => {
    const { link } = setUp();

    expect(link.getAttribute('href')).toBe('/projects');
    expect(link.getAttribute('role')).toBeNull();
    expect(link.className).toContain('border-ink');
  });

  it('removes a disabled button from the tab order', () => {
    const { fixture, host, button } = setUp();

    host.disabled.set(true);
    fixture.detectChanges();

    expect(button.disabled).toBe(true);
    button.focus();
    expect(document.activeElement).not.toBe(button);
  });

  it('keeps an aria-disabled button focusable but ignores its activation', () => {
    const { fixture, host, button } = setUp();
    document.body.appendChild(fixture.nativeElement);

    host.ariaDisabled.set(true);
    fixture.detectChanges();
    button.focus();
    button.click();

    expect(document.activeElement).toBe(button);
    expect(button.getAttribute('aria-disabled')).toBe('true');
    expect(host.clicks()).toBe(0);
    fixture.nativeElement.remove();
  });

  it('announces loading, keeps the label and ignores repeated activations', () => {
    const { fixture, host, button } = setUp();

    host.loading.set(true);
    fixture.detectChanges();
    button.click();
    button.click();

    expect(button.getAttribute('aria-busy')).toBe('true');
    expect(button.textContent?.trim()).toBe('Publier');
    expect(host.clicks()).toBe(0);
  });

  it('reacts normally when active', () => {
    const { host, button } = setUp();

    button.click();

    expect(button.getAttribute('aria-busy')).toBeNull();
    expect(host.clicks()).toBe(1);
  });
});
