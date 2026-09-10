# Amener Barcarole sur le Play Store

Objectif retenu : **l'application reste privée**, mais elle s'installe et se met à jour par le
Play Store, sans avertissement de source inconnue et sans perdre les données à chaque version.

La voie qui correspond à ça n'est pas la publication : c'est la **piste de test interne**.
L'application est distribuée par Google à une liste d'adresses e-mail que tu choisis — la
tienne suffit. Elle n'apparaît pas dans les recherches, elle n'a pas de fiche publique, et
surtout elle n'exige pas l'accès à la production, donc **pas de campagne de test à douze
personnes pendant quatorze jours**, qui est l'obstacle réel pour un compte personnel.

## Ce que ça règle

- **Les mises à jour s'installent par-dessus** et gardent réglages, favoris et historique. Le
  problème actuel — une clé de signature différente à chaque build — disparaît, parce que
  Google signe lui-même chaque version avec une clé unique et permanente.
- **Plus aucun avertissement** : l'installation vient du Play Store, pas d'un navigateur.
- **Les mises à jour arrivent toutes seules**, comme n'importe quelle autre application.

## Ce que ça ne règle pas

- L'application reste invisible pour tout le monde sauf les adresses inscrites.
- Il faut quand même un compte développeur payant et une identité vérifiée.
- Les règles de Google s'appliquent malgré la diffusion restreinte : les déclarations de
  contenu doivent être remplies avant toute mise en ligne.

---

## Phase 0 — Deux décisions à prendre maintenant

**1. L'identifiant de l'application est gelé à vie.** ✅ **Fait** : il est passé de
`com.cdelarue.localmusic` à **`io.github.ofthestreet.barcarole`**, en même temps que le paquet
Java. La forme `io.github.<compte>` est la convention pour un projet hébergé sur GitHub ; elle
désigne un espace de noms réellement contrôlé et n'expose que le pseudonyme, pas le nom de
famille — l'identifiant est visible dans l'URL du Play Store et lisible dans l'APK.

C'est modifiable jusqu'au premier envoi au Play Store, plus jamais après. Conséquence
immédiate sur le téléphone : Android voit une **application différente**, donc la version
déjà installée n'est pas remplacée — il faut la désinstaller.

**2. Compte personnel ou organisation.** Un compte personnel suffit ici. Il demande une pièce
d'identité et une adresse. Un compte d'organisation exige un identifiant D-U-N-S et prend plus
longtemps ; il n'apporte rien tant qu'on reste en test interne.

## Phase 1 — La signature

C'est le cœur du sujet, et ça vaut même sans le Play Store.

- [x] **Gradle sait signer** avec une clé stable dès qu'elle est disponible, et retombe sur la
      clé de débogage sinon, pour qu'un clone sans les secrets construise quand même. Les deux
      types de construction la prennent : l'APK installé à la main mérite des mises à jour
      stables autant que le bundle envoyé au magasin.
- [x] **Les deux workflows** reconstituent le keystore depuis les secrets avant de construire.
- [x] **Générer la clé d'upload** — à faire par toi, une clé privée ne doit transiter par
      personne :

      keytool -genkeypair -v -keystore upload.jks -alias upload \
        -keyalg RSA -keysize 4096 -validity 10000

      La sauvegarder ailleurs que sur le Mac, avec ses mots de passe.

- [x] **Déposer quatre secrets** dans *Settings → Secrets and variables → Actions* du dépôt :

      | Secret | Contenu |
      |---|---|
      | `UPLOAD_KEYSTORE_BASE64` | `base64 -i upload.jks` |
      | `UPLOAD_KEYSTORE_PASSWORD` | le mot de passe du keystore |
      | `UPLOAD_KEY_ALIAS` | `upload` |
      | `UPLOAD_KEY_PASSWORD` | le mot de passe de la clé |

      ✅ Fait, et vérifié : deux constructions successives produisent un certificat de
      signature identique, qui n'est plus une clé de débogage. Les APK s'installent maintenant
      par-dessus la précédente sans rien effacer, **sans attendre le Play Store**.

      Une conséquence à prévoir pour plus tard : avec Play App Signing, Google resigne le
      bundle avec sa propre clé. La version venue du Play Store n'aura donc pas la même
      signature que celles installées à la main, et ne pourra pas s'installer par-dessus. Le
      jour du basculement, il faudra désinstaller une dernière fois.
