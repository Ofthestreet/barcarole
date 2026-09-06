# Barcarole

Un lecteur de musique pour Android qui joue les fichiers déjà présents sur le téléphone ou
sur sa carte SD. Rien d'autre.

## L'idée

Quelque part en mer, il n'y a pas de réseau. Pas de 4G, pas de Wi-Fi, pas de streaming, pas
de « connectez-vous pour continuer », pas de synchronisation qui échoue au mauvais moment.
Il reste le téléphone, et la musique qui est dessus.

La plupart des lecteurs partent du principe inverse : ils supposent une connexion, un compte,
un catalogue distant, et traitent les fichiers locaux comme un cas particulier un peu
négligé. Barcarole fait exactement l'inverse. Les fichiers locaux sont le sujet unique. Tout
ce qui suppose un réseau a été retiré, pas désactivé.

Le nom vient de la barcarolle, la chanson des gondoliers vénitiens : une musique faite pour
être chantée sur l'eau.

## Ce que l'application ne fait pas, par construction

- **Elle n'a aucun accès au réseau.** Ce n'est pas un réglage à décocher : la permission
  Internet n'est pas déclarée du tout. Android l'empêche matériellement de communiquer avec
  quoi que ce soit. Elle ne peut ni envoyer de statistiques, ni vérifier une mise à jour, ni
  télécharger une pochette.
- **Pas de compte, pas d'inscription, pas de connexion.**
- **Pas de pochettes ni de vignettes.** C'est un choix : à la place, des listes denses où
  l'on voit plus de titres d'un coup d'œil.

## Installer

L'application n'est pas sur le Play Store. Chaque version est construite automatiquement et
déposée en ligne.

**Le plus simple — la dernière version publiée :**

