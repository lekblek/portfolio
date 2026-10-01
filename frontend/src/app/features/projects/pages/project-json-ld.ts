import { Project } from '../../../core/api/api-types';
import { projectStageLabel } from '../../../shared/content/project-entry';

/**
 * Données structurées d'un projet : `CreativeWork`, le type de schema.org qui décrit une œuvre
 * produite (ici un projet logiciel), avec seulement les données publiées. `SoftwareSourceCode`
 * n'est pas retenu : la page décrit le projet, pas son code, et le dépôt est souvent absent.
 * `absolute` rend absolue une adresse du site.
 */
export function projectJsonLd(project: Project, absolute: (path: string) => string): object {
  const work: Record<string, unknown> = {
    '@context': 'https://schema.org',
    '@type': 'CreativeWork',
    name: project.title,
    description: project.shortDescription,
    url: absolute(`/projects/${project.slug}`),
    inLanguage: 'fr',
    dateCreated: project.startDate,
    creativeWorkStatus: projectStageLabel(project.stage),
  };
  if (project.cover) {
    work['image'] = absolute(project.cover.url);
  }
  if (project.technologies.length > 0) {
    work['keywords'] = project.technologies.map((technology) => technology.name).join(', ');
  }
  return work;
}
