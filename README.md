# MLTV — lecteur IPTV (Android TV)

Lecteur IPTV personnel : playlists **M3U** et **Xtream Codes**, chaînes par groupes,
lecteur vidéo (ExoPlayer/Media3), favoris. Interface pensée télécommande.
Compatible Android 5.0+ (box Android TV, Google TV, Fire TV, téléphones/tablettes).

> **v1** = TV en direct (M3U + Xtream). Les sections Films/Séries et la déco Leanback
> arrivent en v2, une fois le build validé.

---

## Obtenir l'APK sans rien installer (compilation en ligne)

Pas besoin d'Android Studio : GitHub fabrique l'APK à ta place.

### 1. Créer un compte GitHub
- Va sur **github.com** → **Sign up** (gratuit).

### 2. Créer un dépôt **privé**
- Clique sur **+** (en haut à droite) → **New repository**.
- Nom : `MLTV`.
- Coche **Private**.
- Clique **Create repository**.

### 3. Envoyer le projet
- Sur la page du dépôt vide : **uploading an existing file**.
- Glisse **tout le contenu** du dossier MLTV (le dossier `app`, le dossier `.github`,
  les fichiers `build.gradle`, `settings.gradle`, `gradle.properties`, `.gitignore`, ce `README.md`).
  > Important : garde la **structure des dossiers** telle quelle.
- En bas, clique **Commit changes**.

### 4. Laisser GitHub compiler
- Onglet **Actions** → tu verras « Build MLTV APK » démarrer tout seul.
- Attends ~3–5 min qu'il passe au **vert** ✅.

### 5. Télécharger l'APK
- Clique sur la compilation terminée → en bas, section **Artifacts** → **MLTV-APK**.
- Tu obtiens un `.zip` contenant `app-debug.apk`. Décompresse-le.

---

## Installer sur la TV

**Le plus simple (Fire TV / Android TV) : l'appli Downloader**
1. Installe **Downloader** depuis le store de la TV.
2. Mets ton `app-debug.apk` quelque part accessible par lien (ex. Google Drive, lien direct),
   ou utilise **Send Files to TV** depuis un téléphone sur le même wifi.
3. Downloader → saisis le lien → télécharge → **Installer**.
   (La 1ʳᵉ fois : autorise Downloader à installer des applis.)

**Téléphone / tablette Android** : ouvre simplement le fichier `.apk` → Installer.

---

## Première utilisation
Au lancement, choisis **une source** :
- **M3U** : colle l'URL de ta playlist.
- **Xtream Codes** : serveur (`http://serveur:port`), utilisateur, mot de passe.

Ensuite : groupes en haut, chaînes à gauche, lecture à droite.
**Appui long** sur une chaîne = ajouter/retirer des **favoris**.
Bouton **Sources** = changer de source.

> Les identifiants restent **sur l'appareil**. Ne les mets jamais dans le code / sur GitHub.

---

## Compiler avec Android Studio (optionnel)
`File → Open` → dossier MLTV → attendre le « Gradle sync » →
`Build → Build Bundle(s)/APK(s) → Build APK(s)`.
L'APK sort dans `app/build/outputs/apk/debug/`.
