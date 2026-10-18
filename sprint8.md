# SPRINT 8 — Formulaire d'upload de fichier

## Contexte

Le projet **SPRINT6** (branche `SPRINT-7-bis`) est une application Java Web basée sur un **framework maison** (`itu.webdynamique.framework`). Il utilise un servlet unique (`FrontServlet`) qui invoque par réflexion les méthodes des classes annotées `@Controller`.

L'objectif de ce sprint était de créer un nouveau formulaire permettant d'uploader un fichier (photo, PDF, etc.) via l'application existante, sans casser le code existant.

---

## 1. Analyse du projet existant

### Structure du framework (`framework/src/itu/webdynamique/framework/`)

| Fichier | Rôle |
|---|---|
| `FrontServlet.java` | Servlet unique (mapping `/`) qui résout les URL, invoque les méthodes contrôleur et gère la réponse (JSON avec `@RestAPI` ou forward JSP via `ModelAndView`) |
| `ModelAndView.java` | Contient l'URL de la vue et une map d'attributs |
| `UrlMapping.java`, `Controller.java`, `RestAPI.java` | Annotations de routage |
| `resolveArguments()` | Résout les paramètres des méthodes contrôleur : types simples via `request.getParameter()`, et objets par binding des champs du formulaire |

### Convention du contrôleur `EmpController.java`

- Méthodes annotées `@UrlMapping(value, method)`
- Paramètres nommés (compilation avec l'option `-parameters`)
- `@RestAPI` → réponse JSON (Gson)
- Retour de `ModelAndView` → forward vers `/WEB-INF/views/<url><suffixe>.jsp`

### Vues JSP (`test/WEB-INF/views/employe/`)

`ajout.jsp`, `ajout-object.jsp`, `liste.jsp` — style minimaliste HTML avec EL `${...}` et `contextPath`.

### Déploiement (`run.bat`)

1. Compilation du framework → `framework.jar`
2. `javac --release 8 -parameters` des contrôleurs → `test/WEB-INF/classes`
3. Copie des classes, vues, `web.xml`, `framework.jar` et `gson` dans `D:\XAMPP\tomcat\webapps\sprint8/`

**Java 21**, Tomcat XAMPP sur le port **8087**.


---

## 2. Plan d'implémentation

1. Étendre `FrontServlet` pour supporter les paramètres `javax.servlet.http.Part` (fichiers multipart) et l'injection de `HttpServletRequest`.
2. Ajouter les routes d'upload (GET afficher le formulaire, POST traiter l'envoi) dans `EmpController`.
3. Créer la vue JSP `upload.jsp`.
4. Configurer `multipart-config` dans `web.xml` (limite 10 Mo par fichier).
5. Compiler, déployer et tester avec Tomcat.

---

## 3. Modifications apportées

### 3.1 `framework/src/itu/webdynamique/framework/FrontServlet.java`

**Ajout d'import :**
```java
import javax.servlet.http.Part;
```

**Modification de `resolveArguments(Method, HttpServletRequest)`** pour ajouter deux nouveaux types de paramètres supportés (avant le traitement par défaut simple / binding objet) :

- `HttpServletRequest` → injection directe de l'objet requête (accès au `ServletContext`, etc.)
- `Part` → récupération du fichier via `request.getPart(nom)` (formulaire `multipart/form-data`)

