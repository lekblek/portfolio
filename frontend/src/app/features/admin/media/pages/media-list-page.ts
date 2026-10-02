import { HttpClient, HttpEventType } from '@angular/common/http';
import { Component, computed, ElementRef, inject, input, signal, viewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { toApiError } from '../../../../core/api/api-error';
import { AdminMedia } from '../../../../core/api/api-types';
import { UnsavedChanges } from '../../../../shared/forms/unsaved-changes.guard';
import { formatDay } from '../../../../shared/format/date';
import { formatFileSize } from '../../../../shared/format/file-size';
import { Button } from '../../../../shared/ui/button';
import { ConfirmDialog } from '../../../../shared/ui/confirm-dialog';
import { DataTable } from '../../../../shared/ui/data-table';
import { EmptyState } from '../../../../shared/ui/empty-state';
import { ErrorState } from '../../../../shared/ui/error-state';
import { FileDrop } from '../../../../shared/ui/file-drop';
import { Icon } from '../../../../shared/ui/icon';
import { Pagination } from '../../../../shared/ui/pagination';
import { Toaster } from '../../../../shared/ui/toast';
import {
  ACCEPT,
  deleteMedia,
  isImage,
  mediaPageResource,
  uploadMedia,
  uploadProblem,
} from '../data/media';

type UploadStatus = 'waiting' | 'sending' | 'done' | 'failed';

const FORMAT_SHORT: Record<AdminMedia['format'], string> = {
  PNG: 'PNG',
  JPEG: 'JPEG',
  WEBP: 'WebP',
  PDF: 'PDF',
};

interface Upload {
  id: number;
  name: string;
  size: number;
  status: UploadStatus;
  /** Pourcentage envoyé, de 0 à 100. */
  progress: number;
  /** Raison d'un refus. */
  message: string | null;
  /** Média créé : une image sans texte alternatif invite à l'écrire. */
  media: AdminMedia | null;
}

/**
 * Médiathèque (`/admin/media`) : zone d'envoi (bouton ou dépôt de fichiers, plusieurs à la fois),
 * file des envois avec leur progression, puis tableau paginé des médias, les plus récents d'abord.
 * Chaque fichier est vérifié avant l'envoi (format, taille) puis par le serveur (signature, 413,
 * 415). Une image sans texte alternatif est signalée et mène à sa page. Suppression confirmée ;
 * un média utilisé est refusé par l'API (`MEDIA_STILL_REFERENCED`). Quitter la page pendant un
 * envoi demande confirmation.
 */
@Component({
  selector: 'app-media-list-page',
  imports: [
    Button,
    ConfirmDialog,
    DataTable,
    EmptyState,
    ErrorState,
    FileDrop,
    Icon,
    Pagination,
    RouterLink,
  ],
  host: { '(window:beforeunload)': 'warnBeforeUnload($event)' },
  template: `
    <h1 #heading class="text-2xl tracking-heading" tabindex="-1">Médias</h1>

    <app-file-drop
      class="mt-block block max-w-prose"
      label="Ajouter des fichiers"
      hint="PNG, JPEG ou WebP jusqu’à 5&#160;Mo, PDF jusqu’à 10&#160;Mo. Plusieurs fichiers à la fois."
      buttonLabel="Choisir des fichiers"
      [accept]="accept"
      multiple
      (files)="upload($event)"
    />
    <p class="sr-only" aria-live="polite">{{ announcement() }}</p>

    @if (uploads().length > 0) {
      <section class="mt-block max-w-prose" aria-labelledby="envois">
        <div class="flex flex-wrap items-baseline justify-between gap-3">
          <h2 id="envois" class="text-sm font-semibold text-ink-muted">Envois</h2>
          @if (!busy()) {
            <button appButton type="button" variant="quiet" size="sm" (click)="uploads.set([])">
              Effacer la liste des envois
            </button>
          }
        </div>
        <ul class="upload-list">
          @for (item of uploads(); track item.id) {
            <li class="upload-item">
              <p class="min-w-0 font-medium wrap-break-word">
                {{ item.name }}
                <span class="text-sm font-normal text-ink-muted">{{ size(item.size) }}</span>
              </p>
              @switch (item.status) {
                @case ('waiting') {
                  <p class="text-sm text-ink-muted">En attente</p>
                }
                @case ('sending') {
                  <div class="flex items-center gap-3">
                    <progress
                      class="upload-progress"
                      max="100"
                      [value]="item.progress"
                      [attr.aria-label]="'Envoi de ' + item.name"
                    ></progress>
                    <span class="text-sm tabular-nums">{{ item.progress }}&#8239;%</span>
                  </div>
                }
                @case ('done') {
                  <p class="text-sm text-success">
                    Envoyé.
                    @if (item.media && needsAltText(item.media)) {
                      <a [routerLink]="['/admin/media', item.media.id]"
                        >Écrire le texte alternatif<span class="sr-only">
                          de {{ item.name }}</span
                        ></a
                      >
                    }
                  </p>
                }
                @case ('failed') {
                  <p class="text-sm text-danger">{{ item.message }}</p>
                }
              }
            </li>
          }
        </ul>
      </section>
    }

    @if (media.error(); as error) {
      <app-error-state
        class="mt-block block"
        [title]="'La médiathèque n’a pas pu être chargée.'"
        [detail]="detailOf(error)"
        (retry)="media.reload()"
      />
    } @else if (media.hasValue()) {
      @let current = media.value();
      @if (current.content.length === 0) {
        @if (currentPage() > 1) {
          <app-empty-state class="mt-block block" message="Cette page de la médiathèque est vide.">
            <a routerLink="/admin/media">Première page</a>
          </app-empty-state>
        } @else {
          <app-empty-state
            class="mt-block block"
            message="Aucun média pour le moment&#8239;: ajoutez un premier fichier."
          />
        }
      } @else {
        <p class="mt-block text-sm text-ink-muted">{{ countText() }}</p>
        <app-data-table class="mt-3" label="Médias">
          <table class="data-table">
            <caption class="sr-only">
              Médias, les plus récents d’abord
            </caption>
            <thead>
              <tr>
                <th scope="col"><span class="sr-only">Aperçu</span></th>
                <th scope="col">Fichier</th>
                <th scope="col">Détails</th>
                <th scope="col">Texte alternatif</th>
                <th scope="col">Envoyé le</th>
                <th scope="col" class="data-table-actions"><span class="sr-only">Actions</span></th>
              </tr>
            </thead>
            <tbody>
              @for (item of current.content; track item.id) {
                <tr>
                  <td class="media-thumb-cell">
                    @if (isImage(item)) {
                      <img
                        class="media-thumb"
                        [src]="item.url"
                        [width]="item.width"
                        [height]="item.height"
                        alt=""
                        loading="lazy"
                        decoding="async"
                      />
                    } @else {
                      <span class="media-thumb media-thumb-file"
                        ><app-icon name="file-text"
                      /></span>
                    }
                  </td>
                  <th scope="row" class="media-name">{{ item.originalName }}</th>
                  <td class="text-sm whitespace-nowrap text-ink-muted">{{ details(item) }}</td>
                  <td class="media-alt text-sm">
                    @if (!isImage(item)) {
                      <span class="text-ink-muted">—</span>
                    } @else if (item.altText) {
                      {{ item.altText }}
                    } @else {
                      <span class="media-missing">
                        <app-icon name="circle-alert" />
                        Manquant
                      </span>
                    }
                  </td>
                  <td class="text-sm whitespace-nowrap">{{ day(item.createdAt) }}</td>
                  <td class="data-table-actions">
                    <a appButton variant="quiet" size="sm" [routerLink]="['/admin/media', item.id]"
                      >Modifier<span class="sr-only">
                        le média {{ quoted(item.originalName) }}</span
                      ></a
                    >
                    <button
                      appButton
                      type="button"
                      variant="quiet"
                      size="sm"
                      (click)="remove(item)"
                    >
                      Supprimer<span class="sr-only">
                        le média {{ quoted(item.originalName) }}</span
                      >
                    </button>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </app-data-table>
        <app-pagination
          class="mt-block block"
          label="Pages de la médiathèque"
          [page]="currentPage()"
          [totalPages]="current.totalPages"
        />
      }
    } @else {
      @defer (on timer(300ms)) {
        <p role="status" class="mt-block text-ink-muted">Chargement…</p>
      }
    }

    <app-confirm-dialog #confirm />
  `,
  styles: `
    .upload-list {
      margin-block-start: calc(var(--spacing) * 2);
      border-top: var(--border-rule) solid var(--color-rule);
    }

    .upload-item {
      display: grid;
      gap: calc(var(--spacing) * 1);
      padding-block: calc(var(--spacing) * 3);
      border-bottom: var(--border-rule) solid var(--color-rule);
    }

    .upload-progress {
      width: calc(var(--spacing) * 48);
      accent-color: var(--color-accent);
    }

    .media-thumb-cell {
      width: calc(var(--spacing) * 16);
    }

    .media-thumb {
      display: flex;
      align-items: center;
      justify-content: center;
      width: calc(var(--spacing) * 16);
      height: calc(var(--spacing) * 12);
      object-fit: contain;
      border: var(--border-rule) solid var(--color-rule);
      border-radius: var(--radius-media);
      background: var(--color-paper-sunken);
    }

    .media-thumb-file {
      color: var(--color-ink-muted);
    }

    .media-name {
      max-width: calc(var(--spacing) * 64);
      overflow-wrap: anywhere;
    }

    .media-alt {
      max-width: calc(var(--spacing) * 72);
    }

    .media-missing {
      display: inline-flex;
      align-items: center;
      gap: calc(var(--spacing) * 1);
      color: var(--color-warning);
      font-weight: var(--font-weight-medium);
    }
  `,
})
export class MediaListPage implements UnsavedChanges {
  /** `?page=` de l'URL (base 1). */
  readonly page = input<string>();

  protected readonly accept = ACCEPT;
  protected readonly isImage = isImage;
  protected readonly size = formatFileSize;
  protected readonly day = formatDay;

  protected readonly currentPage = computed(() => {
    const page = Number(this.page());
    return Number.isInteger(page) && page >= 1 ? page : 1;
  });
  protected readonly media = mediaPageResource(() => ({ page: this.currentPage() }));
  protected readonly countText = computed(() => {
    const total = this.media.hasValue() ? this.media.value().totalElements : 0;
    return `${total} ${total === 1 ? 'média' : 'médias'}`;
  });

  protected readonly uploads = signal<readonly Upload[]>([]);
  protected readonly busy = computed(() =>
    this.uploads().some((item) => item.status === 'waiting' || item.status === 'sending'),
  );
  protected readonly announcement = signal('');

  private readonly confirm = viewChild.required<ConfirmDialog>('confirm');
  private readonly heading = viewChild.required<ElementRef<HTMLElement>>('heading');
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly toaster = inject(Toaster);
  private nextUpload = 0;

  protected needsAltText(media: AdminMedia): boolean {
    return isImage(media) && !media.altText;
  }

  protected quoted(name: string): string {
    return `«\u00a0${name}\u00a0»`;
  }

  protected details(media: AdminMedia): string {
    const parts = [FORMAT_SHORT[media.format], formatFileSize(media.sizeBytes)];
    if (media.width !== null && media.height !== null) {
      parts.splice(1, 0, `${media.width}\u00a0×\u00a0${media.height}`);
    }
    return parts.join(' · ');
  }

  protected detailOf(error: unknown): string | null {
    return toApiError(error).detail;
  }

  /** Envoi un par un : la progression de chacun reste lisible, l'ordre de la liste aussi. */
  protected async upload(files: File[]): Promise<void> {
    const items: Upload[] = files.map((file) => {
      const problem = uploadProblem(file);
      return {
        id: ++this.nextUpload,
        name: file.name,
        size: file.size,
        status: problem ? 'failed' : 'waiting',
        progress: 0,
        message: problem,
        media: null,
      };
    });
    this.uploads.update((current) => [...items, ...current]);
    const toSend = items.filter((item) => item.status === 'waiting');
    if (toSend.length > 0) {
      this.announcement.set(
        `Envoi de ${toSend.length} ${toSend.length === 1 ? 'fichier' : 'fichiers'} en cours.`,
      );
    }
    for (const item of toSend) {
      await this.send(item, files[items.indexOf(item)]);
    }
    this.report(items);
    if (toSend.length > 0) {
      await this.showNewest();
    }
  }

  canLeave(): boolean | Promise<boolean> {
    if (!this.busy()) {
      return true;
    }
    return this.confirm().ask({
      title: 'Quitter pendant l’envoi\u202f?',
      message: 'Les fichiers pas encore envoyés ne le seront pas.',
      confirmLabel: 'Quitter la page',
      cancelLabel: 'Rester sur la page',
    });
  }

  protected warnBeforeUnload(event: BeforeUnloadEvent): void {
    if (this.busy()) {
      event.preventDefault();
    }
  }

  protected async remove(media: AdminMedia): Promise<void> {
    const name = this.quoted(media.originalName);
    const confirmed = await this.confirm().ask({
      title: `Supprimer le média ${name}\u202f?`,
      message:
        `Le fichier ${name} sera supprimé définitivement. La suppression est refusée tant qu’il ` +
        'est utilisé par le profil, un projet, une publication ou une série.',
      confirmLabel: 'Supprimer le média',
    });
    if (!confirmed) {
      return;
    }
    try {
      await deleteMedia(this.http, media.id);
      this.toaster.show(`Média ${name} supprimé.`);
    } catch (error) {
      this.toaster.show(
        toApiError(error).code === 'MEDIA_STILL_REFERENCED'
          ? `${name} n’a pas été supprimé\u202f: il est encore utilisé par un contenu. Retirez-le de ce contenu, puis réessayez.`
          : `La suppression de ${name} a échoué\u202f: réessayez dans quelques instants.`,
        'danger',
      );
    }
    this.media.reload();
    this.heading().nativeElement.focus();
  }

  private send(item: Upload, file: File): Promise<void> {
    this.patch(item.id, { status: 'sending' });
    return new Promise((resolve) => {
      uploadMedia(this.http, file).subscribe({
        next: (event) => {
          if (event.type === HttpEventType.UploadProgress && event.total) {
            this.patch(item.id, { progress: Math.round((event.loaded / event.total) * 100) });
          } else if (event.type === HttpEventType.Response && event.body) {
            this.patch(item.id, { status: 'done', progress: 100, media: event.body });
          }
        },
        error: (error: unknown) => {
          this.patch(item.id, { status: 'failed', message: refusal(error) });
          resolve();
        },
        complete: () => resolve(),
      });
    });
  }

  private patch(id: number, changes: Partial<Upload>): void {
    this.uploads.update((current) =>
      current.map((item) => (item.id === id ? { ...item, ...changes } : item)),
    );
  }

  /**
   * Bilan de l'envoi : une notification pour les fichiers envoyés ; les refus sont annoncés et
   * restent dans la liste des envois avec leur raison (une notification d'échec, qui reste
   * affichée, ferait double emploi et recouvrirait la page sur un petit écran).
   */
  private report(items: readonly Upload[]): void {
    const results = this.uploads().filter((item) => items.some(({ id }) => id === item.id));
    const sent = results.filter((item) => item.status === 'done');
    const refused = results.length - sent.length;
    if (sent.length > 0) {
      const toDescribe = sent.some((item) => item.media && this.needsAltText(item.media));
      this.toaster.show(
        `${sent.length} ${sent.length === 1 ? 'fichier envoyé' : 'fichiers envoyés'}.` +
          (toDescribe ? ' Écrivez le texte alternatif des images.' : ''),
      );
    }
    this.announcement.set(
      refused > 0
        ? `${refused} ${refused === 1 ? 'fichier refusé' : 'fichiers refusés'}\u202f: la raison est dans la liste des envois.`
        : '',
    );
  }

  /** Les nouveaux médias sont en tête de la première page. */
  private async showNewest(): Promise<void> {
    if (this.currentPage() > 1) {
      await this.router.navigate([], { queryParams: { page: null }, queryParamsHandling: 'merge' });
    } else {
      this.media.reload();
    }
  }
}

function refusal(error: unknown): string {
  switch (toApiError(error).code) {
    case 'MEDIA_TOO_LARGE':
      return 'Refusé par le serveur\u202f: fichier trop lourd pour son format.';
    case 'UNSUPPORTED_MEDIA_FORMAT':
      return 'Refusé par le serveur\u202f: le contenu n’est ni une image PNG, JPEG, WebP, ni un PDF.';
    default:
      return 'L’envoi a échoué\u202f: réessayez dans quelques instants.';
  }
}
