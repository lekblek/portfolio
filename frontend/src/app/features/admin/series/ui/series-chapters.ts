import { LiveAnnouncer } from '@angular/cdk/a11y';
import { HttpClient } from '@angular/common/http';
import {
  afterNextRender,
  Component,
  computed,
  effect,
  ElementRef,
  inject,
  Injector,
  input,
  output,
  signal,
} from '@angular/core';

import { toApiError } from '../../../../core/api/api-error';
import {
  AdminPublicationSummary,
  AdminSeries,
  PublicationStatus,
} from '../../../../core/api/api-types';
import { Alert } from '../../../../shared/ui/alert';
import { Button } from '../../../../shared/ui/button';
import {
  moveItem,
  SortableItem,
  SortableList,
  SortableMove,
} from '../../../../shared/ui/sortable-list';
import { StatusBadge } from '../../../../shared/ui/status-badge';
import { replaceChapters } from '../data/series';

/** Chapitre en cours de saisie : l'article et ce qu'il faut en afficher. */
interface ChapterItem {
  publicationId: number;
  title: string;
  status: PublicationStatus;
}

/**
 * Chapitres d'une série (F29, D-CV) : liste ordonnable (ordre de lecture), ajout d'un article par
 * une liste déroulante (les articles déjà rangés ici en sont retirés), retrait ; la liste entière
 * remplace l'ancienne à l'enregistrement. Un article d'une autre série est refusé par le serveur
 * (`ARTICLE_ALREADY_IN_SERIES`), avec l'explication.
 */
@Component({
  selector: 'app-series-chapters',
  imports: [Alert, Button, SortableItem, SortableList, StatusBadge],
  template: `
    <section class="chapters" aria-labelledby="chapitres-titre">
      <h2 id="chapitres-titre" class="text-xl tracking-heading">Chapitres</h2>
      <p class="mt-1 max-w-prose text-sm text-ink-muted">
        Dans l’ordre de lecture. La série paraît sur le site dès qu’un de ses articles y est
        visible.
      </p>

      @if (chapters().length === 0) {
        <p class="mt-4 text-ink-muted">Aucun chapitre.</p>
      } @else {
        <app-sortable-list class="mt-4" label="Chapitres" (moved)="move($event)">
          @for (chapter of chapters(); track chapter.publicationId; let i = $index) {
            <app-sortable-item
              [index]="i"
              [count]="chapters().length"
              [name]="chapterName(chapter)"
              (moveTo)="move($event)"
              (remove)="remove(i)"
            >
              <p class="flex flex-wrap items-center gap-x-3 gap-y-1">
                <span class="text-sm text-ink-muted">Chapitre {{ i + 1 }}</span>
                <span class="font-semibold">{{ chapter.title }}</span>
                <app-status-badge [status]="chapter.status" />
              </p>
            </app-sortable-item>
          }
        </app-sortable-list>
      }

      <div class="chapters-add">
        <div class="min-w-0">
          <label class="block font-semibold" for="chapitre-ajout">Ajouter un article</label>
          <select
            id="chapitre-ajout"
            class="field-control mt-2"
            [value]="choice()"
            (change)="choice.set($any($event.target).value)"
          >
            <option value="">Choisir un article</option>
            @for (article of available(); track article.id) {
              <option [value]="'' + article.id">{{ article.title }}</option>
            }
          </select>
        </div>
        <button
          appButton
          type="button"
          variant="secondary"
          [disabled]="choice() === ''"
          (click)="add()"
        >
          Ajouter
        </button>
      </div>

      @if (failure(); as text) {
        <app-alert class="mt-4 block" tone="danger" title="Les chapitres n’ont pas changé">
          <p>{{ text }}</p>
        </app-alert>
      }
      @if (dirty()) {
        <div class="mt-4 flex flex-wrap gap-3">
          <button appButton type="button" [loading]="busy()" (click)="save()">
            Enregistrer les chapitres
          </button>
          <button appButton type="button" variant="secondary" (click)="reset()">
            Annuler les changements
          </button>
        </div>
      }
    </section>
  `,
  styles: `
    .chapters-add {
      display: grid;
      gap: calc(var(--spacing) * 3);
      align-items: end;
      margin-block-start: calc(var(--spacing) * 6);
    }

    @media (width >= 40rem) {
      .chapters-add {
        grid-template-columns: minmax(0, 1fr) auto;
      }
    }
  `,
})
export class SeriesChapters {
  readonly series = input.required<AdminSeries>();
  readonly articles = input<readonly AdminPublicationSummary[]>([]);
  /** Série après l'enregistrement des chapitres. */
  readonly saved = output<AdminSeries>();

