# ADR 0001 — Architecture interne des modules backend : ports et adaptateurs légers

Statut : **Acceptée** (2026-09-24) — reconnaît l'architecture déjà implémentée dans `profile`
Date : 2026-09-24

## Contexte

`docs/04-architecture-backend.md` (étape 10) et toutes les fiches d'étape de 14 à 16.3 décrivent une structure interne en couches simples :

```text
profile/
├── api/          contrôleurs + DTO
├── domain/       entités JPA (= modèle métier)
├── repository/   interfaces Spring Data
└── service/      services applicatifs
```

Le module `profile` a été implémenté autrement (commits `1177490` à `f0f1eb1`) :

```text
profile/
├── domain/
│   ├── model/        records métier, sans JPA
│   └── port/         interfaces sortantes (ProfileRepository)
├── application/
│   └── usecase/      GetProfileUseCase
├── infrastructure/
│   ├── persistence/jpa/
│   │   ├── entity/       *Entity (JPA)
│   │   ├── mapper/       entité ↔ modèle
│   │   └── repository/   ProfileJpaRepository (Spring Data) + ProfileRepositoryAdapter (implémente le port)
│   └── seed/         ProfileSeeder (profil Spring dev)
└── web/
    ├── controller/   PublicProfileController
    └── dto/          *Response
```

Cette divergence n'était documentée nulle part. Conséquences observées pendant l'audit du 2026-09-24 :

- chaque fiche d'étape prescrit des chemins qui n'existent pas (`profile/repository/…`, `profile/service/ProfileQueryService`, `profile/api/…`) ;
- la migration est à moitié faite : l'invariant de période (`DateRange`) et la garde de `Certification` vivent dans les entités JPA, tandis que leurs tests sont rangés dans `profile.domain` ;
- `ArchUnit` vérifie les nouveaux emplacements (`..web.controller..`, `..infrastructure.persistence.jpa.entity..`) mais les noms des tests et `05-conventions-api.md` §23/§32 décrivent encore les anciens (`..api..`, `..domain..`) ;
- le port contient des méthodes non utilisées, implémentées par des bouchons silencieux (`findById` → vide, `exists` → `false`, `delete` → rien).

## Options étudiées

### A. Revenir à la structure en couches documentée

- \+ moins de code (pas de mappers ni de ports) ; alignement immédiat avec les fiches d'étape.
- − réécriture du module `profile` et de ses tests (≈ 25 fichiers) ;
- − les entités JPA redeviennent le modèle métier : les invariants dépendent du cycle de vie JPA (constructeur sans argument, setters, proxies) ;
- − abandonne un choix fait délibérément et déjà appliqué.

### B. Conserver ports et adaptateurs, sous une forme « légère » et encadrée — **recommandée**

- \+ aucun coût de migration pour l'existant ;
- \+ modèle métier sans JPA ni Spring → invariants testables en tests unitaires purs, sans Docker ;
- \+ le contrat HTTP ne peut pas dépendre de la persistance (le web ne voit jamais une `*Entity`) ;
- \+ démontre une séparation des responsabilités défendable en entretien ;
- − une couche de mapping par agrégat ;
- − risque de cérémonie (interfaces vides, ports « au cas où ») → contenu par les règles ci-dessous.

### C. Mixte selon les modules (couches simples pour `taxonomy`, ports pour `publication`)

- − deux conventions à maintenir, règles ArchUnit conditionnelles, fiches d'étape encore plus ambiguës. Écartée.

## Décision

Option **B**, avec les règles suivantes.

### 1. Paquets d'un module

| Paquet | Contient | Peut dépendre de |
|---|---|---|
| `<module>.domain.model` | records / classes métier, objets de valeur, enums, invariants | JDK, `shared.error` |
| `<module>.domain.port` | interfaces **sortantes** (persistance, stockage, mail, horloge si besoin) | `domain.model` |
| `<module>.application.usecase` | un cas d'usage par classe, frontière transactionnelle | `domain`, `shared`, Spring (`@Service`, `@Transactional`) |
| `<module>.infrastructure.persistence.jpa.entity` | entités JPA `*Entity` | `domain.model` (types de valeur seulement si utile), JPA |
| `<module>.infrastructure.persistence.jpa.repository` | interfaces Spring Data `*JpaRepository` + `*RepositoryAdapter` qui implémente le port | `domain`, `entity`, `mapper` |
| `<module>.infrastructure.persistence.jpa.mapper` | conversions statiques entité ↔ modèle | `domain.model`, `entity` |
| `<module>.infrastructure.<autre>` | autres adaptateurs (seed dev, stockage, SMTP) | `domain`, `application` si nécessaire |
| `<module>.web.controller` | `Public*Controller`, `Admin*Controller` | `application`, `domain.model`, `web.dto`, `shared.api` |
| `<module>.web.dto` | records `*Request` / `*Response` avec `from(...)` | `domain.model` |

