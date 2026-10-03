import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { Field } from './field';
import { MultiSelect, MultiSelectOption } from './multi-select';

const OPTIONS: MultiSelectOption<number>[] = [
  { value: 1, label: 'Java' },
  { value: 2, label: 'Python' },
  { value: 3, label: 'PyTorch' },
  { value: 4, label: 'Sécurité' },
];

@Component({
  imports: [Field, MultiSelect],
  template: `
    <app-field label="Technologies" controlId="techs">
      <app-multi-select listLabel="Technologies" [options]="options" [(value)]="value" />
    </app-field>
  `,
})
class Host {
  readonly options = OPTIONS;
  readonly value = signal<number[]>([1, 4]);
}

async function setUp() {
  const fixture = TestBed.createComponent(Host);
  await fixture.whenStable();
  const element = fixture.nativeElement as HTMLElement;
  const input = element.querySelector<HTMLInputElement>('input')!;
  const settle = async () => {
    await fixture.whenStable();
    await new Promise((resolve) => setTimeout(resolve));
    await fixture.whenStable();
  };
  const type = async (text: string) => {
    input.value = text;
    input.dispatchEvent(new Event('input'));
    await settle();
  };
  const chips = () =>
    Array.from(element.querySelectorAll('.multi-select-chip > span')).map(
      (chip) => chip.textContent,
    );
  return { fixture, element, input, type, settle, chips };
}

describe('MultiSelect', () => {
  it('is a labelled combobox showing the chosen values', async () => {
    const { input, chips } = await setUp();

    expect(input.getAttribute('role')).toBe('combobox');
    expect(input.id).toBe('techs');
    expect(chips()).toEqual(['Java', 'Sécurité']);
  });

  it('filters without accents and keeps the choices hidden by the filter', async () => {
    const { fixture, element, type, settle, chips } = await setUp();
    await type('py');
    await settle();

    const options = Array.from(element.querySelectorAll('[role="option"]'));
    expect(options.map((option) => option.textContent?.trim())).toEqual(['Python', 'PyTorch']);
    expect(element.querySelector('[role="listbox"]')?.getAttribute('aria-multiselectable')).toBe(
      'true',
    );

    (options[1] as HTMLElement).dispatchEvent(new PointerEvent('pointerdown', { bubbles: true }));
    (options[1] as HTMLElement).click();
    await settle();

    expect(fixture.componentInstance.value()).toEqual([1, 3, 4]);
    expect(chips()).toEqual(['Java', 'PyTorch', 'Sécurité']);

    await type('securite');
    expect(element.querySelectorAll('[role="option"]')).toHaveLength(1);
  });

  it('removes a choice and keeps the focus among the choices', async () => {
    const { fixture, element, settle, chips } = await setUp();

    element.querySelector<HTMLButtonElement>('[data-chip="0"]')!.click();
    await settle();

    expect(fixture.componentInstance.value()).toEqual([4]);
    expect(chips()).toEqual(['Sécurité']);
    expect(document.activeElement?.getAttribute('data-chip')).toBe('0');
    expect(element.querySelector('[aria-live="polite"]')?.textContent).toBe('Java retiré.');
  });
});