- [ ] Activer **Play App Signing** à la création de l'application dans la console. Google
      conserve alors la clé de signature définitive ; ta clé d'upload ne sert qu'à lui prouver
      que l'envoi vient de toi. Si tu la perds, elle se réinitialise — c'est précisément ce qui
      rend cette option préférable à une clé unique qu'on garde soi-même.

## Phase 2 — Le format et la chaîne de construction

- [ ] Produire un **Android App Bundle** (`.aab`) : le Play Store n'accepte plus l'APK pour une
      nouvelle application.
- [ ] Faire **incrémenter le `versionCode`** automatiquement, par exemple depuis le numéro
      d'exécution du workflow. Le Play Store refuse deux envois avec le même numéro.
- [ ] Vérifier le `targetSdk`. Il est à 36, largement au-dessus du minimum exigé, mais cette
      exigence monte chaque année : à revérifier le jour où tu t'y mets.
- [ ] Garder le workflow APK en parallèle, pour continuer à installer directement quand c'est
      pratique.

## Phase 3 — Les déclarations à préparer

Rien de tout cela n'est du code, mais tout est bloquant.

- [ ] **Politique de confidentialité** hébergée à une adresse publique. Une page servie par
      GitHub Pages depuis ce dépôt suffit. Le texte est court et honnête : l'application ne
      collecte rien, n'a aucun accès au réseau, ne crée aucun compte.
- [ ] **Sécurité des données** : le formulaire se remplit en quelques minutes puisque la
      réponse est « aucune donnée collectée, aucune donnée partagée ». L'absence de permission
      Internet est vérifiable dans le manifeste, c'est un argument solide.
- [ ] **Service au premier plan** : depuis 2024, l'usage d'un service `mediaPlayback` doit être
      déclaré et justifié dans la console, parfois avec une vidéo de démonstration. Le nôtre
      sert à continuer la lecture écran éteint — c'est exactement l'usage prévu.
- [ ] **Classification du contenu** (questionnaire IARC), **public visé**, **publicités**
      (aucune), **accès à l'application** (aucune authentification).
- [ ] **Android Auto** : déclarer le support voiture peut déclencher un examen supplémentaire
      contre les règles de distraction au volant. En test interne, la portée de cet examen est
      à vérifier au moment venu — c'est le point le plus incertain de ce plan.

## Phase 4 — La console

- [ ] Créer le compte développeur (**25 $, une seule fois**) et passer la vérification
      d'identité. C'est l'étape la plus lente : compte plusieurs jours, parfois plus.
- [ ] Créer l'application, activer Play App Signing, remplir les déclarations de la phase 3.
- [ ] Fournir le minimum de fiche exigé même en test interne : icône 512×512, quelques
      captures d'écran, description courte et longue. L'icône existe déjà dans `art/`.
- [ ] Créer la **piste de test interne** et y inscrire ton adresse.
- [ ] Envoyer le premier bundle, accepter l'invitation reçue par e-mail, installer depuis le
      lien du Play Store.

## Phase 5 — Plus tard, si l'envie vient

Passer en production depuis cette position demande, pour un compte personnel, un **test fermé
avec une douzaine de testeurs inscrits pendant quatorze jours consécutifs** avant de pouvoir
demander l'accès à la production. Ce nombre a déjà changé une fois (vingt au départ) et peut
changer encore. C'est la seule raison pour laquelle la production n'est pas le point de départ
de ce plan.

---

## Qui fait quoi

**Ce que je peux faire :** le renommage de l'identifiant, la configuration de signature dans
Gradle, la production du bundle et l'incrémentation du `versionCode` dans la chaîne de
construction, la page de politique de confidentialité, les textes de la fiche, et à terme la
publication automatique sur la piste interne via l'API du Play Store.

**Ce que toi seul peux faire :** créer et payer le compte, passer la vérification d'identité,
**générer la clé de signature et la déposer dans les secrets** — une clé privée ne doit pas
transiter par moi —, et remplir les déclarations dans la console.

## Ordre de grandeur

L'argent : 25 $, une fois. Le travail technique : quelques heures, réparties sur les phases 1
et 2. Le reste est de l'attente administrative, dominée par la vérification d'identité.

## Une mise en garde

Les règles du Play Store changent souvent, et plusieurs des points ci-dessus ont bougé au
cours des deux dernières années : le nombre de testeurs exigé, la déclaration des services au
premier plan, la vérification d'identité. Ce plan donne la structure et l'ordre ; les seuils
exacts sont à revérifier dans la documentation de Google le jour où tu t'y mets.
