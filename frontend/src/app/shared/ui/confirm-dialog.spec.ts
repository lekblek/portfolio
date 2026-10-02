import { Component, DOCUMENT, viewChild } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { ConfirmDialog } from './confirm-dialog';

@Component({
  imports: [ConfirmDialog],
  template: `
    <button type="button" id="ouvrir">Supprimer</button>
    <app-confirm-dialog #confirm />
  `,
})
class Host {
  readonly confirm = viewChild.required<ConfirmDialog>('confirm');
}

async function open() {
  const fixture = TestBed.createComponent(Host);
  await fixture.whenStable();
  const element = fixture.nativeElement as HTMLElement;
  const opener = element.querySelector<HTMLButtonElement>('#ouvrir')!;
  opener.focus();
  const answer = fixture.componentInstance.confirm().ask({
    title: 'Supprimer le tag « Java » ?',
    message: 'Le tag sera supprimé définitivement.',
    confirmLabel: 'Supprimer le tag',
  });
  await fixture.whenStable();
  const dialog = element.querySelector('dialog')!;
  const button = (name: string) =>
    Array.from(dialog.querySelectorAll('button')).find(
      (candidate) => candidate.textContent?.trim() === name,
    )!;
  return { answer, dialog, opener, button };
}

describe('ConfirmDialog', () => {
  it('names the element, then focuses the way out first', async () => {
    const { dialog, button } = await open();

    expect(dialog.open).toBe(true);
    expect(dialog.querySelector(`#${dialog.getAttribute('aria-labelledby')}`)?.textContent).toBe(
      'Supprimer le tag « Java » ?',
    );
    expect(dialog.querySelector(`#${dialog.getAttribute('aria-describedby')}`)?.textContent).toBe(
      'Le tag sera supprimé définitivement.',
    );
    expect(TestBed.inject(DOCUMENT).activeElement).toBe(button('Annuler'));
    expect(button('Supprimer le tag').className).toContain('bg-danger');
  });

  it('answers yes to the destructive action and gives the focus back', async () => {
    const { answer, dialog, opener, button } = await open();

    button('Supprimer le tag').click();

    expect(await answer).toBe(true);
    expect(dialog.open).toBe(false);
    expect(TestBed.inject(DOCUMENT).activeElement).toBe(opener);
  });

  it('answers no when cancelled or closed with escape', async () => {
    const first = await open();
    first.button('Annuler').click();
    expect(await first.answer).toBe(false);

    TestBed.resetTestingModule();
    const second = await open();
    // Échap : le navigateur ferme le dialogue sans valeur de retour
    second.dialog.close();
    expect(await second.answer).toBe(false);
  });
});
