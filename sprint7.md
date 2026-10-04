
TODO
# Sprint 7
- creer une fonction *save()* dans le controller qui necessite des attributs
exemple : nom, prenom, age 
- attribuer un url api a la fonction *save()*
- creer un formulaire d'ajout dans le cote client
- le formulaire fait appel a l'url de la fonction *save()*
- dans FrontServlet, lors de l'identification de la fonction attribuer a 
l'url appeler, on verifie si la fonction necessite des attribut
    - si oui, on fait coinsider les attributs qui conrespondent
    - si non, on fait *NULL*