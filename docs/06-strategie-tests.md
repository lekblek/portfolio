# Stratégie de tests — Portfolio

## 1. Objectif

Ce document définit la stratégie de tests du backend Portfolio.

Les objectifs sont :

* conserver une boucle de développement rapide ;
* tester les composants au niveau le plus adapté ;
* vérifier l’intégration réelle avec PostgreSQL ;
* garantir que les migrations Flyway fonctionnent sur une base vierge ;
* rendre les tests reproductibles localement et en CI ;
* éviter toute dépendance à une base PostgreSQL démarrée manuellement ;
* empêcher qu’une suite de tests verte masque l’absence réelle de tests d’intégration.

La stratégie distingue deux commandes principales :

```bash
./mvnw test
```

pour les tests rapides, et :

```bash
./mvnw verify
```

pour la vérification complète du backend.

---

# 2. Pyramide de tests retenue

Le backend utilise plusieurs niveaux de tests.

```text
tests unitaires
      ↓
tests de tranche Spring
      ↓
tests d’intégration
      ↓
PostgreSQL réel via Testcontainers
```

L’objectif n’est pas d’utiliser `@SpringBootTest` partout.

Chaque comportement doit être testé avec le niveau le plus léger capable de le vérifier correctement.

---

## 2.1 Tests unitaires

Les tests unitaires vérifient une classe ou une règle métier sans démarrer le contexte Spring lorsque celui-ci n’est pas nécessaire.

Ils sont adaptés notamment aux :

* règles métier ;
* objets de valeur ;
* conversions ;
* calculs ;
* fonctions déterministes ;
* composants ne nécessitant pas l’infrastructure Spring.

Ils doivent rester rapides et indépendants de Docker.

---

## 2.2 Tests de tranche

Les tests de tranche démarrent uniquement la partie de Spring nécessaire au comportement testé.

Exemples :

```text
@WebMvcTest
→ couche HTTP / MVC

@DataJpaTest
→ persistence JPA
```

Un test de contrôleur qui ne nécessite pas de base de données ne doit pas démarrer un contexte applicatif complet.

Exemple :

```text
GlobalExceptionHandlerTest
→ @WebMvcTest
→ aucune base PostgreSQL nécessaire
```

Cette approche permet de conserver une suite rapide pour le développement quotidien.

---

## 2.3 Tests d’intégration

Les tests nécessitant l’application complète utilisent :

```text
@SpringBootTest
```

Ils héritent de :

```text
AbstractIntegrationTest
```

qui fournit :

* le profil Spring `test` ;
* la configuration Testcontainers ;
* un PostgreSQL réel ;
* l’application des migrations Flyway ;
* la validation Hibernate du schéma.

Exemples :

```text
ApplicationContextIT
FlywayMigrationIT
```

Ces tests sont volontairement séparés des tests rapides.

---

# 3. Convention de nommage

Le projet distingue les tests Maven grâce au nom des classes.

## Tests rapides

Convention :

```text
*Test
```

Exemples :

```text
PageResponseTest
GlobalExceptionHandlerTest
ModuleBoundariesTest
```

Ils sont exécutés par Maven Surefire.

Commande :

```bash
cd backend
./mvnw test
```

Sous Windows PowerShell :

```powershell
cd backend
.\mvnw.cmd test
```

Ces tests ne doivent pas nécessiter le démarrage d’un conteneur PostgreSQL.

---

## Tests d’intégration

Convention :

```text
*IT
```

Exemples :

```text
ApplicationContextIT
FlywayMigrationIT
```

Ils sont exécutés par Maven Failsafe pendant :

```text
integration-test
verify
```

Commande de référence :

```bash
cd backend
./mvnw verify
```

Sous Windows PowerShell :

```powershell
cd backend
.\mvnw.cmd verify
```

`verify` constitue la vérification complète du backend.

---

# 4. Surefire et Failsafe

Le projet utilise deux boucles distinctes.

