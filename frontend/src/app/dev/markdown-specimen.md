# Une erreur, un format

Une API se juge d’abord à ses erreurs : c’est là qu’un client passe le plus de temps. Ce spécimen réunit tout ce que le moteur de rendu doit traiter : [un lien interne](/projects), [un lien externe](https://angular.dev), [une ancre](#un-code-stable-plutot-qu-un-message), un `code` en ligne, du texte **gras**, *italique* et ~~barré~~, et une adresse nue : https://example.org.

## Un code stable plutôt qu’un message

Le texte de `detail` peut changer ; un client décide à partir de `code`.

```java
@ExceptionHandler(ResourceNotFoundException.class)
ProblemDetail handleResourceNotFound(ResourceNotFoundException ex) {
    // Même réponse pour une ressource absente et une ressource non publique
    return problem(HttpStatus.NOT_FOUND, ex.getMessage(), ex.errorCode());
}
```

```json
{
  "status": 404,
  "code": "RESOURCE_NOT_FOUND",
  "detail": "Publication introuvable."
}
```

### Ligne de 200 colonnes

```typescript
export const tresLongueLigne = 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa';
```

### Langage inconnu

```cobol
DISPLAY 'Le texte reste échappé : <b>pas de gras</b>'.
```

## Listes et citations

- Une liste à puces,
- avec un second élément qui passe à la ligne parce que sa phrase est volontairement longue pour vérifier l’interlignage et la césure du texte courant.

1. Une liste ordonnée,
2. dont l’ordre compte.

- [x] Écrire les tests du moteur
- [ ] Publier le premier article

> Une citation, en italique et en retrait, repérée par un filet.

## Tableau

| Statut | Code | Cas | Réponse du serveur | Remarque sur la colonne la plus longue du tableau |
|---|---|---|---|---|
| 400 | `VALIDATION_FAILED` | requête invalide | erreurs de champ | le client rattache chaque erreur à son champ |
| 404 | `RESOURCE_NOT_FOUND` | ressource absente ou non publique | même réponse | ne confirme pas l’existence d’un brouillon |
| 409 | `SLUG_LOCKED` | slug d’un contenu publié | refus | le slug d’un contenu publié ne change plus |

---

## Un code stable plutôt qu’un message

Un titre répété reçoit un identifiant distinct. Le HTML brut reste du texte : <script>alert(1)</script>, et [un lien piégé](javascript:alert(1)) perd son lien.