Le reste de la logique (types simples, binding d'objets, conversion des primitives) est conservé intact, garantissant la rétrocompatibilité avec les contrôleurs existants (`save`, `save-object`, etc.).

### 3.2 `test/src/itu/webdynamique/app/controller/EmpController.java`

**Nouveaux imports :**
```java
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
```

**Deux nouvelles méthodes :**

| Méthode | Route | Description |
|---|---|---|
| `afficherFormulaireUpload()` | `GET /employe/upload` | Retourne un `ModelAndView` vers la vue `employe/upload` |
| `upload(String nom, Part fichier, HttpServletRequest request)` | `POST /employe/upload` | Reçoit le fichier, l'enregistre dans `<contexte>/uploads/`, retourne un message de succès ou d'erreur |

Le fichier est nommé avec `Paths.get(fichier.getSubmittedFileName()).getFileName()` pour éviter les séquences de parcours de répertoires, puis écrit via `fichier.write(destination.toString())`.

### 3.3 `test/WEB-INF/views/employe/upload.jsp` (créé)

Vue JSP suivant le même style que `ajout.jsp` :
- `<form method="post" enctype="multipart/form-data">`
- `<input type="file" name="fichier" accept=".jpg,.jpeg,.png,.gif,.pdf" required>`
- Champ texte optionnel `nom`
- Affichage des messages `${message}` (succès) et `${erreur}` (rouge)
- Lien de retour vers `/employe/liste`

### 3.4 `test/WEB-INF/web.xml`

Ajout d'une configuration multipart sur le `FrontServlet` pour lever la limitation par défaut de Tomcat (2 Mo) :

```xml
<multipart-config>
    <max-file-size>10485760</max-file-size>       <!-- 10 Mo par fichier -->
    <max-request-size>104857600</max-request-size> <!-- 100 Mo par requête -->
    <file-size-threshold>102400</file-size-threshold> <!-- 100 Ko sur disque -->
</multipart-config>
```

---

## 4. Build et déploiement

Exécution du script maison `run.bat` :

```
javac --release 8 -d bin -cp servlet-api.jar;gson-2.10.1.jar framework\src\...\*.java
jar -cvf framework.jar -C bin .
javac --release 8 -parameters -d test\WEB-INF\classes -cp framework.jar;servlet-api.jar;gson-2.10.1.jar test\src\itu\webdynamique\app\controller\*.java
```

Résultat :
- Compilation du framework et des contrôleurs : **succès** (seuls des warnings d'option `--release 8` obsolète, normaux sous JDK 21)
- `test/WEB-INF/classes/itu/webdynamique/app/controller/EmpController.class` généré
- `test/WEB-INF/views/employe/upload.jsp` copié
- Déploiement dans `D:\XAMPP\tomcat\webapps\sprint8/`

---

## 5. Tests effectués en direct (Tomcat XAMPP, port 8087)

### 5.1 Affichage du formulaire
```
GET /sprint8/employe/upload  ->  200 OK
```
La page "Upload de fichier" s'affiche avec le formulaire, les champs et le lien de retour.

### 5.2 Upload d'un fichier (PDF de test)
```
POST /sprint8/employe/upload
  nom = "Mon document test"
  fichier = test.pdf (306 octets)
```
Résultat : *"Le fichier "test.pdf" (306 octets) a été uploadé avec succès."*  
Le fichier `test.pdf` est physiquement présent dans `webapps/sprint8/uploads/test.pdf`.

### 5.3 Erreur : pas de fichier sélectionné
```
POST /sprint8/employe/upload  (sans champ fichier)
```
Résultat : *"Aucun fichier sélectionné."* (affiché en rouge)

### 5.4 Non-régression : fonctionnalités existantes
```
GET /sprint8/employe/liste  ->  200 OK
```

---

## 6. Limites et notes

- **Stockage** : les fichiers sont enregistrés dans le dossier `uploads/` du contexte web (créé automatiquement s'il n'existe pas). En cas de nom de fichier identique, le fichier précédent est écrasé.
- **Sécurité nom de fichier** : utilisation de `Paths.get(...).getFileName()` pour neutraliser les tentatives de parcours de répertoires (ex: `../../etc/passwd`).
- **Types acceptés** : le champ `accept` du formulaire restreint à `.jpg,.jpeg,.png,.gif,.pdf`, mais la restriction réelle se fait côté serveur via la configuration `multipart-config` (taille maximale).
- **Framework** : l'injection de `HttpServletRequest` et le support des `Part` sont ajoutés de manière additive ; aucun contrôleur existant n'est modifié ni cassé.
- **Port Tomcat** : l'instance XAMPP locale écoute sur **8087** (pas 8080 par défaut dans cette configuration).

---

## 7. Résumé des fichiers concernés

| Fichier | Action |
|---|---|
| `framework/src/itu/webdynamique/framework/FrontServlet.java` | Modifié (support `Part` + `HttpServletRequest`) |
| `test/src/itu/webdynamique/app/controller/EmpController.java` | Modifié (2 nouvelles méthodes d'upload) |
| `test/WEB-INF/views/employe/upload.jsp` | Créé |
| `test/WEB-INF/web.xml` | Modifié (ajout `<multipart-config>`) |
| `sprint8.md` | Créé (ce document) |

---

*Document généré le 18/10/2026 dans le cadre du SPRINT 8 (branche SPRINT-7-bis).*