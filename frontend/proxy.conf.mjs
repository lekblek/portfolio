// Proxy du serveur de développement : le navigateur appelle /api en relatif, comme en production.
// API_ORIGIN désigne le backend (défaut : celui lancé depuis l'IDE sur le port 8080).
export default {
  '/api': {
    target: process.env.API_ORIGIN ?? 'http://localhost:8080',
    secure: false,
  },
};