  protected readonly chapters = signal<ChapterItem[]>([]);
  protected readonly choice = signal('');
  protected readonly busy = signal(false);
  protected readonly failure = signal<string | null>(null);

  /** Chapitres saisis différents des chapitres enregistrés. */
  readonly dirty = computed(
    () =>
      this.chapters()
        .map((chapter) => chapter.publicationId)
        .join(',') !==
      this.series()
        .chapters.map((chapter) => chapter.publicationId)
        .join(','),
  );
  protected readonly available = computed(() => {
    const chosen = new Set(this.chapters().map((chapter) => chapter.publicationId));
    return this.articles().filter((article) => !chosen.has(article.id));
  });

  private readonly host: HTMLElement = inject(ElementRef).nativeElement;
  private readonly http = inject(HttpClient);
  private readonly announcer = inject(LiveAnnouncer);
  private readonly injector = inject(Injector);

  private readonly savedChapters = computed(() =>
    this.series().chapters.map((chapter) => ({
      publicationId: chapter.publicationId,
      title: chapter.title,
      status: chapter.status,
    })),
  );

  constructor() {
    // Chapitres enregistrés : point de départ, et de nouveau après chaque enregistrement
    effect(() => this.chapters.set(this.savedChapters()));
  }

  protected chapterName(chapter: ChapterItem): string {
    return `le chapitre «\u00a0${chapter.title}\u00a0»`;
  }

  protected move(move: SortableMove): void {
    this.chapters.update((list) => moveItem(list, move));
  }

  protected add(): void {
    const article = this.articles().find((candidate) => String(candidate.id) === this.choice());
    if (!article) {
      return;
    }
    this.chapters.update((list) => [
      ...list,
      { publicationId: article.id, title: article.title, status: article.status },
    ]);
    this.choice.set('');
    void this.announcer.announce(
      `«\u00a0${article.title}\u00a0» ajouté en chapitre ${this.chapters().length}.`,
    );
  }

  protected remove(index: number): void {
    const removed = this.chapters()[index];
    this.chapters.update((list) => list.filter((_, i) => i !== index));
    void this.announcer.announce(
      `«\u00a0${removed.title}\u00a0» retiré. Enregistrez les chapitres pour l’ôter de la série.`,
    );
    // L'élément a disparu : le focus va à l'ajout, voisin stable de la liste
    afterNextRender(() => this.host.querySelector<HTMLElement>('#chapitre-ajout')?.focus(), {
      injector: this.injector,
    });
  }

  protected reset(): void {
    this.chapters.set(this.savedChapters());
    this.failure.set(null);
  }

  protected async save(): Promise<void> {
    this.busy.set(true);
    this.failure.set(null);
    try {
      const updated = await replaceChapters(
        this.http,
        this.series().id,
        this.chapters().map((chapter) => chapter.publicationId),
      );
      this.saved.emit(updated);
    } catch (error) {
      const code = toApiError(error).code;
      this.failure.set(
        code === 'ARTICLE_ALREADY_IN_SERIES'
          ? 'Un de ces articles appartient déjà à une autre série : retirez-le de celle-ci, ou de l’autre série d’abord.'
          : code === 'NEWS_CANNOT_JOIN_SERIES'
            ? 'Une actualité ne peut pas devenir un chapitre.'
            : 'Le serveur ne répond pas pour le moment : réessayez dans quelques instants.',
      );
    } finally {
      this.busy.set(false);
    }
  }
}
