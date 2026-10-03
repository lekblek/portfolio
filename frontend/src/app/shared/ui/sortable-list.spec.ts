import { LiveAnnouncer } from '@angular/cdk/a11y';
import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { moveItem, SortableItem, SortableList, SortableMove } from './sortable-list';

@Component({
  imports: [SortableList, SortableItem],
  template: `
    <app-sortable-list label="Liens" (moved)="move($event)">
      @for (item of items(); track item; let i = $index) {
        <app-sortable-item
          [index]="i"
          [count]="items().length"
          [name]="'le lien ' + item"
          (moveTo)="move($event)"
          (remove)="removed.set(item)"
        >
          <input [attr.aria-label]="item" />
        </app-sortable-item>
      }
    </app-sortable-list>
  `,
})
class Host {
  readonly items = signal(['GitHub', 'LinkedIn', 'Blog']);
  readonly removed = signal<string | null>(null);

  move(move: SortableMove): void {
    this.items.update((list) => moveItem(list, move));
  }
}

async function setUp() {
  const announce = vi.fn(() => Promise.resolve());
  TestBed.configureTestingModule({
    providers: [{ provide: LiveAnnouncer, useValue: { announce } }],
  });
  const fixture = TestBed.createComponent(Host);
  await fixture.whenStable();
  const element = fixture.nativeElement as HTMLElement;
  const button = (name: string) =>
    [...element.querySelectorAll('button')].find(
      (b) => b.textContent!.replace(/\s+/g, ' ').trim() === name,
    )!;
  const settle = async () => {
    await fixture.whenStable();
    await new Promise((resolve) => setTimeout(resolve));
    await fixture.whenStable();
  };
  return { fixture, element, button, announce, settle };
}

describe('moveItem', () => {
  it('moves an item without touching the original list', () => {
    const list = ['a', 'b', 'c'];

    expect(moveItem(list, { from: 0, to: 2 })).toEqual(['b', 'c', 'a']);
    expect(moveItem(list, { from: 2, to: 0 })).toEqual(['c', 'a', 'b']);
    expect(list).toEqual(['a', 'b', 'c']);
  });
});

describe('SortableList', () => {
  it('names the list, its items and their buttons', async () => {
    const { element, button } = await setUp();

    expect(element.querySelector('[role="list"]')?.getAttribute('aria-label')).toBe('Liens');
    expect(element.querySelectorAll('[role="listitem"]')).toHaveLength(3);
    expect(button('Monter le lien GitHub').disabled).toBe(true);
    expect(button('Descendre le lien Blog').disabled).toBe(true);
    expect(button('Descendre le lien GitHub').disabled).toBe(false);
    expect(element.textContent).toContain('1 / 3');
  });

  it('moves an item with the keyboard buttons, announces it and keeps the focus', async () => {
    const { fixture, button, announce, settle } = await setUp();

    button('Descendre le lien GitHub').click();
    await settle();

    expect(fixture.componentInstance.items()).toEqual(['LinkedIn', 'GitHub', 'Blog']);
    expect(announce).toHaveBeenCalledWith('le lien GitHub déplacé en position 2 sur 3.');
    expect(document.activeElement).toBe(button('Descendre le lien GitHub'));
  });

  it('hands the focus to the other button when the item reaches an end', async () => {
    const { fixture, button, settle } = await setUp();

    button('Monter le lien LinkedIn').click();
    await settle();

    expect(fixture.componentInstance.items()).toEqual(['LinkedIn', 'GitHub', 'Blog']);
    expect(document.activeElement).toBe(button('Descendre le lien LinkedIn'));
  });

  it('asks the page to remove an item', async () => {
    const { fixture, button } = await setUp();

    button('Retirer le lien Blog').click();

    expect(fixture.componentInstance.removed()).toBe('Blog');
  });
});
