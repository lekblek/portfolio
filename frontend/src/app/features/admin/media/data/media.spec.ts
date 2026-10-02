import { IMAGE_MAX_BYTES, isImage, PDF_MAX_BYTES, uploadProblem } from './media';

function file(name: string, type: string, size: number): File {
  const content = new File(['x'], name, { type });
  Object.defineProperty(content, 'size', { value: size });
  return content;
}

describe('uploadProblem', () => {
  it('accepts the four formats within their size', () => {
    expect(uploadProblem(file('a.png', 'image/png', IMAGE_MAX_BYTES))).toBeNull();
    expect(uploadProblem(file('a.jpg', 'image/jpeg', 1000))).toBeNull();
    expect(uploadProblem(file('a.webp', 'image/webp', 1000))).toBeNull();
    expect(uploadProblem(file('cv.pdf', 'application/pdf', PDF_MAX_BYTES))).toBeNull();
  });

  it('recognizes the format by its extension when the system gives no type', () => {
    expect(uploadProblem(file('photo.JPEG', '', 1000))).toBeNull();
  });

  it('refuses another format, an empty file and a file too heavy for its format', () => {
    expect(uploadProblem(file('a.svg', 'image/svg+xml', 1000))).toContain('Format refusé');
    expect(uploadProblem(file('a.gif', 'image/gif', 1000))).toContain('Format refusé');
    expect(uploadProblem(file('a.png', 'image/png', 0))).toBe('Fichier vide.');
    expect(uploadProblem(file('a.png', 'image/png', IMAGE_MAX_BYTES + 1))).toContain(
      '5\u00a0Mo au plus pour une image',
    );
    expect(uploadProblem(file('cv.pdf', 'application/pdf', PDF_MAX_BYTES + 1))).toContain(
      '10\u00a0Mo au plus pour un PDF',
    );
  });
});

describe('isImage', () => {
  it('tells an image from a PDF', () => {
    expect(isImage({ format: 'WEBP' })).toBe(true);
    expect(isImage({ format: 'PDF' })).toBe(false);
  });
});