```text
./mvnw test
        ↓
Surefire
        ↓
*Test
        ↓
tests rapides
```

et :

```text
./mvnw verify
        ↓
Surefire
        ↓
*Test
        ↓
Failsafe
        ↓
*IT
        ↓
vérification complète
```

Failsafe doit exécuter les deux goals :

```text
integration-test
verify
```

Le goal :

```text
integration-test
```

exécute les tests d’intégration.

Le goal :

```text
verify
```

vérifie ensuite leurs résultats et fait échouer le build si nécessaire.

Un plugin Failsafe configuré sans `verify` pourrait produire un build apparemment réussi malgré un test d’intégration en échec.

---

# 5. PostgreSQL réel pour les tests d’intégration

Les tests d’intégration utilisent :

```text
PostgreSQL
```

dans un conteneur Testcontainers.

Le projet n’utilise pas une base en mémoire telle que :

```text
H2
```

pour simuler PostgreSQL.

La règle est :

```text
tests d’intégration nécessitant la persistence
→ PostgreSQL réel
```

---

# 6. Pourquoi ne pas utiliser H2

Le projet utilise des fonctionnalités spécifiques à PostgreSQL.

Le moteur de recherche prévu utilisera notamment :

```text
tsvector
GIN
websearch_to_tsquery
```

Ces comportements ne sont pas équivalents dans une base générique en mémoire.

Un test exécuté uniquement avec H2 pourrait donc réussir alors que l’application échoue avec PostgreSQL en production.

La stratégie retenue privilégie :

```text
fidélité au moteur réel
```

plutôt qu’une simulation approximative de la persistence.

---

# 7. Testcontainers

Testcontainers pilote Docker directement depuis les tests.

Flux général :

```text
mvn verify
    ↓
test d’intégration
    ↓
démarrage PostgreSQL
    ↓
port hôte aléatoire
    ↓
connexion Spring
    ↓
Flyway
    ↓
Hibernate validate
    ↓
tests
    ↓
destruction du conteneur
```

Le conteneur est éphémère.

Il ne dépend pas du PostgreSQL utilisé pour le développement quotidien.

Il est donc possible d’exécuter les tests alors que :

```text
deploy/compose.dev.yaml
```

est arrêté.

---

# 8. `@ServiceConnection`

Le conteneur PostgreSQL est déclaré comme bean Spring et annoté :

```text
@ServiceConnection
```

Spring Boot récupère automatiquement les informations de connexion du conteneur :

```text
URL JDBC
utilisateur
mot de passe
port
```

Il n’est donc pas nécessaire de maintenir manuellement une configuration du type :

```text
spring.datasource.url
spring.datasource.username
spring.datasource.password
```

pour le conteneur de test.

Le port PostgreSQL côté machine hôte est choisi dynamiquement par Testcontainers.

Il ne faut pas supposer qu’il s’agit de :

```text
5432
```

---

# 9. Configuration commune des tests d’intégration

Tous les tests d’intégration doivent, sauf justification explicite, hériter de :

```text
AbstractIntegrationTest
```

Cette classe centralise :

```text
@SpringBootTest
@ActiveProfiles("test")
@Import(ContainersConfiguration.class)
```

Le but est d’avoir une configuration identique pour l’ensemble des tests d’intégration.

---

# 10. Cache de contexte Spring

Spring met les contextes de test en cache.

Deux tests ayant exactement la même configuration peuvent réutiliser :

```text
le même ApplicationContext
        ↓
le même bean PostgreSQLContainer
        ↓
le même conteneur
```

Cette optimisation est importante pour conserver une durée de test raisonnable.

Une variation de configuration peut créer un nouveau contexte.

Exemples :

```text
@TestPropertySource
@MockitoBean
annotations de contexte supplémentaires
configuration Spring différente
```

Conséquence possible :

```text
nouveau contexte
        ↓
nouveau conteneur PostgreSQL
        ↓
suite plus lente
```

