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

export type AdminAccount = Schemas['AdminSessionResponse'];
export type AdminCredentials = Schemas['OpenAdminSessionRequest'];

export type AdminCategory = Schemas['AdminCategoryResponse'];
export type AdminTag = Schemas['AdminTagResponse'];
export type AdminTechnology = Schemas['AdminTechnologyResponse'];
export type SaveCategoryRequest = Schemas['SaveCategoryRequest'];
export type SaveTagRequest = Schemas['SaveTagRequest'];
export type SaveTechnologyRequest = Schemas['SaveTechnologyRequest'];

export type AdminMedia = Schemas['AdminMediaResponse'];
export type MediaFormat = AdminMedia['format'];

export type AdminProfile = Schemas['AdminProfileResponse'];
export type SaveProfileRequest = Schemas['SaveProfileRequest'];

export type AdminProjectSummary = Schemas['AdminProjectSummaryResponse'];
export type AdminProject = Schemas['AdminProjectResponse'];
export type SaveProjectRequest = Schemas['SaveProjectRequest'];
export type ProjectVisibility = AdminProject['visibility'];
