Construire une classe Session (map(String,object))
dans la classe on recupere toute les session
dans la classe on peut effacer ou ajouter des session 
et puis renvoyer les session dans httpSession 
dans context ajouter un map(sessionID,callsse session)
dans controller on verifie la requete de l'HttpSession si il est deja dans la mapContext ou pas et si non on va l'ajouter . on instancera la nouvelle classe session et c'est la classe session qui va etre la session de l'utilisateur



Donc si j'ouvre deux navigateur differents leurs session devarit etre tres differentes
on affcihera les session dans la page et donc les session devarit etre bien differentes 
et je veux une nouvelle page login pour le tester apres 