> **[Télécharger la dernière version](https://github.com/Ofthestreet/local-music-player/releases/latest)**

Ce lien s'ouvre directement depuis le téléphone : il mène à la page de la version la plus
récente, où l'APK est attaché sous un nom du genre `barcarole-v0.1.0.apk`. Un appui dessus le
télécharge, un second l'installe. Android demandera l'autorisation d'installer des
applications depuis cette source — il faut l'accorder à l'application qui ouvre le fichier
(navigateur ou gestionnaire de fichiers).

**Pour prendre une version en cours de développement**, avant qu'elle soit publiée : ouvrir
la page **Actions** du dépôt, choisir la dernière exécution réussie (coche verte), télécharger
l'archive **`barcarole-debug-apk`** et la décompresser pour en sortir l'APK.

Une mise à jour s'installe par-dessus la précédente et conserve les réglages, les favoris et
l'historique d'écoutes.

## Au premier lancement

L'application demande **l'accès aux fichiers audio**. C'est la seule permission
indispensable : sans elle, il n'y a rien à jouer. Elle demande aussi l'autorisation
d'afficher des notifications — facultative, elle ne sert qu'à la commande de lecture dans le
volet de notifications et sur l'écran verrouillé.

Ensuite, l'application recense ce qu'Android sait déjà de la musique du téléphone, carte SD
comprise. Il n'y a pas de dossier à désigner ni d'analyse à lancer : c'est immédiat, et la
bibliothèque se met à jour d'elle-même quand des fichiers sont ajoutés ou retirés.

## Se repérer

Cinq onglets, dont un optionnel : **Titres**, *(Albums)*, **Artistes**, **File d'attente**,
**Playlists**. La barre du morceau en cours reste visible en bas de tous les écrans ; un
appui dessus ouvre le lecteur en plein écran.

## Les fonctionnalités qui méritent une explication

### Les playlists se remplissent toutes seules

Il n'y a aucune playlist à créer ni à entretenir. L'onglet Playlists calcule ses listes à
partir de ce que vous écoutez réellement :

- **Favoris** — ceux que vous avez marqués d'un cœur.
- **Les plus joués** — ce mois-ci, cette année, ou depuis toujours.
- **Ajoutés récemment** — la dernière semaine, le dernier mois, les 2 ou les 3 derniers mois.
- **Jamais joués** — le fond de cale : ce que vous avez copié sans jamais l'écouter.

Les périodes se choisissent par des puces, et la liste change juste en dessous.

**Ce qui compte comme une écoute :** un morceau n'est décompté qu'après en avoir entendu la
moitié, ou trente secondes s'il est long — le premier des deux atteint. Passer dix titres à
la suite ne gonfle donc pas les compteurs, et laisser tourner un morceau en boucle compte
chaque passage.

### Le cœur est dans la barre de lecture

Marquer un favori ne demande pas d'ouvrir un menu : le cœur est directement dans la barre du
morceau en cours, à portée de pouce pendant l'écoute. Les favoris sont aussi accessibles
depuis la voiture.

### Supprimer un fichier

La corbeille, à côté du cœur, supprime **le fichier lui-même du téléphone**, pas seulement
son entrée dans la bibliothèque. Une confirmation nomme le morceau et son artiste avant
d'agir. Sur Android 11 et suivants, le système affiche **sa propre** confirmation par-dessus
celle de l'application : c'est lui qui détient ce droit et il ne le délègue pas — il y a donc
deux questions pour une seule suppression. Et c'est définitif : rien ne part dans une
corbeille intermédiaire.

Une fois le fichier supprimé, l'application oublie tout le reste : le morceau quitte la file
de lecture, son favori et son historique d'écoutes disparaissent.

### Le nettoyage des doublons

Dans les réglages, **Remove duplicates** cherche les fichiers qui sont deux fois la même
chose — typiquement le même morceau copié depuis deux sources différentes.

La détection est volontairement sévère : même titre, même artiste et **même durée à la
seconde près**. Un live, une reprise, un remaster ou un autre encodage qui ne tombe pas
exactement sur la même durée ne sont pas considérés comme des doublons. Le choix est
délibéré : mieux vaut laisser passer des doublons que supprimer un enregistrement que vous
vouliez garder.

Rien n'est supprimé sans être passé sous vos yeux. L'écran liste chaque groupe, indique
quelle copie est **conservée** et lesquelles seraient **supprimées**, avec leur dossier et
leur taille, et chaque groupe peut être décoché. La copie gardée est le fichier le plus gros,
c'est-à-dire en général le meilleur encodage. Une seule confirmation ensuite, qui annonce
l'espace récupéré.

### Les albums sont facultatifs

Si vous écoutez des morceaux plutôt que des disques, l'onglet Albums est du bruit. Un
interrupteur dans les réglages le fait disparaître, ainsi que les albums dans les résultats
de recherche.

### Les dossiers, et la carte SD

L'application connaît le chemin réel de chaque fichier, et sait donc les présenter par
dossier — pratique quand la bibliothèque est rangée à la main, et pour retrouver ce qui est
sur la carte SD plutôt que dans la mémoire du téléphone. Comme c'est une entrée de secours
plus qu'un usage quotidien, elle est rangée dans les réglages et non dans les onglets.

### Ignorer les fichiers trop courts

Un curseur dans les réglages écarte de la bibliothèque tout ce qui dure moins de N secondes.
Réglé sur trente secondes par défaut, il tient les sonneries, les notifications et les mémos
vocaux hors de la liste des morceaux.

### La taille du texte

Quatre niveaux, de 100 % à 70 %. Réduire le texte ne réduit **que** le texte : les rangées,
les icônes et les zones à toucher gardent leur taille. Sur un écran petit ou fatigué, on
gagne plusieurs lignes visibles sans que les boutons deviennent difficiles à viser. Ce
réglage se combine avec celui d'Android : si la police du téléphone est déjà agrandie, ce
curseur redescend par-dessus.

### Chercher, et se déplacer dans une longue liste

La recherche couvre les titres, les artistes, les albums et les noms de dossiers. Sur les
listes longues, une réglette alphabétique sur le bord droit permet de sauter directement à
une lettre.

### La file d'attente

C'est un onglet à part entière, pas un écran caché. On y voit ce qui va suivre, on réordonne
par glissement, on retire un titre d'un geste, on vide tout. N'importe quel morceau peut être
ajouté à la suite ou en fin de file depuis un appui long.

## Dans la voiture

Barcarole apparaît dans Android Auto avec les favoris, les titres, les artistes et les
dossiers, et répond à la recherche vocale.

Une application installée à la main n'apparaît pas dans la voiture par défaut. Une fois, sur
le téléphone, dans l'application Android Auto :

1. Ouvrir les paramètres et taper une dizaine de fois sur le numéro de version, tout en bas,
   pour débloquer le mode développeur.
2. Dans les paramètres développeur, activer **Sources inconnues**.
3. Rebrancher le téléphone à la voiture.

## Ce qui se passe quand on quitte l'application

La lecture continue : elle est portée par un service en arrière-plan, pas par l'écran. Les
commandes restent dans les notifications et sur l'écran verrouillé, les boutons du casque et
du Bluetooth fonctionnent, la lecture se met en pause quand on débranche les écouteurs, et
baisse puis reprend lors d'un appel.

Au redémarrage du téléphone, l'application retrouve la file d'attente, le morceau en cours et
la position exacte dans ce morceau.

## Les réglages en un coup d'œil

| Réglage | Ce qu'il fait |
|---|---|
| Thème | Clair, sombre, ou selon le système |
| Couleurs dynamiques | Reprend la palette du fond d'écran (Android 12 et plus) |
| Taille du texte | 100 %, 90 %, 80 % ou 70 % |
| Afficher les albums | Montre ou cache l'onglet Albums |
| Parcourir par dossier | Ouvre la bibliothèque rangée par dossiers |
| Supprimer les doublons | Cherche et propose les copies en trop |
| Ignorer les morceaux courts | Écarte les fichiers sous une durée donnée |
| Réanalyser | Force un nouveau recensement de la bibliothèque |

## Deux mises en garde

**Les suppressions sont définitives.** La corbeille et le nettoyage des doublons effacent des
fichiers du téléphone. Il n'y a ni annulation ni corbeille intermédiaire. Avant un gros
nettoyage, faites un essai sur un morceau dont vous vous moquez.

**L'application ne connaît que ce téléphone.** Elle ignore ce qui existe ailleurs : si un
fichier n'est nulle part d'autre et que vous le supprimez, il est perdu. En mer, il n'y aura
pas de sauvegarde à retélécharger.

---

Pour construire l'application depuis les sources, voir [docs/BUILD.md](docs/BUILD.md).
