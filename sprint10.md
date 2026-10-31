# SPRINT 10 — Authentification obligatoire pour accéder aux contrôleurs

## Objectif

L'utilisateur doit être **connecté** pour pouvoir exécuter les méthodes des contrôleurs et naviguer selon les URLs. Auparavant, toute URL était accessible sans authentification. Désormais, seules les pages de connexion (`/login`, `/logout`) et la racine (`/`) sont accessibles sans session ; toute autre requête redirige vers `/login` (ou retourne une erreur 401 JSON pour les `@RestAPI`).

---

## 1. Principe de fonctionnement

Le `FrontServlet` (servlet unique) intercepte chaque requête avant d'invoquer la méthode du contrôleur :

1. Il récupère (ou crée) l'objet `Session` personnalisé associé à l'identifiant de session HTTP (`HttpSession.getId()`), stocké dans une `Map<sessionId, Session>` partagée dans le `ServletContext`.
2. Il vérifie si la session contient l'attribut `"user"` (défini lors du login).
3. Si l'URL est publique (`/login`, `/logout`, `/`) → accès autorisé.
4. Sinon, si l'utilisateur n'est pas connecté :
   - Méthode annotée `@RestAPI` → réponse HTTP 401 avec JSON `{"erreur":"Authentification requise"}`.
   - Autre cas → redirection HTTP 302 vers `/login`.
5. Si connecté → la méthode contrôleur est invoquée normalement.

Ainsi, deux navigateurs différents ont deux sessions HTTP distinctes, donc deux objets `Session` distincts, et leurs données sont bien isolées.

---

## 2. Fichiers modifiés

### 2.1 `framework/src/itu/webdynamique/framework/FrontServlet.java`

Ajout, dans `processRequest(HttpServletRequest, HttpServletResponse)`, juste après la détection de `isRestAPI` et avant l'invocation de la méthode contrôleur :

```java
// ===== SPRINT 10 : Authentification obligatoire =====
String[] urlsPubliques = { "/login", "/logout", "/" };
boolean estPublique = false;
for (String u : urlsPubliques) {
    if (requestedUrl.equals(u)) {
        estPublique = true;
        break;
    }
}

Session sessionUtilisateur = (Session) request.getAttribute("__user_session__");
boolean estConnecte = (sessionUtilisateur != null && sessionUtilisateur.get("user") != null);

if (!estPublique && !estConnecte) {
    if (isRestAPI) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        out.print("{\"erreur\":\"Authentification requise\"}");
        return;
    }
    response.sendRedirect(contextPath + "/login");
    return;
}
```

Ce bloc s'appuie sur le bloc de gestion des sessions (Sprint 9) qui place déjà l'objet `Session` dans l'attribut de requête `__user_session__`.

---

### 2.2 `test/src/itu/webdynamique/app/controller/EmpController.java`

Aucune modification supplémentaire n'était nécessaire dans le contrôleur pour l'authentification elle-même (la vérification est centrale dans `FrontServlet`). Les contrôleurs existants (`liste`, `save`, `upload`, `session`, etc.) sont désormais protégés automatiquement.

Les contrôleurs de session déjà présents (Sprint 9) restent inchangés :
- `GET /login` → affiche le formulaire
- `POST /login` → définit `session.set("user", username)`
- `GET /session` → affiche les données de la session (id, utilisateur, attributs)
- `GET /logout` → vide la session et redirige vers `/login`

---

### 2.3 `framework/src/itu/webdynamique/framework/session/Session.java` (Sprint 9)

Classe créée précédemment, réutilisée ici. Elle encapsule une `Map<String, Object>` avec les méthodes `get`, `set`, `remove`, `clear`, `getAll`, `contains`.

---

## 3. URLs publiques (sans authentification requise)

| URL | Méthode | Rôle |
|---|---|---|
| `/` | GET | Liste des mappings disponibles (JSON) |
| `/login` | GET | Formulaire de connexion |
| `/login` | POST | Traitement de la connexion (définit `session.user`) |
| `/logout` | GET | Déconnexion (vide la session) |

Toutes les autres URLs (`/employe/liste`, `/employe/save`, `/employe/upload`, `/session`, etc.) requièrent une session valide.

---

## 4. Tests effectués (Tomcat XAMPP, port 8087, webapp `sprint9`)

| Test | Requête | Résultat attendu | Résultat obtenu |
|---|---|---|---|
| Page login publique | `GET /sprint9/login` | 200 | 200 ✅ |
| Accès liste sans login | `GET /sprint9/employe/liste` | 302 → `/login` | 302 ✅ |
| API sans login | `GET /sprint9/employe/api` | 401 JSON | 401 ✅ |
| Connexion | `POST /sprint9/login` (username=test) | 200 + cookie | 200 ✅ |
| Liste après login | `GET /sprint9/employe/liste` (avec cookie) | 200 | 200 ✅ |
| API après login | `GET /sprint9/employe/api` (avec cookie) | 200 JSON | 200 ✅ |
| Page session | `GET /sprint9/session` (avec cookie) | 200 | 200 ✅ |

---

## 5. Comportement multi-navigateur

Comme l'identifiant de session (`JSESSIONID`) est lié au cookie HTTP, deux navigateurs différents (ou deux fenêtres en navigation privée) obtiennent deux `sessionId` distincts. Le `FrontServlet` crée deux objets `Session` séparés dans la `Map<sessionId, Session>` du contexte. Leurs données (notamment l'attribut `"user"`) sont donc totalement isolées.

---

## 6. Résumé des fichiers concernés

| Fichier | Action |
|---|---|
| `framework/src/itu/webdynamique/framework/FrontServlet.java` | Modifié (ajout vérification d'authentification obligatoire) |
| `framework/src/itu/webdynamique/framework/session/Session.java` | Créé (Sprint 9, réutilisé) |
| `test/src/itu/webdynamique/app/controller/EmpController.java` | Inchangé pour ce sprint (contrôleurs login/session déjà présents) |
| `test/WEB-INF/views/login.jsp` | Créé (Sprint 9) |
| `test/WEB-INF/views/session.jsp` | Créé (Sprint 9) |

---

*Document généré dans le cadre du SPRINT 10 (branche SPRINT-7-bis).'
