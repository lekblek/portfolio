# Conventions Git — Portfolio

## 1. Objectif

Ce document définit les conventions Git du projet Portfolio.

Les objectifs sont :

* conserver un historique lisible ;
* produire des commits cohérents ;
* faciliter le diagnostic des régressions ;
* travailler de manière proche d’un projet professionnel ;
* éviter l’introduction de secrets ou d’artefacts inutiles ;
* permettre la création propre de releases ;
* rendre le repository présentable lors d’une revue technique ou d’un entretien.

---

# 2. Branches principales

Le repository utilise deux branches permanentes :

```text
main
develop
```

## `main`

`main` représente l’état stable du projet.

Elle doit contenir uniquement du code :

* validé ;
* testé ;
* intégrable ou déployable ;
* correspondant à un état stable.

Les releases sont créées depuis `main`.

Exemple :

```text
v1.0.0
```

Le développement quotidien ne doit pas être réalisé directement sur `main`.

---

## `develop`

`develop` représente la branche d’intégration courante.

Les développements validés y sont regroupés avant leur passage vers `main`.

Flux général :

```text
feature/*
     │
     ▼
  develop
     │
     ▼
   main
     │
     ▼
  release/tag
```

---

# 3. Branches de travail

Lorsque le changement le justifie, créer une branche courte depuis `develop`.

Formats recommandés :

```text
feat/<sujet>
fix/<sujet>
refactor/<sujet>
test/<sujet>
docs/<sujet>
chore/<sujet>
```

Exemples :

```text
feat/project-persistence
feat/publication-lifecycle
feat/admin-login
fix/contact-email-failure
refactor/media-storage
test/project-api
docs/domain-model
chore/repository-setup
```

Les noms :

* utilisent des minuscules ;
* utilisent `-` comme séparateur ;
* restent courts ;
* décrivent le changement.

Éviter :

```text
my-branch
test2
changes
new
final
portfolio-update-final-v2
```

---

# 4. Création d’une branche

Toujours repartir d’un `develop` à jour.

```bash
git switch develop
git pull --ff-only origin develop
git switch -c feat/project-persistence
```

`--ff-only` évite de créer automatiquement un merge commit inattendu pendant un simple `pull`.

---

# 5. Principe d’un commit

Un commit doit représenter **une modification cohérente**.

Un bon commit doit pouvoir répondre à la question :

> Quel changement précis ce commit introduit-il ?

Exemple cohérent :

```text
feat(project): add project persistence
```

Exemple trop large :

```text
feat: add project, articles, authentication, docker and redesign
```

Une étape pédagogique peut produire un commit lorsque du code ou de la documentation a réellement changé.

Il n’est pas nécessaire de créer artificiellement un commit vide pour chaque sous-étape.

---

# 6. Conventional Commits

Le projet utilise Conventional Commits.

Format :

```text
type(scope): description
```

Le `scope` est recommandé lorsqu’il apporte une information utile.

Exemples :

```text
feat(project): add project persistence
fix(contact): preserve message when email fails
test(publication): cover scheduled visibility
refactor(media): isolate storage abstraction
docs(domain): document publication lifecycle
ci(backend): run Maven tests on pull requests
build(frontend): configure production build
chore(repo): add editor configuration
perf(search): add GIN index
```

---

# 7. Types autorisés

## `feat`

Nouvelle fonctionnalité utilisateur ou métier.

```text
feat(publication): add scheduled publishing
```

---

## `fix`

Correction d’un comportement incorrect.

```text
fix(series): prevent news from joining a series
```

---

## `test`

Ajout ou modification de tests sans nouvelle fonctionnalité métier.

```text
test(project): add repository integration tests
```

---

## `refactor`

Modification interne sans changement fonctionnel volontaire.

```text
refactor(media): extract storage port
```

---

## `docs`

Documentation uniquement.

```text
docs: document Git conventions
```

---

## `chore`

Maintenance générale ne correspondant pas aux catégories précédentes.

```text
chore: add editorconfig
```

---

## `ci`

Configuration CI/CD.

```text
ci: add backend verification workflow
```

---

## `build`

Système de build ou dépendances.

```text
build(backend): add Testcontainers dependency
```

---

## `perf`

Optimisation de performance mesurable.

```text
perf(search): add publication search index
```

---

# 8. Description du commit

La description doit :

* être courte ;
* être précise ;
* utiliser l’impératif anglais ;
* commencer par une minuscule ;
* ne pas terminer inutilement par un point.

Préférer :

```text
feat(profile): add skill ordering
```

à :

```text
feat(profile): added some things for skills.
```

---

# 9. Scopes recommandés

Les scopes peuvent suivre les domaines du projet.

