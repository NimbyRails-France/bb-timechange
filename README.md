# Time Change

Mod outil Windows pour modifier la date et l'heure UTC depuis une fenêtre
appartenant au jeu. Aucun signal ou autre mod n'est nécessaire.

Ouvrir une partie, puis **Ctrl + Maj + T**. Renseigner la date et l'heure,
cliquer sur **Changer l'heure avec intervention sur les trains…**, puis confirmer.
La croix ferme le panneau.

Le mod propose un seul mode : le changement de date avec les interventions
natives sur les trains. Celles-ci peuvent les déplacer vers leur prochaine
destination et entraîner des coûts. La confirmation rappelle ces effets.
Le SDK conserve son mode sans intervention pour les autres outils.
Une erreur n'est jamais rejouée.
La saisie est UTC ; elle peut différer de l'heure affichée selon le fuseau du jeu.
Les conséquences sur tous les systèmes économiques et le multijoueur ne sont
pas qualifiées ; utiliser une sauvegarde dédiée pour la première recette.

Construire avec le nouveau kit SDK, JDK 21 et :

```powershell
.\gradlew.bat windowsTest packageMod -PnrfSdkDir=C:/chemin/kit-kotlin
```

Le plugin génère `mod.txt` depuis `metadata` dans `Entry.kt`. L'identité vient
de `mod.json` via `modInfo`. Les textes sont dans `assets/translations.json`.
Le mod définit le formulaire et la confirmation ; le SDK gère la fenêtre,
le calendrier, les erreurs natives et le journal commun dans `logs/mods`.

Sources : https://github.com/NimbyRails-France/time-change

La configuration Woodpecker prépare un paquet Windows puis publie une release
GitHub, son catalogue public pour le Hub et le miroir NRF. Le secret
`nrf_release_token` reste dans Woodpecker. Les builds ordinaires ne publient pas :
un commit `release 0.1.0-alpha.1` sur `alpha` déclenche la publication, après
validation. Publier d'abord le SDK contenant l'API des fenêtres d'outils, puis
son commit exact est épinglé dans `.woodpecker/sdk-revision.txt`. Activer aussi
le dépôt dans Woodpecker pour les publications ultérieures sur le VPS.

Woodpecker publishes verified Windows releases to GitHub and NRF, including a
public catalogue for Hub update detection. Publisher credentials remain CI-only.
Before the first release, publish the SDK with the tool-window API, pin its
commit in `.woodpecker/sdk-revision.txt`, and enable this repository in Woodpecker.

Publication GitHub pendant une indisponibilité du VPS : commit de release avec
le trailer `Release-Runner: github`. Les binaires Windows sont construits et
testés par GitHub Actions, puis le catalogue public du Hub est actualisé.
Le SDK épinglé doit être publié avant de lancer cette release.
