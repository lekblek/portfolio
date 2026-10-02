import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { FileDrop } from './file-drop';

@Component({
  imports: [FileDrop],
  template: `
    <app-file-drop
      label="Ajouter des fichiers"
      hint="PNG ou PDF."
      accept="image/png,application/pdf"
      [multiple]="multiple()"
      (files)="received.set($event)"
    />
  `,
})
class Host {
  readonly multiple = signal(true);
  readonly received = signal<File[]>([]);
}

const PNG = new File(['png'], 'a.png', { type: 'image/png' });
const PDF = new File(['pdf'], 'b.pdf', { type: 'application/pdf' });

async function setUp(multiple = true) {
  const fixture = TestBed.createComponent(Host);
  fixture.componentInstance.multiple.set(multiple);
  await fixture.whenStable();
  const element = fixture.nativeElement as HTMLElement;
  const zone = element.querySelector<HTMLElement>('.file-drop')!;
  const drop = (files: File[]) => {
    const event = new Event('drop', { bubbles: true, cancelable: true });
    Object.defineProperty(event, 'dataTransfer', { value: { files } });
    zone.dispatchEvent(event);
    return event;
  };
  return { fixture, element, zone, drop };
}

describe('FileDrop', () => {
  it('offers a button described by the hint, the native field staying out of reach', async () => {
    const { element } = await setUp();

    const button = element.querySelector('button')!;
    const hint = element.querySelector(`#${button.getAttribute('aria-describedby')}`);
    expect(button.textContent?.trim()).toBe('Choisir des fichiers');
    expect(hint?.textContent).toBe('PNG ou PDF.');
    const input = element.querySelector<HTMLInputElement>('input[type="file"]')!;
    expect(input.getAttribute('tabindex')).toBe('-1');
    expect(input.accept).toBe('image/png,application/pdf');
    expect(input.multiple).toBe(true);
  });

  it('opens the system picker from the button', async () => {
    const { element } = await setUp();
    const input = element.querySelector<HTMLInputElement>('input[type="file"]')!;
    const click = vi.spyOn(input, 'click').mockImplementation(() => undefined);

    element.querySelector('button')!.click();

    expect(click).toHaveBeenCalledOnce();
  });

  it('hands over dropped files, and keeps the browser from opening them', async () => {
    const { fixture, drop } = await setUp();

    const event = drop([PNG, PDF]);

    expect(event.defaultPrevented).toBe(true);
    expect(fixture.componentInstance.received()).toEqual([PNG, PDF]);
  });

  it('keeps the first file only when a single one is expected', async () => {
    const { fixture, drop } = await setUp(false);

    drop([PNG, PDF]);

    expect(fixture.componentInstance.received()).toEqual([PNG]);
  });
});