Backend :

```text
shared
security
profile
project
publication
series
taxonomy
media
search
contact
```

Autres scopes possibles :

```text
frontend
backend
admin
public
api
db
deploy
docker
docs
repo
ci
```

Le scope ne doit pas devenir obligatoire lorsqu’il n’apporte rien.

Exemple acceptable :

```text
docs: update installation guide
```

---

# 10. Avant chaque commit

Toujours inspecter les modifications.

```bash
git status
git diff
```

Pour les fichiers déjà indexés :

```bash
git diff --staged
```

Vérifier notamment :

```text
[ ] aucun secret
[ ] aucun mot de passe
[ ] aucun token
[ ] aucune clé privée
[ ] aucun fichier .env réel
[ ] aucun fichier généré inutile
[ ] aucun debug temporaire
[ ] aucun fichier IDE inutile
[ ] aucune modification sans rapport avec le commit
[ ] les tests utiles ont été exécutés
```

---

# 11. `git add` ciblé

Préférer l’ajout explicite des fichiers concernés.

Exemple :

```bash
git add backend/src/main/java/...
git add backend/src/test/java/...
```

ou :

```bash
git add docs/02-modele-metier.md docs/conventions-git.md
```

Éviter par réflexe :

```bash
git add .
```

ou :

```bash
git add -A
```

lorsqu’on n’a pas vérifié précisément l’état du repository.

L’objectif est d’éviter d’ajouter accidentellement :

* secrets ;
* logs ;
* fichiers temporaires ;
* données locales ;
* modifications sans rapport.

---

# 12. Vérification avant validation

Séquence recommandée :

```bash
git status
git diff
git add <fichiers ciblés>
git diff --staged
git status
git commit -m "type(scope): description"
```

Le contenu de :

```bash
git diff --staged
```

doit correspondre exactement au commit que l’on souhaite créer.

---

# 13. Push

Une branche de travail est publiée avec :

```bash
git push -u origin feat/project-persistence
```

Les pushes suivants peuvent ensuite utiliser :

```bash
git push
```

Pour `develop` :

```bash
git push origin develop
```

Ne pas effectuer de force push sur les branches partagées principales.

---

# 14. Fichiers interdits dans Git

Ne jamais committer :

```text
.env
.env.production
.env.local contenant des secrets
passwords
credentials
tokens
API keys
clés privées
certificats privés
secrets applicatifs
secrets SMTP
mot de passe PostgreSQL
mot de passe administrateur
```

Exemples :

```text
DATABASE_PASSWORD
ADMIN_PASSWORD
SMTP_PASSWORD
PRIVATE_KEY
API_TOKEN
```

doivent venir d’une configuration externe sécurisée.

---

# 15. Fichiers générés

Ne pas versionner les artefacts pouvant être reconstruits automatiquement.

Exemples généraux :

```text
backend/target/
frontend/node_modules/
frontend/dist/
logs/
coverage/
fichiers temporaires
caches
```

Ils doivent être gérés par `.gitignore`.

---

# 16. IDE et système

Éviter les fichiers locaux spécifiques à une machine lorsque le projet n’en a pas besoin.

Exemples :

```text
.idea/
.vscode/ spécifiques à l'utilisateur
*.iml
.DS_Store
Thumbs.db
```

Une configuration IDE réellement partagée peut être versionnée uniquement si elle apporte une valeur claire au projet.

---

# 17. Variables d’environnement

Un modèle sans secret peut être versionné :

```text
.env.example
```

Exemple :

```text
DATABASE_URL=
DATABASE_USERNAME=
DATABASE_PASSWORD=
ADMIN_USERNAME=
ADMIN_PASSWORD=
SMTP_HOST=
SMTP_PORT=
SMTP_USERNAME=
SMTP_PASSWORD=
```

Il ne contient aucune vraie valeur sensible.

---

# 18. Base de données et Flyway

Les migrations Flyway sont versionnées dans Git.

Exemple :

```text
V1__create_profile.sql
V2__create_projects.sql
```

Une migration déjà appliquée et partagée ne doit normalement pas être réécrite.

Créer une nouvelle migration :

```text
V3__add_project_featured.sql
```

plutôt que modifier silencieusement :

```text
V2__create_projects.sql
```

après son utilisation.

---

# 19. Secrets et migrations

Une migration ne doit jamais contenir :

```text
mot de passe réel
hash administrateur définitif
token
clé API
secret SMTP
```

Le compte administrateur initial doit être créé ou initialisé par une procédure utilisant une configuration sécurisée.

---

# 20. Documentation

La documentation fait partie du projet.

Les changements importants de comportement ou d’architecture doivent mettre à jour les documents concernés.

Exemples :

