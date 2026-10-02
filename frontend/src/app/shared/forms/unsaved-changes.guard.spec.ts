import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';

import { UnsavedChanges, unsavedChangesGuard } from './unsaved-changes.guard';

function leave(page: UnsavedChanges) {
  return TestBed.runInInjectionContext(() =>
    unsavedChangesGuard(
      page,
      {} as ActivatedRouteSnapshot,
      {} as RouterStateSnapshot,
      {} as RouterStateSnapshot,
    ),
  );
}

describe('unsavedChangesGuard', () => {
  it('lets the navigation go when the page allows it', () => {
    expect(leave({ canLeave: () => true })).toBe(true);
  });

  it('waits for the answer of the page', async () => {
    let answer: (stay: boolean) => void = () => undefined;
    const pending = leave({
      canLeave: () => new Promise<boolean>((resolve) => (answer = resolve)),
    });

    answer(false);

    expect(await pending).toBe(false);
  });
});
