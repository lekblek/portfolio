Un monolithe modulaire garde **un seul déploiement** et des frontières *vérifiées* : chaque module expose une façade, jamais ses entités.[^frontieres]

## Les règles

1. Un module ne lit les tables d'un autre que par sa façade.
2. Les cas d'usage portent les transactions (`@Transactional`).
3. Un test d'architecture fait échouer la construction.

### Le test d'architecture

```java
@AnalyzeClasses(packages = "com.scalke.portfolio.backend")
class ModuleBoundariesTest {

    @ArchTest
    static final ArchRule modules = slices()
        .matching("..backend.(*)..")
        .should().notDependOnEachOther();
}
```

## Ce que cela coûte

| Choix | Avantage | Coût |
| --- | --- | --- |
| Monolithe modulaire | un déploiement, transactions locales | discipline des frontières |
| Microservices | déploiements indépendants | réseau, cohérence éventuelle |

Les frontières se vérifient à chaque commit : voir [la documentation d'ArchUnit](https://www.archunit.org/userguide/html/000_Index.html).

[^frontieres]: Une façade ne renvoie que des valeurs immuables (records).