```text
docs: define V1 business model
docs: document deployment procedure
docs: update publication lifecycle
```

Un changement architectural significatif ne doit pas laisser la documentation volontairement incohérente.

---

# 21. Tests avant commit

Les tests à exécuter dépendent du changement.

## Backend

Lorsqu’un commit touche au backend, la commande de vérification de référence est :

```bash
cd backend
./mvnw verify
```

Sous Windows PowerShell :

```powershell
cd backend
.\mvnw.cmd verify
```

Cette commande exécute :

```text
tests rapides (*Test)
        ↓
Surefire
        ↓
tests d’intégration (*IT)
        ↓
Failsafe
        ↓
PostgreSQL Testcontainers lorsque nécessaire
        ↓
BUILD SUCCESS
```

Pour la boucle de développement rapide, il reste possible d’utiliser :

```bash
./mvnw test
```

mais cette commande ne remplace pas :

```bash
./mvnw verify
```

avant un commit contenant des modifications backend.

## Frontend

Lorsque le frontend est concerné :

```bash
cd frontend
npm test
```

et/ou :

```bash
npm run build
```

selon l’étape.

Un changement purement documentaire ne nécessite pas artificiellement de lancer l’intégralité de la suite applicative.


## Backend

Lorsque le backend est concerné :

```bash
cd backend
./mvnw test
```

ou la commande de vérification définie par le projet.

## Frontend

Lorsque le frontend est concerné :

```bash
cd frontend
npm test
```

et/ou :

```bash
npm run build
```

selon l’étape.

Un changement purement documentaire ne nécessite pas artificiellement de lancer l’intégralité de la suite applicative.

---

# 22. Commit d’une étape pédagogique

Le tutoriel suit généralement le flux :

```text
comprendre
   ↓
modifier
   ↓
tester
   ↓
vérifier
   ↓
git status
   ↓
git diff
   ↓
git add ciblé
   ↓
git diff --staged
   ↓
commit
```

Exemple :

```bash
git add backend/src/main/java/... \
        backend/src/test/java/...

git commit -m "feat(project): add project persistence"
```

---

# 23. Pull Request

Pour une branche fonctionnelle importante, le flux cible est :

```text
feat/*
   ↓
Pull Request
   ↓
develop
```

La Pull Request doit permettre de comprendre :

* le problème traité ;
* le changement réalisé ;
* la manière de le tester ;
* les éventuelles décisions techniques importantes.

Même lorsque le projet est développé seul, cette discipline est utile pour rendre l’historique vérifiable.

---

# 24. Revue d’une Pull Request

Avant fusion :

```text
[ ] le périmètre du changement est clair
[ ] les tests nécessaires passent
[ ] la CI passe
[ ] aucun secret n'est présent
[ ] aucun fichier parasite n'est ajouté
[ ] les migrations sont cohérentes
[ ] les règles métier sont respectées
[ ] la documentation est à jour si nécessaire
[ ] le changement peut être expliqué
```

---

# 25. Fusion vers `develop`

Une branche terminée est intégrée dans :

```text
develop
```

après validation.

Après fusion, la branche courte peut être supprimée.

```bash
git branch -d feat/project-persistence
```

et éventuellement côté distant :

```bash
git push origin --delete feat/project-persistence
```

---

# 26. Passage de `develop` vers `main`

`main` ne doit pas recevoir chaque petite modification quotidienne.

Lorsqu’un ensemble cohérent est stable :

```text
develop
   ↓
validation complète
   ↓
main
```

Avant intégration :

```text
[ ] CI verte
[ ] tests critiques verts
[ ] application compilable
[ ] migrations valides
[ ] documentation cohérente
[ ] aucun secret
```

---

# 27. Protection des branches

Lorsque le repository GitHub est configuré, les branches principales doivent être protégées autant que raisonnablement possible.

Pour `main`, privilégier :

* interdiction des force push ;
* CI obligatoire ;
* branche à jour avant fusion lorsque nécessaire ;
* fusion par Pull Request.

`develop` peut également exiger les vérifications essentielles.

---

# 28. Force push

Éviter :

```bash
git push --force
```

sur :

```text
main
develop
```

Si une branche personnelle nécessite exceptionnellement de réécrire son historique, préférer :

```bash
git push --force-with-lease
```

car cette commande protège mieux contre l’écrasement de changements distants inattendus.

---

# 29. `git reset` et historique partagé

Les commandes modifiant l’historique doivent être utilisées avec prudence.

Une fois un commit partagé, préférer généralement une correction explicite plutôt qu’une réécriture silencieuse de l’historique des branches principales.

Pour annuler un commit déjà partagé, `git revert` est souvent plus approprié.

---

