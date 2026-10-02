import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { DataTable } from './data-table';

@Component({
  imports: [DataTable],
  template: `
    <app-data-table label="Tags">
      <table class="data-table">
        <caption>
          Tags
        </caption>
        <thead>
          <tr>
            <th scope="col">Nom</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <th scope="row">Java</th>
          </tr>
        </tbody>
      </table>
    </app-data-table>
  `,
})
class Host {}

describe('DataTable', () => {
  it('frames the table in a named region, out of the tab order while nothing scrolls', async () => {
    const fixture = TestBed.createComponent(Host);
    await fixture.whenStable();
    const frame = (fixture.nativeElement as HTMLElement).querySelector('app-data-table')!;

    expect(frame.getAttribute('role')).toBe('region');
    expect(frame.getAttribute('aria-label')).toBe('Tags');
    expect(frame.classList).toContain('data-table-frame');
    // jsdom ne mesure pas la mise en page : rien ne défile, le cadre n'est pas une étape du clavier
    expect(frame.hasAttribute('tabindex')).toBe(false);
  });
});
