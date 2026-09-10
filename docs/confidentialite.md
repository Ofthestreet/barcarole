# Politique de confidentialité — Barcarole

*Dernière mise à jour : 10 septembre 2026 · [English version](privacy.md)*

Barcarole est un lecteur de musique hors ligne pour Android. Il joue les fichiers audio déjà
présents sur le téléphone ou sur sa carte mémoire.

## En bref

**Barcarole ne collecte rien, n'envoie rien, et n'a aucun moyen de faire l'un ou l'autre.**
Aucun compte, aucune publicité, aucune mesure d'audience, aucun traçage d'aucune sorte.

## Aucun accès au réseau

Ce n'est pas un réglage qu'on pourrait activer plus tard : l'application ne déclare tout
simplement pas la permission `INTERNET`. Android lui interdit donc toute communication réseau,
au niveau du système d'exploitation. Elle ne peut ni transmettre de données, ni vérifier une
mise à jour, ni charger quoi que ce soit depuis un serveur, quel que soit celui qui le lui
demanderait.

Cette affirmation se vérifie sans avoir à me croire : les permissions déclarées par une
application Android figurent dans son manifeste, qui fait partie du paquet publié, et le code
source de celle-ci est [public](https://github.com/Ofthestreet/barcarole).

## Ce qui est enregistré, et où

Tout ce que Barcarole retient reste sur l'appareil, dans l'espace privé de l'application :

- vos réglages (thème, taille du texte, ordres de tri, durée minimale) ;
- les morceaux marqués comme favoris ;
- le nombre de lectures de chaque morceau, et leur date ;
- la file d'attente en cours et la position de lecture.

Rien de tout cela ne quitte le téléphone. Rien n'est lisible par une autre application. Tout
est effacé à la désinstallation : Android supprime l'espace privé d'une application avec elle.

## Les permissions, et à quoi chacune sert

| Permission | Pourquoi elle est nécessaire |
|---|---|
| Lecture des fichiers audio (`READ_MEDIA_AUDIO`, ou `READ_EXTERNAL_STORAGE` sur les versions plus anciennes) | Pour trouver et jouer la musique déjà présente sur l'appareil. Sans elle, il n'y a rien à jouer. |
| Affichage de notifications | Pour proposer les commandes de lecture dans le volet de notifications et sur l'écran verrouillé. Facultative : la refuser ne retire que ces commandes. |
| Service au premier plan, type lecture de média | Pour continuer la lecture quand l'application passe en arrière-plan ou que l'écran s'éteint. |
| Écriture sur le stockage externe (Android 9 et antérieurs uniquement) | Pour supprimer un fichier audio lorsque vous le demandez explicitement. |

## La suppression de fichiers

Barcarole peut supprimer des fichiers audio de l'appareil, mais uniquement à votre demande :
par le bouton corbeille, ou par l'écran de nettoyage des doublons, qui demandent chacun une
confirmation. Sur Android 11 et suivants, le système en demande une seconde de son côté. La
suppression est définitive : les fichiers ne passent par aucune corbeille, et l'application
n'a aucun moyen de les restaurer.

## Les enfants

Barcarole ne s'adresse pas aux enfants et ne contient rien qui les vise. Aucune fonction
sociale, aucun contenu produit par des tiers, aucun lien sortant.

## Modifications de cette politique

Toute modification de cette page est enregistrée dans l'historique du dépôt, public et daté.

## Contact

Les questions sur cette politique peuvent être posées sous forme de ticket sur le
[dépôt du projet](https://github.com/Ofthestreet/barcarole/issues).