# 30. Tags

Les versions stables sont identifiées par des tags.

La première release de production prévue est :

```text
v1.0.0
```

Le tag est créé depuis un commit stable de `main`.

Exemple :

```bash
git switch main
git pull --ff-only origin main

git tag -a v1.0.0 -m "Portfolio V1"
git push origin v1.0.0
```

---

# 31. Versioning

Le projet utilise un versioning de type Semantic Versioning :

```text
MAJOR.MINOR.PATCH
```

Exemple :

```text
1.0.0
```

Conceptuellement :

```text
PATCH
→ correction compatible

MINOR
→ fonctionnalité compatible

MAJOR
→ rupture importante de compatibilité
```

Pour la première mise en production :

```text
v1.0.0
```

n’est créée que lorsque la Definition of Done V1 est satisfaite.

---

# 32. CI GitHub Actions

La CI doit au minimum vérifier :

```text
checkout
    ↓
backend tests
    ↓
frontend tests
    ↓
build
    ↓
vérifications
```

Un changement qui casse la CI n’est pas considéré comme prêt à intégrer dans `main`.

---

# 33. Exemples de bons commits

```text
chore(repo): initialize repository structure

docs(domain): define V1 business model

build(backend): initialize Spring Boot project

build(frontend): initialize Angular application

feat(profile): add profile persistence

feat(project): add project management

feat(publication): add publication lifecycle

test(publication): cover scheduled visibility

feat(series): add ordered article series

feat(media): add local media storage

feat(search): add PostgreSQL full-text search

feat(contact): persist contact messages

fix(contact): preserve message when notification fails

feat(security): add admin session authentication

ci: add application verification workflow

docs(deploy): document production deployment
```

---

# 34. Exemples à éviter

Éviter :

```text
update
fix
changes
working
final
final2
test
stuff
wip
many changes
portfolio done
```

Ces messages n’expliquent pas ce qui a changé.

Éviter également les commits contenant simultanément des modifications indépendantes.

Exemple :

```text
feat: add login, fix search, redesign home and update docker
```

Il devrait normalement être séparé en plusieurs commits cohérents.

---

# 35. Commits temporaires

Des commits intermédiaires peuvent exister sur une branche personnelle pendant le travail, mais l’historique intégré doit rester compréhensible.

Éviter de construire volontairement l’historique final avec :

```text
wip
wip2
try
fix again
really fix
final final
```

---

# 36. Procédure standard quotidienne

Début d’un travail :

```bash
git switch develop
git pull --ff-only origin develop
git switch -c feat/<nom>
```

Pendant le développement :

```bash
git status
git diff
```

Après validation :

```bash
git status
git diff

git add <fichiers ciblés>

git diff --staged

git commit -m "type(scope): description"

git push -u origin feat/<nom>
```

Puis :

```text
Pull Request
    ↓
CI
    ↓
revue
    ↓
develop
```

---

# 37. Procédure pour une petite modification directe sur `develop`

Dans le cadre du tutoriel, certaines modifications limitées peuvent être réalisées directement sur `develop` lorsque cela reste volontaire et contrôlé.

Exemple :

```bash
git switch develop
git pull --ff-only origin develop

# modification limitée

git status
git diff

git add docs/conventions-git.md
git diff --staged

git commit -m "docs: document Git conventions"
git push origin develop
```

Pour une fonctionnalité significative, préférer une branche courte.

---

# 38. Ce que Git ne doit pas devenir

Le workflow ne doit pas ajouter de complexité sans bénéfice.

La V1 n’a pas besoin d’un Git Flow lourd comprenant systématiquement :

```text
release/*
hotfix/*
support/*
```

Le modèle retenu reste volontairement simple :

```text
main
develop
branches courtes lorsque nécessaire
```

---

# 39. Règle fondamentale

Avant chaque commit :

```text
observer
↓
comprendre les modifications
↓
tester
↓
git status
↓
git diff
↓
git add ciblé
↓
git diff --staged
↓
commit cohérent
```

Git ne doit jamais servir uniquement à sauvegarder un répertoire sans comprendre ce qui entre dans l’historique.

---

# 40. Résumé des conventions

```text
Branches permanentes
→ main
→ develop

Branches courtes
→ feat/*
→ fix/*
→ refactor/*
→ test/*
→ docs/*
→ chore/*

Commits
→ Conventional Commits

Avant commit
→ git status
→ git diff
→ git add ciblé
→ git diff --staged

Interdit
→ secrets
→ .env réel
→ credentials
→ artefacts de build
→ fichiers locaux inutiles

Release
→ depuis main
→ tag v1.0.0 après validation complète
```

Ces conventions constituent la référence Git du projet Portfolio.