Interdits :

```text
domain          → application, infrastructure, web, Spring, jakarta.persistence
application     → infrastructure, web
web             → infrastructure
infrastructure  → web
```

### 2. Garde-fous contre la cérémonie

- **Pas de port entrant** : un cas d'usage est une classe concrète, sans interface `XxxUseCase` + `XxxUseCaseImpl`.
- **Un port ne contient que des méthodes appelées** par un cas d'usage existant. Aucune méthode bouchon (`return Optional.empty()`, `return false`, corps vide). Une méthode manquante s'ajoute avec le cas d'usage qui en a besoin.
- **Pas de mapper généré** (MapStruct) tant que les conversions tiennent en quelques lignes.
- Les sous-paquets n'apparaissent qu'avec leur première classe.

### 3. Où vivent les invariants

- Dans `domain.model` (constructeur compact de record ou méthode de l'agrégat), testés par des `*Test` purs.
- Doublés par une contrainte PostgreSQL lorsque la base peut les garantir (principe D-N).
- Les entités JPA restent des structures de persistance : elles ne sont pas la source des règles métier.

### 4. Transactions

La frontière transactionnelle est le **cas d'usage** (`@Transactional`, `readOnly = true` pour une lecture).

Un adaptateur de persistance peut aussi être annoté `@Transactional` lorsqu'il doit rester correct hors cas d'usage (ex. `ProfileSeeder` en profil `dev`, qui appelle directement le port) : appelé depuis un cas d'usage, il rejoint la transaction existante (propagation `REQUIRED`). Il convertit toujours ses entités en objets du domaine **avant** de rendre la main.

### 5. Nommage

`<Verbe><Nom>UseCase` avec une méthode `execute(...)` ; `<Nom>Repository` pour le port ; `<Nom>JpaRepository` pour Spring Data ; `<Nom>RepositoryAdapter` pour l'implémentation ; `<Nom>Entity` ; `<Nom>PersistenceMapper` ; `<Nom>Response` / `<Action><Nom>Request`.

## Conséquences

### Traduction des fiches d'étape

Les fiches de `docs/steps/` restent rédigées pour la structure A. Jusqu'à leur réécriture éventuelle, elles se lisent avec cette table :

| Fiche d'étape | Projet réel |
|---|---|
| `<module>/domain/X.java` (entité JPA) | `domain/model/X.java` (record) **+** `infrastructure/persistence/jpa/entity/XEntity.java` **+** mapper |
| `<module>/repository/XRepository.java` (Spring Data) | `domain/port/XRepository.java` (port) **+** `infrastructure/persistence/jpa/repository/XJpaRepository.java` **+** `XRepositoryAdapter.java` |
| `<module>/service/XQueryService.java` | `application/usecase/GetXUseCase.java` (une classe par cas d'usage) |
| `<module>/api/*Controller.java` | `web/controller/*Controller.java` |
| `<module>/api/*Response.java`, `*Request.java` | `web/dto/*` |
| `XQueryServiceIT` | `GetXUseCaseIT` (ou `XRepositoryAdapterIT`) |
| « entité détachée » | le cas d'usage renvoie des records : aucun chargement paresseux possible hors transaction |

### Travaux induits

| # | Travail | État |
|---|---|---|
| 1 | Retirer du port `ProfileRepository` les méthodes non utilisées (`findById`, `delete`, `exists`) et leurs bouchons | fait le 2026-09-24 |
| 2 | Déplacer `DateRange` et les gardes de dates vers `profile.domain.model` | prévu en 16.3 (décision D-Q de la fiche) |
| 3 | Règles ArchUnit de couches (§1) : `ModuleLayersTest` | fait le 2026-09-24 |
| 4 | `04-architecture-backend.md` §6 et §12.4, `05-conventions-api.md` §23/§32 | fait le 2026-09-24 |
| 5 | Entités JPA fermées (constructeur protégé, pas de setter public hors champs optionnels, `addX` qui fixe la référence arrière) | fait le 2026-09-24 |

### Protection

`ModuleLayersTest` (ArchUnit) fait échouer le build si une couche viole les interdits du §1.
