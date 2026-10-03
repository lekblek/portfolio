import { Injector, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { form } from '@angular/forms/signals';

import { AdminProfile } from '../../../../core/api/api-types';
import {
  EMPTY_PROFILE,
  keepUnchanged,
  newCertification,
  newExperience,
  newSkill,
  ProfileModel,
  profileSchema,
  toModel,
  toRequest,
} from './profile-form';

const SAVED: AdminProfile = {
  displayName: 'Ada Lovelace',
  professionalTitle: 'Développeuse',
  shortBio: 'Calculs et machines.',
  aboutMarkdown: null,
  publicLocation: 'Londres',
  publicEmail: null,
  avatarMediaId: 3,
  cvMediaId: null,
  links: [{ label: 'GitHub', url: 'https://example.test/ada' }],
  skills: [{ name: 'Analyse', category: 'Mathématiques' }],
  experiences: [
    {
      organization: 'Analytical Engine',
      title: 'Programmeuse',
      location: 'Londres',
      startDate: '1842-01-01',
      endDate: null,
      description: '',
    },
  ],
  educations: [],
  certifications: [
    {
      name: 'Logique',
      issuer: 'Société royale',
      issuedAt: '1840-05-01',
      expiresAt: null,
      credentialUrl: null,
    },
  ],
};

function profileForm(model: ProfileModel) {
  const value = signal(model);
  return {
    value,
    tree: form(value, profileSchema, { injector: TestBed.inject(Injector) }),
  };
}

function valid(): ProfileModel {
  return { ...toModel(SAVED) };
}

describe('profile form', () => {
  it('turns a saved profile into a model and back without loss', () => {
    const model = toModel(SAVED);

    expect(model.aboutMarkdown).toBe('');
    expect(model.experiences[0].endDate).toBe('');
    expect(model.certifications[0].credentialUrl).toBe('');
    expect(toRequest(model)).toEqual({
      displayName: 'Ada Lovelace',
      professionalTitle: 'Développeuse',
      shortBio: 'Calculs et machines.',
      aboutMarkdown: undefined,
      publicLocation: 'Londres',
      publicEmail: undefined,
      avatarMediaId: 3,
      cvMediaId: undefined,
      links: SAVED.links,
      skills: SAVED.skills,
      experiences: [{ ...SAVED.experiences[0], endDate: undefined }],
      educations: [],
      certifications: [
        { ...SAVED.certifications[0], expiresAt: undefined, credentialUrl: undefined },
      ],
    });
  });

  it('trims the texts and omits the empty optional ones', () => {
    const request = toRequest({
      ...EMPTY_PROFILE,
      displayName: '  Ada  ',
      aboutMarkdown: '   ',
      skills: [{ name: ' Analyse ', category: ' Maths ' }],
    });

    expect(request.displayName).toBe('Ada');
    expect(request.aboutMarkdown).toBeUndefined();
    expect(request.skills).toEqual([{ name: 'Analyse', category: 'Maths' }]);
  });

  it('accepts a complete profile', () => {
    expect(profileForm(valid()).tree().valid()).toBe(true);
  });

  it('requires the identity and refuses blank texts', () => {
    const { tree } = profileForm({ ...valid(), displayName: '   ', professionalTitle: '' });

    expect(tree.displayName().errors()[0]?.message).toBe('Indiquez le nom affiché.');
    expect(tree.professionalTitle().errors()[0]?.message).toBe('Indiquez le titre professionnel.');
  });

  it('refuses a period that ends before it starts', () => {
    const { tree } = profileForm({
      ...valid(),
      experiences: [
        {
          ...newExperience(),
          organization: 'A',
          title: 'B',
          location: 'C',
          startDate: '2024-05-01',
          endDate: '2023-01-01',
        },
      ],
      certifications: [
        {
          ...newCertification(),
          name: 'N',
          issuer: 'O',
          issuedAt: '2024-05-01',
          expiresAt: '2024-04-30',
        },
      ],
    });

    expect(tree.experiences[0].endDate().errors()[0]?.message).toBe('La fin précède le début.');
    expect(tree.certifications[0].expiresAt().errors()[0]?.message).toBe(
      'L’expiration précède la délivrance.',
    );
  });

  it('refuses the same skill twice, whatever the case', () => {
    const { tree } = profileForm({
      ...valid(),
      skills: [
        { name: 'Angular', category: 'Frontend' },
        { ...newSkill(), name: ' angular ', category: 'Web' },
      ],
    });

    expect(tree.skills[1].name().errors()[0]?.message).toBe(
      'Cette compétence figure déjà dans la liste.',
    );
  });

  it('requires web addresses for links', () => {
    const { tree } = profileForm({
      ...valid(),
      links: [{ label: 'Site', url: 'example.test' }],
    });

    expect(tree.links[0].url().valid()).toBe(false);
  });

  it('keeps the unchanged list items, so their rows survive a save', () => {
    const previous = toModel(SAVED);
    const next = toModel({
      ...SAVED,
      links: [...SAVED.links, { label: 'Blog', url: 'https://example.test/blog' }],
      skills: [{ name: 'Analyse', category: 'Logique' }],
    });

    const merged = keepUnchanged(previous, next);

    expect(merged).toEqual(next);
    expect(merged.links[0]).toBe(previous.links[0]);
    expect(merged.links[1]).toBe(next.links[1]);
    expect(merged.skills[0]).toBe(next.skills[0]);
    expect(merged.experiences[0]).toBe(previous.experiences[0]);
  });
});
