package itu.webdynamique.framework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation permettant de restreindre l'acces a une methode de controleur
 * aux utilisateurs possedant l'un des roles specifies.
 *
 * Exemple :
 *   @RolesAllowed({"ADMIN", "MANAGER"})
 *   public ModelAndView adminPage() { ... }
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RolesAllowed {
    String[] value();
}