---

# 11. Règle pour `AbstractIntegrationTest`

Par défaut, une classe `*IT` doit simplement hériter de :

```text
AbstractIntegrationTest
```

Exemple :

```java
class ApplicationContextIT extends AbstractIntegrationTest {

    @Test
    void context_loads() {
    }
}
```

Éviter d’ajouter directement sur chaque test :

```text
@SpringBootTest
@ActiveProfiles
@Import
@TestPropertySource
```

sans nécessité démontrée.

Une annotation modifiant le contexte doit être ajoutée uniquement lorsqu’un test possède réellement un besoin différent.

---

# 12. Flyway dans les tests

Flyway reste l’unique propriétaire du schéma PostgreSQL.

Le profil de test conserve :

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
```

Hibernate :

```text
valide
```

le schéma.

Il ne doit pas :

```text
create
update
create-drop
```

le schéma utilisé par les tests d’intégration.

Cette règle permet de vérifier que les migrations Flyway suffisent réellement à reconstruire la base.

---

# 13. Base vierge

Le test :

```text
FlywayMigrationIT
```

doit vérifier que les migrations s’appliquent correctement sur une base initialement vierge.

Il contrôle notamment :

```text
flyway_schema_history
```

et vérifie que les migrations enregistrées ont :

```text
success = true
```

Cela protège contre les migrations qui fonctionneraient uniquement sur une base locale déjà modifiée manuellement.

---

# 14. Version de PostgreSQL

L’image PostgreSQL utilisée par Testcontainers doit rester alignée avec celle utilisée par le projet.

La version de référence actuelle de l’infrastructure de test est :

```text
postgres:18.6
```

Toute montée de version PostgreSQL doit vérifier au minimum :

```text
deploy/compose.dev.yaml
configuration de production
ContainersConfiguration
```

afin d’éviter de tester une version différente de celle réellement utilisée.

---

# 15. Version de Testcontainers

La version de Testcontainers n’est pas fixée manuellement dans :

```text
backend/pom.xml
```

Elle est gérée par le BOM Spring Boot.

La version effectivement utilisée doit être contrôlée avec :

```powershell
cd backend
.\mvnw.cmd -q dependency:tree | Select-String "testcontainers"
```

ou sous un shell Unix :

```bash
cd backend
./mvnw -q dependency:tree | grep testcontainers
```

La sortie Maven constitue la référence sur la version réellement résolue.

Version actuellement résolue :

```text
À renseigner après exécution de dependency:tree.
```

Cette valeur doit être mise à jour lors d’une montée de version importante de Spring Boot ou Testcontainers.

---

# 16. Testcontainers 2.x

Le projet utilise les artefacts Testcontainers 2.x compatibles avec Spring Boot 4.

Pour PostgreSQL :

```text
org.testcontainers:testcontainers-postgresql
```

La classe utilisée est :

```text
org.testcontainers.postgresql.PostgreSQLContainer
```

Ne pas introduire volontairement les anciens imports Testcontainers 1.x lorsqu’une API 2.x est disponible.

---

# 17. Prérequis Docker

Les tests rapides :

```bash
./mvnw test
```

ne nécessitent normalement pas Docker.

Les tests d’intégration :

```bash
./mvnw verify
```

nécessitent un moteur Docker fonctionnel.

Avant de diagnostiquer un problème Testcontainers, vérifier :

```bash
docker version
docker ps
```

La commande :

```text
docker version
```

doit pouvoir communiquer avec le client et le serveur Docker.

Il n’est en revanche pas nécessaire de démarrer manuellement PostgreSQL avec Docker Compose.

---

# 18. Commandes de référence

## Boucle rapide

```bash
cd backend
./mvnw test
```

Objectif :

```text
tests rapides
aucun PostgreSQL Testcontainers
retour rapide pendant le développement
```

---

## Vérification complète

```bash
cd backend
./mvnw verify
```

Objectif :

```text
tests rapides
+
tests d’intégration
+
PostgreSQL réel
+
Flyway
+
validation Hibernate
```

Cette commande est la référence avant un commit concernant le backend.

---

# 19. Autonomie de la suite

La suite d’intégration doit fonctionner même lorsque l’environnement PostgreSQL de développement est arrêté.

Vérification :

```bash
docker compose -f deploy/compose.dev.yaml down
```

puis :

```bash
cd backend
./mvnw verify
```

Résultat attendu :

```text
BUILD SUCCESS
```

Cette vérification démontre que les tests ne dépendent pas d’un état manuel de la machine.

---

# 19 bis. Niveau de test par couche d’un module

> Ajouté le 2026-09-24. S’appuie sur la structure décrite dans `04-architecture-backend.md` §6.

| Couche testée | Type | Suffixe | Outil | Exemple actuel |
|---|---|---|---|---|
| `domain.model` (invariants, objets de valeur) | unitaire pur | `*Test` | JUnit + AssertJ, sans Spring | `CertificationTest`, `ProjectTest`, `ProjectFilterTest`, `PublicationTest`, `PublicationTransitionTest` (table des transitions exhaustive, paramétrée), `TagTest` ; partagés : `DateRangeTest`, `PageQueryTest`, `PageResultTest` (`shared.domain.model`) |
| entités JPA (rattachement à l’agrégat) | unitaire pur | `*Test` | JUnit + AssertJ | `CertificationEntityTest` |
| mappers de persistance | unitaire pur | `*Test` | aller-retour modèle ↔ entité | `ProfilePersistenceMapperTest`, `ProjectPersistenceMapperTest`, `PublicationPersistenceMapperTest`, `CategoryPersistenceMapperTest`, `TagPersistenceMapperTest` |
| `application.usecase` (orchestration, filtrage, tri, pagination, nombre de requêtes) | intégration | `*IT` | `AbstractIntegrationTest` + `Statistics` Hibernate (`generate_statistics`, profil `test` seulement) | `GetProfileUseCaseIT`, `ListPublishedProjectsUseCaseIT` (dont le nombre de requêtes d’une page, D-AB), `GetPublishedProjectUseCaseIT`, `ListVisiblePublicationsUseCaseIT`, `GetVisiblePublicationUseCaseIT` (horloge fixe), `ChangePublicationStatusUseCaseIT` (écriture sans HTTP), `TaxonomyQueryServiceIT` (façade inter-modules) |
| `infrastructure.persistence` (mapping, tri, requêtes) | intégration | `*IT` | `AbstractIntegrationTest` + `EntityManager` | `ProfileMappingIT` |
| schéma (contraintes SQL) | intégration | `*IT` | `JdbcClient` | `ProfileSchemaIT`, `ProjectSchemaIT`, `TechnologySchemaIT`, `PublicationSchemaIT`, `TaxonomySchemaIT`, `PublicationTaxonomySchemaIT` |
| `web` (contrat JSON, codes HTTP, erreurs, bornes de pagination) | tranche | `*Test` | `@WebMvcTest` + `@MockitoBean` du cas d’usage | `PublicProfileControllerTest`, `PublicProjectControllerTest`, `PublicPublicationControllerTest` |
| parcours HTTP complet (contrôleur → base) | intégration | `*IT` | `AbstractIntegrationTest` + `MockMvc` | `PublicProfileIT`, `PublicProjectIT`, `PublicPublicationIT` |
| architecture | unitaire | `*Test` | ArchUnit | `ModuleBoundariesTest`, `ModuleLayersTest`, `ApiConventionsTest` |

Règles :

* la classe de test vit dans le **même paquet** que la classe testée (un test de `infrastructure…DateRange` ne se range pas dans `profile.domain`) ;
* tout test qui hérite d’`AbstractIntegrationTest` est un `*IT`, sans exception ;
* les `*IT` tournent avec une horloge fixe (`FixedClockConfiguration.NOW`, importée par `AbstractIntegrationTest`) : les données temporelles d’un test se construisent relativement à `NOW`, jamais à `Instant.now()` (D-AG) ;
* les données d’un `*IT` peuvent être créées par le port du module (ex. `ProjectRepository.create` avec `ProjectFixtures`) : elles passent alors par les invariants du domaine ; les tests de schéma utilisent `JdbcClient` pour atteindre la base sans le domaine ;
* une règle écrite deux fois (en Java dans le domaine, en SQL dans une requête) est protégée par un test de concordance qui compare les deux sur les mêmes données (ex. `the_domain_rule_and_the_query_agree_on_visibility`, D-AY) ;
* une machine à états se teste par une table exhaustive (`@CsvSource` de toutes les paires origine → cible) plutôt que par quelques cas choisis ;
* un test `@WebMvcTest` construit ses données avec le **modèle métier**, pas avec des entités JPA ni des mappers de persistance ;
* un test de chargement ne doit pas être annoté `@Transactional` s’il prétend vérifier ce qui est chargé **hors** transaction : la transaction du test garderait la session ouverte et masquerait le problème. Pour compter les requêtes, utiliser les statistiques Hibernate plutôt qu’une supposition. Un test de comptage se valide par mutation : retirer l’optimisation qu’il protège (ex. `@BatchSize`) doit le faire échouer. Une mutation se vérifie avec `./mvnw clean verify` : la compilation incrémentale peut laisser `target/` incohérent (entité « Not a managed type », classe supprimée encore présente) et produire une erreur sans rapport avec la mutation.

## Pièges connus

* Les classes de test annotées `@RestController` (ex. `ErrorHandlingTestController`) sont détectées par le scan de composants de **tous** les `@SpringBootTest`, car elles vivent sous `com.scalke.portfolio.backend`. C’est sans effet aujourd’hui, mais un tel contrôleur doit rester limité à des routes `/test-…`.
* `./mvnw test` doit rester exécutable sans Docker : c’est le critère qui détecte un `*Test` mal nommé.

---

# 20. Règles du projet

```text
*Test
→ Surefire
→ rapide
→ aucun conteneur requis

