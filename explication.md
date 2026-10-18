# Sprint 7 bis : fonctionnement des URL et réception d'un objet `Employe`

## 1. Comment une URL est associée à une méthode

Une route est déclarée sur une méthode de contrôleur avec `@UrlMapping`. Par
exemple, dans `EmpController.java` :

```java
@UrlMapping("/employe/ajout-object")
public ModelAndView afficherFormulaireAjoutObject() { ... }

@UrlMapping(value = "/employe/save-object", method = "POST")
@RestAPI
public Map<String, Object> saveObject(Employe employe) { ... }
```

`value` est le chemin pris en charge. `method` est la méthode HTTP ; si elle
n'est pas précisée, sa valeur par défaut est `GET`. Le couple méthode HTTP +
chemin identifie une route. Ainsi, `GET /employe/ajout-object` et
`POST /employe/save-object` sont deux routes distinctes.

## 2. Comment le framework découvre les routes

1. Dans `test/WEB-INF/web.xml`, le paramètre `package_controllers` indique le
   paquet `itu.webdynamique.app.controller` à examiner.
2. Au démarrage de l'application, `FrameworkContextListener` lit ce paramètre
   et demande à `MappingInitializer` de parcourir le paquet.
3. `PackageScanner` cherche les classes compilées de ce paquet. `MappingInitializer`
   garde celles qui ont `@Controller`, puis examine leurs méthodes annotées
   `@UrlMapping`.
4. Pour chaque méthode, le framework enregistre le chemin et le verbe HTTP dans
   une table. La valeur enregistrée désigne la classe du contrôleur et le nom de
   la méthode. Les clés sont des `VerbUrl`, qui contiennent le chemin et le
   verbe HTTP.
5. `FrameworkContextListener` place cette table dans le contexte de l'application.
   `FrontServlet` la récupère lors de son initialisation. Le `servlet-mapping`
   `/` de `web.xml` envoie les requêtes de l'application vers ce servlet.

## 3. Comment `FrontServlet` traite une requête

Pour chaque requête, `FrontServlet` lit le verbe HTTP et le chemin demandé. Il
retire le chemin de contexte de l'application (par exemple `/sprint7`) afin de
comparer uniquement le chemin applicatif, comme `/employe/ajout-object`. Il
cherche ensuite dans la table une entrée ayant à la fois ce chemin et ce verbe.

Quand une entrée correspond, le servlet charge la classe du contrôleur, crée
une instance, retrouve la méthode, prépare ses arguments, puis l'appelle par
réflexion. Si aucune entrée ne correspond, il renvoie une réponse 404 et liste
les routes disponibles. À la racine de l'application, `/sprint7/`, il renvoie
la table des routes en JSON.

## 4. Parcours du formulaire Sprint 7 bis

1. Ouvrir `http://localhost:8087/sprint7/employe/ajout-object` en GET. Le chemin
   `/sprint7` désigne le contexte Tomcat ; `/employe/ajout-object` est la route.
2. `FrontServlet` trouve `afficherFormulaireAjoutObject()` dans `EmpController`.
   Cette méthode renvoie un `ModelAndView` dont la vue est
   `employe/ajout-object`.
3. Le servlet construit le chemin JSP avec le préfixe et le suffixe de
   `web.xml` : `/WEB-INF/views/` + `employe/ajout-object` + `.jsp`. Il transmet
   aussi l'attribut `titre`, puis fait suivre la requête vers
   `test/WEB-INF/views/employe/ajout-object.jsp`.
4. La JSP affiche les champs `id`, `nom`, `prenom` et `age`. Chaque attribut
   HTML `name` correspond au nom d'un champ de la classe `Employe`.
5. À la soumission, le formulaire envoie une requête POST à
   `/sprint7/employe/save-object`. L'attribut `action` inclut le chemin de
   contexte automatiquement grâce à `${pageContext.request.contextPath}`.
6. `FrontServlet` trouve la route POST `/employe/save-object`. La méthode
   `saveObject` attend un argument de type `Employe`, donc le servlet crée un
   `Employe` avec son constructeur sans argument, puis lit les paramètres de la
   requête ayant les mêmes noms que les champs de l'objet. Les valeurs sont
   converties selon le type du champ ; par exemple `age` et `id` deviennent des
   entiers.
7. Le servlet appelle `saveObject(employe)`. Cette méthode renvoie une map avec
   un message et l'employé reçu. Comme elle porte `@RestAPI`, `FrontServlet`
   sérialise le résultat en JSON dans la réponse.

La classe `Employe` est dans `test/src/itu/webdynamique/app/controller/Employe.java`.
Elle fournit un constructeur sans argument et les champs `nom`, `prenom`, `age`
et `id`. Le formulaire JSP est dans `test/WEB-INF/views/employe/ajout-object.jsp`.

## 5. Compilation et déploiement

`run.bat` compile le framework et l'application avec `--release 8`, pour que les
classes puissent être chargées par le Java 8 utilisé par Tomcat 8.5. L'option
`-parameters` conserve les noms des paramètres des méthodes, dont le framework
a besoin pour associer les valeurs reçues aux paramètres. Le script place
l'application dans `webapps/sprint7`, ce qui donne le chemin de contexte
`/sprint7`. Le descripteur `web.xml` utilise Servlet 3.1, compatible avec Tomcat
8.5.
