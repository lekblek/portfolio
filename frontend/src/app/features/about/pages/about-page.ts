import { Component, computed, effect, inject } from '@angular/core';

import { toApiError } from '../../../core/api/api-error';
import { injectResponseStatus } from '../../../core/platform/response-status';
import { Seo } from '../../../core/seo/seo';
import { MarkdownView } from '../../../shared/markdown/markdown-view';
import { EmptyState } from '../../../shared/ui/empty-state';
import { ErrorState } from '../../../shared/ui/error-state';
import { profileResource } from '../data/profile.resource';
import { CertificationList } from '../ui/certification-list';
import { EducationList } from '../ui/education-list';
import { ExperienceList } from '../ui/experience-list';
import { ProfileIntro } from '../ui/profile-intro';
import { ProfileSection } from '../ui/profile-section';
import { SkillGroups } from '../ui/skill-groups';
import { profilePageJsonLd } from './profile-json-ld';

const PATH = '/about';
const TITLE = 'À propos';

/**
 * Page À propos : le profil public, rendu par le serveur. Une collection vide ne produit aucune
 * zone ; un profil jamais enregistré donne une page introuvable (404) ; une API indisponible,
 * un état d'erreur avec « Réessayer » (503 au rendu serveur, pour qu'aucune erreur ne soit
 * indexée ni mise en cache comme une page valide).
 */
@Component({
  selector: 'app-about-page',
  imports: [
    CertificationList,
    EducationList,
    EmptyState,
    ErrorState,
    ExperienceList,
    MarkdownView,
    ProfileIntro,
    ProfileSection,
    SkillGroups,
  ],
  host: { class: 'block page-container wrap-break-word' },
  template: `
    @if (profile.hasValue()) {
      @let current = profile.value();
      <app-profile-intro [profile]="current" />
      @if (current.aboutMarkdown?.trim(); as about) {
        <app-profile-section heading="Présentation" headingId="zone-presentation">
          <app-markdown-view [source]="about" [headingLevel]="3" />
        </app-profile-section>
      }
      @if (current.experiences.length > 0) {
        <app-profile-section heading="Expériences" headingId="zone-experiences">
          <app-experience-list [experiences]="current.experiences" />
        </app-profile-section>
      }
      @if (current.educations.length > 0) {
        <app-profile-section heading="Formation" headingId="zone-formation">
          <app-education-list [educations]="current.educations" />
        </app-profile-section>
      }
      @if (current.skillGroups.length > 0) {
        <app-profile-section heading="Compétences" headingId="zone-competences">
          <app-skill-groups [groups]="current.skillGroups" />
        </app-profile-section>
      }
      @if (current.certifications.length > 0) {
        <app-profile-section heading="Certifications" headingId="zone-certifications">
          <app-certification-list [certifications]="current.certifications" />
        </app-profile-section>
      }
    } @else if (error(); as failure) {
      <div class="grid gap-3 py-section lg:grid-cols-12 lg:gap-8">
        <p class="text-sm font-semibold text-ink-muted lg:col-span-3 lg:pt-3">À propos</p>
        <div class="lg:col-span-9">
          @if (failure.status === 404) {
            <h1 class="text-3xl leading-tight tracking-title">Profil non publié</h1>
            <div class="mt-block">
              <app-empty-state
                message="La présentation, le parcours et les compétences apparaîtront ici dès la publication du profil."
              />
            </div>
          } @else {
            <h1 class="text-3xl leading-tight tracking-title">Profil indisponible</h1>
            <div class="mt-block">
              <app-error-state
                title="Le profil n’a pas pu être chargé."
                [detail]="failure.detail"
                (retry)="profile.reload()"
              />
            </div>
          }
        </div>
      </div>
    } @else {
      <!-- Navigation dans le navigateur seulement : le rendu serveur attend la réponse -->
      @defer (on timer(300ms)) {
        <p role="status" class="py-section text-ink-muted">Chargement du profil…</p>
      }
    }
  `,
})
export class AboutPage {
  protected readonly profile = profileResource();
  protected readonly error = computed(() =>
    this.profile.error() ? toApiError(this.profile.error()) : null,
  );

  private readonly seo = inject(Seo);
  private readonly setResponseStatus = injectResponseStatus();

  constructor() {
    effect(() => {
      if (this.profile.hasValue()) {
        const profile = this.profile.value();
        this.seo.set({
          title: TITLE,
          description: profile.shortBio,
          path: PATH,
          type: 'profile',
          image: profile.avatar?.url ?? null,
          jsonLd: profilePageJsonLd(profile, (path) => this.seo.absolute(path)),
        });
        return;
      }
      const failure = this.error();
      if (failure?.status === 404) {
        this.setResponseStatus(404);
        this.seo.set({ title: TITLE, path: PATH, noindex: true });
      } else if (failure) {
        this.setResponseStatus(503);
      }
    });
  }
}
