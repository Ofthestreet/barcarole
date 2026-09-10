# Réponses prêtes pour la Play Console

Tout ce que la console va demander, préparé à l'avance. Rien ici n'est du code : c'est du
copier-coller et des cases à cocher, mais toute la phase 3 du [plan](play-store.md) tient
là-dedans.

---

## 1. Fiche principale

**Nom de l'application** — `Barcarole`

**Description courte** (80 caractères maximum)

```
Lecteur de musique hors ligne pour les fichiers déjà présents sur le téléphone.
```

**Description complète**

```
Barcarole joue la musique qui est déjà sur votre téléphone ou sur sa carte SD. Rien d'autre.

Pas de streaming, pas de compte, pas de catalogue distant. L'application n'a même aucun accès
au réseau : la permission Internet n'est pas déclarée, Android lui interdit donc toute
communication, au niveau du système. Elle ne peut ni envoyer de statistiques, ni charger quoi
que ce soit depuis un serveur.

C'est un lecteur pensé pour les endroits où il n'y a pas de réseau du tout.

CE QU'ELLE FAIT

• Recense la musique du téléphone et de la carte SD, sans rien demander de plus qu'une
  permission de lecture. Pas de dossier à désigner, pas d'analyse à lancer.
• Titres, artistes, file d'attente et listes de lecture, en onglets. Les albums sont
  facultatifs et se cachent d'un interrupteur.
• Des listes de lecture qui se remplissent seules : favoris, les plus joués (ce mois-ci, cette
  année, depuis toujours), ajoutés récemment, jamais joués. Rien à créer ni à entretenir.
• Une écoute n'est comptée qu'après la moitié du morceau, ou trente secondes s'il est long :
  passer dix titres ne gonfle pas les compteurs.
• Le cœur des favoris est directement dans la barre de lecture, à portée de pouce.
• Suppression d'un fichier depuis l'application, avec confirmation. Et un nettoyage des
  doublons qui montre chaque groupe avant d'effacer quoi que ce soit.
• Taille du texte réglable de 100 % à 70 %, pour voir plus de lignes sur un petit écran.
• Recherche sur les titres, artistes, albums et dossiers, réglette alphabétique sur les
  longues listes.
• Lecture qui continue écran éteint, commandes dans les notifications et sur l'écran
  verrouillé, boutons du casque et du Bluetooth, pause au débranchement des écouteurs.
• File d'attente, morceau en cours et position exacte retrouvés après un redémarrage.
• Android Auto : favoris, titres, artistes et dossiers accessibles depuis la voiture.

CE QU'ELLE NE FAIT PAS

Pas de publicité. Pas de compte. Aucune donnée collectée, aucune donnée transmise, aucun
traçage. Pas de pochettes : des listes denses à la place, pour voir plus de titres d'un coup
d'œil.

Le nom vient de la barcarolle, la chanson des gondoliers vénitiens : une musique faite pour
être chantée sur l'eau.
```

**Catégorie** — Application › Musique et audio
**Tags** — lecteur de musique, hors ligne, lecteur local

**Ressources graphiques**

| Élément | Où le trouver |
|---|---|
| Icône 512×512 | `art/icon-512.png` |
| Bannière 1024×500 | `art/feature-graphic-1024x500.png` |
| Captures d'écran téléphone (2 minimum) | **à faire par toi** — voir la fin de cette page |

**Coordonnées** — une adresse e-mail est obligatoire. Sur une fiche publique elle est visible
de tous ; en test interne, la fiche ne l'est pas. À toi de choisir entre ton adresse
personnelle et une adresse dédiée. Site web : `https://github.com/Ofthestreet/barcarole`.

**Politique de confidentialité** — `https://ofthestreet.github.io/barcarole/privacy.html`

---

## 2. Sécurité des données

| Question | Réponse |
|---|---|
| L'application collecte-t-elle ou partage-t-elle des données utilisateur ? | **Non** |
| Les données sont-elles chiffrées en transit ? | Sans objet — rien n'est transmis |
| Proposez-vous un moyen de demander la suppression des données ? | Sans objet — rien n'est collecté |

À déclarer si la console insiste sur les données stockées localement : réglages, favoris,
compteurs d'écoute et file d'attente restent dans l'espace privé de l'application et
disparaissent à la désinstallation. Ce ne sont pas des données « collectées » au sens du
formulaire, qui ne vise que ce qui quitte l'appareil.

L'argument décisif, si la déclaration est contestée : **l'application ne déclare pas la
permission `INTERNET`**. C'est vérifiable dans le manifeste du paquet envoyé.

---

## 3. Accès à l'application

**Toutes les fonctionnalités sont disponibles sans accès particulier.** Pas d'authentification,
pas de code, pas d'abonnement. Aucun identifiant à fournir aux évaluateurs.

## 4. Publicités

**L'application ne contient pas de publicité.**

## 5. Classification du contenu (questionnaire IARC)

Catégorie déclarée : **Utilitaire, productivité, communication ou autre**.

Toutes les réponses sont « non » : ni violence, ni contenu sexuel, ni langage grossier, ni
substances contrôlées, ni jeux d'argent, ni contenu produit par les utilisateurs, ni partage de
position, ni partage d'informations personnelles, ni achats intégrés. Le résultat attendu est
la classification la plus basse dans chaque système.

## 6. Public visé

**18 ans et plus.** L'application ne vise pas les enfants et ne contient rien qui les
concerne ; choisir une tranche d'âge incluant les mineurs ferait entrer la fiche dans le
programme « Familles » et ses obligations supplémentaires, sans aucun bénéfice ici.

## 7. Services au premier plan

Un service de type `mediaPlayback` est déclaré, et il faut le justifier.

**Fonctionnalité concernée** — Lecture audio en arrière-plan.

**Justification à copier :**

```
L'application est un lecteur de musique. Le service au premier plan de type mediaPlayback
maintient la lecture lorsque l'utilisateur quitte l'application ou éteint l'écran, et porte la
notification de lecture qui affiche le morceau en cours et les commandes lecture, pause,
précédent et suivant. C'est également ce service qui expose la bibliothèque à Android Auto.
Le service ne démarre qu'à la première lecture demandée par l'utilisateur et s'arrête avec
elle.
```

Une **vidéo de démonstration** peut être exigée : filmer le démarrage d'un morceau, le retour
à l'écran d'accueil, puis l'écran éteint, en montrant que la lecture continue et que la
notification affiche les commandes.

## 8. Autres déclarations

Application gouvernementale : non. Fonctionnalités financières : aucune. Santé : aucune.
Contenu généré par les utilisateurs : aucun. Chiffrement soumis à restriction : aucun usage
au-delà de ce qu'Android fournit.

## 9. Android Auto

Le support voiture est déclaré dans le manifeste, ce qui peut déclencher un examen
supplémentaire au titre des règles de distraction au volant. C'est le point le plus incertain
de la démarche : la portée de cet examen sur une piste de test interne reste à constater. Si
l'envoi bloque là-dessus, l'issue de secours consiste à retirer temporairement la déclaration
voiture, publier, puis la remettre — au prix de perdre Android Auto sur la version installée
par le magasin.

---

## Ce qui reste à ta charge

- [ ] **Les captures d'écran.** Google veut de vraies captures de l'application, pas des
      maquettes. Deux minimum, prises sur le téléphone : la liste des titres et le lecteur en
      plein écran suffisent, l'onglet Playlists fait un bon troisième.
- [ ] **L'adresse e-mail de contact**, et le choix entre personnelle et dédiée.
- [ ] **La vidéo du service au premier plan**, si la console la réclame.
