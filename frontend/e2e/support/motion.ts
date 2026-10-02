import { Page } from '@playwright/test';

interface MotionLog {
  __motion: string[];
}

/**
 * Enregistre, dès le chargement de chaque document, les transitions et animations CSS qui
 * démarrent (`transitionrun`, `animationstart`) : « élément propriété ». À appeler avant `goto`.
 */
export async function recordMotionFromLoad(page: Page): Promise<() => Promise<string[]>> {
  await page.addInitScript(() => {
    const log = window as unknown as MotionLog;
    log.__motion = [];
    const record = (target: EventTarget | null, what: string) =>
      log.__motion.push(`${(target as Element | null)?.localName ?? '?'} ${what}`);
    addEventListener('transitionrun', (event) => record(event.target, event.propertyName), true);
    addEventListener('animationstart', (event) => record(event.target, event.animationName), true);
  });
  return () => page.evaluate(() => (window as unknown as MotionLog).__motion);
}

/**
 * Enregistre les propriétés des transitions CSS qui démarrent sur les éléments désignés par
 * `selector`, à partir de maintenant. Retourne la lecture : propriétés triées, sans doublon, après
 * deux images.
 */
export async function recordTransitions(
  page: Page,
  selector: string,
): Promise<() => Promise<string[]>> {
  await page.evaluate((target) => {
    const log = window as unknown as MotionLog;
    log.__motion = [];
    addEventListener(
      'transitionrun',
      (event) => {
        if ((event.target as Element).matches(target)) {
          log.__motion.push(event.propertyName);
        }
      },
      true,
    );
  }, selector);
  // Les événements d'animation partent à l'image suivante : deux images attendues avant la lecture
  return () =>
    page.evaluate(async () => {
      for (let frame = 0; frame < 2; frame++) {
        await new Promise((resolve) => requestAnimationFrame(resolve));
      }
      return [...new Set((window as unknown as MotionLog).__motion)].sort();
    });
}
