package itu.webdynamique.framework;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.HashMap;

import com.google.gson.Gson;
import itu.webdynamique.framework.annotation.RestAPI;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class FrontServlet extends HttpServlet {

    private Map<VerbUrl, Mapping> urlMappingMap;

    
    private String prefixe;
    private String suffixe;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

       
        ServletContext servletContext = config.getServletContext();
        Object attribute = servletContext.getAttribute(
            FrameworkContextListener.MAPPING_MAP_ATTRIBUTE
        );

        if (attribute instanceof Map<?, ?>) {
            @SuppressWarnings("unchecked")
            Map<VerbUrl, Mapping> sharedMap = (Map<VerbUrl, Mapping>) attribute;
            this.urlMappingMap = sharedMap;
        } else {
            this.urlMappingMap = new HashMap<>();
            String packageToScan = config.getInitParameter("package_controllers");
            MappingInitializer initializer = new MappingInitializer();
            initializer.initializeMappings(packageToScan, this.urlMappingMap);
        }

        this.prefixe = config.getInitParameter("prefixe");
        this.suffixe = config.getInitParameter("suffixe");

        System.out.println("[Framework] prefixe = " + prefixe);
        System.out.println("[Framework] suffixe = " + suffixe);
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/plain;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String httpMethod   = request.getMethod().toUpperCase();
        String contextPath  = request.getContextPath();
        String requestedUrl = request.getRequestURI().substring(contextPath.length());

        if (requestedUrl.equals("/") || requestedUrl.isEmpty()) {
            response.setContentType("application/json;charset=UTF-8");
            Map<String, String> mappings = new HashMap<>();
            for (VerbUrl cle : urlMappingMap.keySet()) {
                mappings.put(cle.toString(), urlMappingMap.get(cle).toString());
            }
            Gson gson = new Gson();
            out.print(gson.toJson(mappings));
            return;
        }

        VerbUrl cle = new VerbUrl(requestedUrl, httpMethod);

        if (urlMappingMap.containsKey(cle)) {
            Mapping mapping = urlMappingMap.get(cle);

            try {
                Class<?> laClasse  = Class.forName(mapping.getClassName());
                Object   instance  = laClasse.getDeclaredConstructor().newInstance();
                Method   laMethode = findMappedMethod(laClasse, mapping.getMethodName());

                // Vérifier si la méthode a l'annotation @RestAPI
                boolean isRestAPI = laMethode.isAnnotationPresent(RestAPI.class);

                Object[] arguments = resolveArguments(laMethode, request);
                Object resultat = laMethode.invoke(instance, arguments);

                if (isRestAPI) {
                    // Mode API REST - retourner du JSON
                    response.setContentType("application/json;charset=UTF-8");
                    
                    if (resultat instanceof String) {
                        // Si c'est une String, écrire directement
                        out.print(resultat);
                    } else {
                        // Sinon, convertir en JSON avec Gson
                        Gson gson = new Gson();
                        String json = gson.toJson(resultat);
                        out.print(json);
                    }
                } else if (resultat instanceof ModelAndView) {
                    // Mode classique - forward vers JSP
                    ModelAndView mv = (ModelAndView) resultat;
                    String cheminJsp = prefixe + mv.getUrl() + suffixe;
                    if (!cheminJsp.startsWith("/")) {
                        cheminJsp = "/" + cheminJsp;
                    }

                    for (Map.Entry<String, Object> entry : mv.getData().entrySet()) {
                        request.setAttribute(entry.getKey(), entry.getValue());
                    }

                    RequestDispatcher dispatcher = getServletContext().getRequestDispatcher(cheminJsp);
                    dispatcher.forward(request, response);

                } else {
                    out.println("Methode executee. (pas de ModelAndView retourne)");
                }

            } catch (IllegalArgumentException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.println("Requête invalide : " + e.getMessage());
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.println("Erreur : " + e.getMessage());
                e.printStackTrace();
            }
            return;
        }

        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        out.println("=== URL non supportee ===");
        out.println("Demandee : " + cle);
        out.println("");
        out.println("URLs disponibles :");
        for (VerbUrl k : urlMappingMap.keySet()) {
            out.println("  " + k);
        }
    }

    private Method findMappedMethod(Class<?> controllerClass, String methodName)
            throws NoSuchMethodException {
        for (Method method : controllerClass.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        throw new NoSuchMethodException(controllerClass.getName() + "." + methodName);
    }

    private Object[] resolveArguments(Method method, HttpServletRequest request)
            throws IllegalArgumentException {
        Parameter[] parameters = method.getParameters();
        if (parameters.length == 0) {
            return null;
        }

        Object[] arguments = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            if (!parameter.isNamePresent()) {
                throw new IllegalArgumentException("Noms de paramètres indisponibles pour "
                        + method.getName() + "; compiler avec l'option -parameters.");
            }

            String value = request.getParameter(parameter.getName());
            if (value == null) {
                if (parameter.getType().isPrimitive()) {
                    throw new IllegalArgumentException("Paramètre obligatoire manquant : "
                            + parameter.getName());
                }
                arguments[i] = null;
            } else {
                arguments[i] = convertParameter(value, parameter.getType(), parameter.getName());
            }
        }
        return arguments;
    }

    private Object convertParameter(String value, Class<?> type, String name)
            throws IllegalArgumentException {
        try {
            if (type == String.class) return value;
            if (type == int.class || type == Integer.class) return Integer.valueOf(value);
            if (type == long.class || type == Long.class) return Long.valueOf(value);
            if (type == double.class || type == Double.class) return Double.valueOf(value);
            if (type == float.class || type == Float.class) return Float.valueOf(value);
            if (type == short.class || type == Short.class) return Short.valueOf(value);
            if (type == byte.class || type == Byte.class) return Byte.valueOf(value);
            if (type == boolean.class || type == Boolean.class) return Boolean.valueOf(value);
            if (type == char.class || type == Character.class) {
                if (value.length() == 1) return value.charAt(0);
                throw new IllegalArgumentException("Le paramètre '" + name
                        + "' doit contenir un seul caractère.");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Valeur invalide pour le paramètre '" + name + "'.", e);
        }
        throw new IllegalArgumentException("Type de paramètre non pris en charge pour '"
                + name + "' : " + type.getName());
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
}
