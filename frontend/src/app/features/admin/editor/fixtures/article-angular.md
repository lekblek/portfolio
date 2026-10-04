## Un formulaire à base de signaux

Les formulaires de signaux d'Angular suivent chaque élément **par identité** : un élément déplacé garde son état.

> Un formulaire n'est pas une liste de champs : c'est un modèle, des règles et des erreurs.
>
> — notes de conception

Trois étapes :

1. Déclarer le modèle avec `signal()`.
2. Créer le formulaire avec `form(model, schema)`.
3. Lier chaque contrôle avec la directive `[formField]`.

```typescript
const model = signal({ title: '', tags: [] as string[] });
const articleForm = form(model, (path) => {
  required(path.title, { message: 'Indiquez le titre.' });
});
```

Le contrôle d'un champ de texte :

```html
<input type="text" [formField]="articleForm.title" />
```

- Les erreurs du serveur se rattachent au champ (`experiences[2].endDate`).
- Le focus va à la première erreur, dans l'ordre de la page.
  - Une liste vide est une erreur de la liste entière.
  - Une liste ordonnée se réordonne au clavier.

Plus de détails dans [l'article sur la pagination](/articles/pagination-par-liens-l-etat-dans-l-adresse).
