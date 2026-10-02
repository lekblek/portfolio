import { booleanAttribute, Component, input, output, signal } from '@angular/core';

import { Button } from './button';
import { Icon } from './icon';

let nextId = 0;

/**
 * Zone d'envoi de fichiers (02-design-system §18). Le chemin principal est un bouton qui ouvre le
 * sélecteur du système (clavier, lecteur d'écran, tactile) ; déposer des fichiers sur la zone en
 * est le raccourci à la souris. Les fichiers choisis sont émis tels quels (`files`) : la page les
 * vérifie et les envoie. Le champ natif reste dans la page, masqué, lié à la consigne.
 */
@Component({
  selector: 'app-file-drop',
  imports: [Button, Icon],
  template: `
    <div
      class="file-drop"
      [class.file-drop-over]="over()"
      (dragenter)="enter($event)"
      (dragover)="enter($event)"
      (dragleave)="over.set(false)"
      (drop)="drop($event)"
    >
      <app-icon name="upload" class="file-drop-icon" />
      <p class="font-semibold">{{ label() }}</p>
      <p class="text-sm text-ink-muted" [id]="hintId">{{ hint() }}</p>
      <button
        appButton
        type="button"
        variant="secondary"
        class="mt-3"
        [attr.aria-describedby]="hintId"
        (click)="picker.click()"
      >
        {{ buttonLabel() }}
      </button>
      <input
        #picker
        class="sr-only"
        type="file"
        tabindex="-1"
        aria-hidden="true"
        [accept]="accept()"
        [multiple]="multiple()"
        (change)="choose($event)"
      />
    </div>
  `,
  styles: `
    .file-drop {
      display: grid;
      justify-items: start;
      gap: calc(var(--spacing) * 1);
      padding: calc(var(--spacing) * 6);
      border: var(--border-strong) dashed var(--color-ink-muted);
      border-radius: var(--radius-control);
      background: var(--color-paper);
    }

    .file-drop-over {
      border-style: solid;
      border-color: var(--color-accent);
      background: var(--color-paper-sunken);
    }

    .file-drop-icon {
      color: var(--color-ink-muted);
    }
  `,
})
export class FileDrop {
  /** Ce que la zone reçoit : « Ajouter des fichiers ». */
  readonly label = input.required<string>();
  /** Formats et tailles acceptés, lus aussi par le lecteur d'écran avec le bouton. */
  readonly hint = input.required<string>();
  readonly buttonLabel = input('Choisir des fichiers');
  /** Filtre du sélecteur du système (`accept`) : une aide, la page vérifie toujours. */
  readonly accept = input('');
  readonly multiple = input(false, { transform: booleanAttribute });
  readonly files = output<File[]>();

  protected readonly over = signal(false);
  protected readonly hintId = `envoi-${++nextId}-consigne`;

  protected enter(event: DragEvent): void {
    // Sans preventDefault, le navigateur ouvrirait le fichier déposé à la place de la page
    event.preventDefault();
    this.over.set(true);
  }

  protected drop(event: DragEvent): void {
    event.preventDefault();
    this.over.set(false);
    this.emit(Array.from(event.dataTransfer?.files ?? []));
  }

  protected choose(event: Event): void {
    const field = event.target as HTMLInputElement;
    this.emit(Array.from(field.files ?? []));
    // Même fichier choisi deux fois de suite : l'événement doit repartir
    field.value = '';
  }

  private emit(files: File[]): void {
    const chosen = this.multiple() ? files : files.slice(0, 1);
    if (chosen.length > 0) {
      this.files.emit(chosen);
    }
  }
}
