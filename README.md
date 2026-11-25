### Consignes: 
* Ignorez les migrations BDD
* Ne pas modifier les classes qui ont un commentaire: `// WARN: Should not be changed during the exercise
`
* Pour lancer les tests (depuis le sous-répertoire `api`) :
  * unitaires: `mvnw test`
  * integration: `mvnw integration-test`
  * tous: `mvnw verify`

---

## Refactoring réalisé

### Démarche

1. **Figer l'existant** : avant toute modification, des tests de caractérisation ont été écrits sur l'endpoint
   (`ProcessOrderIntegrationTests`). Ils décrivent ce que fait le code en production, y compris ses cas discutables.
2. **Refactorer par petits pas** : chaque commit ne fait qu'une chose et laisse `mvnw verify` vert
   (tests, couverture JaCoCo ≥ 80 %, PMD). Les scénarios et les assertions des tests de caractérisation n'ont pas
   changé pendant le refactoring : seule leur date de référence est devenue fixe, grâce à l'horloge injectée.
3. **Verrouiller l'architecture** : des règles ArchUnit font échouer le build si la séparation des couches est cassée.

L'historique git suit ces étapes dans l'ordre.

### Architecture

```
controllers/        OrderController               HTTP uniquement : délègue et construit la réponse
services/           OrderService                  cas d'usage « traiter une commande », transactionnel
services/handlers/  ProductOrderHandler           une règle de commande par type de produit
                    ├─ NormalProductOrderHandler
                    ├─ SeasonalProductOrderHandler
                    ├─ ExpirableProductOrderHandler
                    └─ ProductOrderHandlerRegistry   trouve le handler d'un produit
entities/           Product, ProductType, Order   état et règles métier du produit (stock, saison, expiration)
repositories/       OrderRepository, ProductRepository
config/             ClockConfiguration            horloge injectable
```

Sens des dépendances : `controllers → services → repositories`. Les handlers et les entités ne dépendent pas de la
persistance.

### Choix de conception

| Choix | Raison |
|---|---|
| **Pattern Strategy** (`ProductOrderHandler`) | Remplace le `if/else` sur le type de produit. Ajouter un type = ajouter une classe et une valeur d'enum, sans modifier l'existant (Open/Closed). |
| **Règles métier dans `Product`** (`isInStock`, `isInSeason`, `isExpired`…) | Le code se lit comme le cahier des charges. `Product` n'expose plus de setters : le stock ne change que par `decrementStock()` et `emptyStock()`. |
| **`Clock` injectée** | Supprime les `LocalDate.now()` en dur : les règles de saison et d'expiration sont testées sur une date fixe. |
| **`OrderService` transactionnel** | Le controller ne contient plus ni logique métier ni accès aux repositories. |
| **Injection par constructeur** | Dépendances explicites, classes testables sans Spring. |
| **`type` reste une `String` dans l'entité** | Un type inconnu en base est ignoré aujourd'hui. Mapper la colonne sur l'enum ferait échouer le chargement de la commande. |

### Règles métier

| Type | Situation | Résultat |
|---|---|---|
| NORMAL | en stock | stock − 1 |
| NORMAL | en rupture, délai > 0 | notification de délai |
| NORMAL | en rupture, délai = 0 | rien |
| SEASONAL | en saison et en stock | stock − 1 |
| SEASONAL | réapprovisionnement après la fin de saison | notification d'indisponibilité, stock à 0 |
| SEASONAL | saison pas encore commencée | notification d'indisponibilité, stock inchangé |
| SEASONAL | sinon | notification de délai |
| EXPIRABLE | en stock et non expiré | stock − 1 |
| EXPIRABLE | sinon | notification d'expiration, stock à 0 |

### Comportements legacy conservés

La mission étant « pas de régression », ces comportements discutables ont été **conservés** et figés par des tests
nommés `legacy: …`. Ils sont à valider avec le métier avant toute correction.

* **SEASONAL** : le premier et le dernier jour de saison ne sont pas considérés « en saison ». Un produit en stock ces
  jours-là n'est pas vendu : délai notifié le premier jour, indisponibilité et stock à 0 le dernier.
* **SEASONAL** : hors saison, si le réapprovisionnement tombe après la fin de saison, le stock est mis à 0 même s'il
  restait des unités.
* **EXPIRABLE** : un produit en rupture mais non expiré déclenche une notification d'*expiration*.
* **EXPIRABLE** : un produit est considéré expiré le jour même de sa date d'expiration.
* **Type inconnu** : le produit est ignoré, sans erreur.
* **Commande introuvable** : non gérée, la requête échoue avec une `NoSuchElementException` (erreur 500).

### Différences de comportement assumées

Elles ne concernent que des cas d'erreur :

* Un produit dont le type est `null` provoquait une `NullPointerException`. Il est maintenant ignoré, comme un type
  inconnu.
* La commande est traitée dans une seule transaction. Si une erreur survient en cours de traitement, les modifications
  de stock déjà faites sont annulées, alors qu'avant les premiers produits restaient modifiés.

### Tests

60 tests, lancés par `mvnw verify` depuis `api` :

| Niveau | Classes | Ce qui est vérifié |
|---|---|---|
| Unitaire | `ProductTests` | règles de stock, de saison et d'expiration, avec leurs bornes |
| Unitaire | `*ProductOrderHandlerTests`, `ProductOrderHandlerRegistryTests` | tous les cas métier de chaque type de produit |
| Unitaire | `OrderServiceTests` | orchestration du traitement d'une commande |
| Architecture | `ArchitectureTests` | séparation des couches |
| Intégration | `ProcessOrderIntegrationTests` | l'endpoint de bout en bout sur H2 : stock en base et notifications envoyées |
