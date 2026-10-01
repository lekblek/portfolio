import { components } from './openapi';

/**
 * Schémas du contrat (`docs/api/openapi.json`), générés par `npm run api:types`.
 * Chaque fonctionnalité ajoute ici les alias qu'elle utilise :
 * `export type ProjectSummary = Schemas['ProjectSummaryResponse'];`
 */
export type Schemas = components['schemas'];

export type Profile = Schemas['ProfileResponse'];
export type ProfileExperience = Schemas['ExperienceResponse'];
export type ProfileEducation = Schemas['EducationResponse'];
export type ProfileCertification = Schemas['CertificationResponse'];
export type SkillGroup = Schemas['SkillGroupResponse'];
export type ProfessionalLink = Schemas['ProfessionalLinkResponse'];
export type PublicImage = Schemas['PublicImage'];
export type PublicDocument = Schemas['PublicDocument'];

export type ProjectSummary = Schemas['ProjectSummaryResponse'];
export type Project = Schemas['ProjectResponse'];
export type ProjectStage = Project['stage'];

export type PublicationSummary = Schemas['PublicationSummaryResponse'];
export type Publication = Schemas['PublicationResponse'];
export type PublicationType = Publication['type'];

export type SeriesSummary = Schemas['SeriesSummaryResponse'];
export type Series = Schemas['SeriesResponse'];
export type SeriesChapter = Schemas['SeriesChapterResponse'];
export type SeriesNavigation = Schemas['SeriesNavigationResponse'];

export type SearchResult = Schemas['SearchResultResponse'];
export type SearchResultType = SearchResult['type'];

export type ContactMessageRequest = Schemas['SubmitContactMessageRequest'];