*IT
→ Failsafe
→ intégration
→ peut utiliser PostgreSQL Testcontainers
```

Règles complémentaires :

```text
PostgreSQL réel pour les tests de persistence

pas de H2 pour simuler PostgreSQL

Flyway possède le schéma

Hibernate utilise validate

les tests d’intégration héritent d’AbstractIntegrationTest

éviter les annotations modifiant inutilement le contexte

aucune datasource Testcontainers codée en dur

./mvnw verify avant un commit backend
```

---

# 21. Critères de validation

```text
[ ] ./mvnw test exécute les tests rapides
[ ] ./mvnw test ne démarre pas PostgreSQL
[ ] ./mvnw verify exécute les *Test et les *IT
[ ] les tests d’intégration utilisent PostgreSQL réel
[ ] Flyway fonctionne sur une base vierge
[ ] Hibernate valide le schéma sans le modifier
[ ] verify fonctionne lorsque compose.dev.yaml est arrêté
[ ] les tests d’intégration partagent le contexte lorsque possible
[ ] aucun conteneur PostgreSQL résiduel ne reste après les tests
[ ] la version Testcontainers résolue est connue
[ ] aucun secret ou identifiant PostgreSQL de test n’est codé en dur
```

---

# 22. Résumé

La stratégie backend repose sur deux boucles :

```text
développement quotidien
→ ./mvnw test
→ tests rapides
```

et :

```text
validation complète
→ ./mvnw verify
→ tests rapides
→ tests d’intégration
→ PostgreSQL réel
→ Flyway
→ Hibernate validate
```

Cette séparation permet de conserver une boucle de travail rapide tout en vérifiant le backend contre l’infrastructure réellement utilisée par l’application.
