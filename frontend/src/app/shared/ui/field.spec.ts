import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { Field, FieldControl } from './field';

@Component({
  imports: [Field, FieldControl],
  template: `
    <app-field label="Adresse" controlId="adresse" [hint]="hint()" [error]="error()" optional>
      <input appFieldControl type="email" />
    </app-field>
  `,
})
class Host {
  readonly hint = signal<string | null>('Pour vous répondre.');
  readonly error = signal<string | null>(null);
}

describe('Field', () => {
  function render() {
    const fixture = TestBed.createComponent(Host);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    return { fixture, element, input: element.querySelector('input')! };
  }

  it('links the label and the hint to the control', () => {
    const { element, input } = render();

    expect(element.querySelector('label')?.getAttribute('for')).toBe('adresse');
    expect(element.querySelector('label')?.textContent).toContain('(facultatif)');
    expect(input.id).toBe('adresse');
    expect(input.getAttribute('aria-describedby')).toBe('adresse-aide');
    expect(input.getAttribute('aria-invalid')).toBeNull();
    expect(input.classList).toContain('field-control');
  });

  it('describes the control by its error first, and marks it invalid', () => {
    const { fixture, element, input } = render();

    fixture.componentInstance.error.set('Adresse incomplète.');
    fixture.detectChanges();

    expect(input.getAttribute('aria-describedby')).toBe('adresse-erreur adresse-aide');
    expect(input.getAttribute('aria-invalid')).toBe('true');
    expect(element.querySelector('#adresse-erreur')?.textContent).toContain('Adresse incomplète.');
  });

  it('describes nothing without hint nor error', () => {
    const { fixture, input } = render();

    fixture.componentInstance.hint.set(null);
    fixture.detectChanges();

    expect(input.hasAttribute('aria-describedby')).toBe(false);
  });
});
