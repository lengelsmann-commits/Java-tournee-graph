package algo;

import modele.MonGraphe;
import org.graphstream.algorithm.Dijkstra;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;
import org.graphstream.graph.Path;

public class Itineraire {

    // Plus court chemin en DUREE (minutes), via le Dijkstra de GraphStream
    public static Resultat plusCourtEnDuree(MonGraphe g, String depart, String arrivee) {
        Dijkstra dijkstra = new Dijkstra(Dijkstra.Element.EDGE, null, "duree");
        dijkstra.init(g);
        dijkstra.setSource(g.getNode(depart));
        dijkstra.compute();

        Node cible = g.getNode(arrivee);
        Path chemin = dijkstra.getPath(cible);
        double cout = dijkstra.getPathLength(cible);

        dijkstra.clear();
        if (chemin == null || chemin.empty()) return Resultat.impossible();
        return new Resultat(chemin, cout);
    }

    // Plus court chemin selon la DUREE ESTIMEE (pondérée par la fiabilité).
    // Les routes trop risquées (fiabilité < 0.25) sont exclues.
    public static Resultat plusSurEtRapide(MonGraphe g, String depart, String arrivee) {
        preparerPoidsSur(g);

        Dijkstra dijkstra = new Dijkstra(Dijkstra.Element.EDGE, null, "poidsSur");
        dijkstra.init(g);
        dijkstra.setSource(g.getNode(depart));
        dijkstra.compute();

        Node cible = g.getNode(arrivee);
        Path chemin = dijkstra.getPath(cible);
        double cout = dijkstra.getPathLength(cible);

        dijkstra.clear();
        if (chemin == null || chemin.empty() || Double.isInfinite(cout)) return Resultat.impossible();
        return new Resultat(chemin, cout);
    }

    /**
     * Calcule la distance minimale (en km) depuis un centre vers tous les autres.
     *
     * @param g      le graphe du district
     * @param source l'id du centre de départ
     * @return une map associant l'id de chaque centre à sa distance minimale depuis {@code source}
     *         (l'infini si le centre n'est pas accessible)
     */
    public static java.util.Map<String, Double> toutesDistances(MonGraphe g, String source) {
        return dijkstraVersTous(g, source, "distance");
    }

    /**
     * Calcule la durée minimale (en minutes) depuis un centre vers tous les autres.
     *
     * @param g      le graphe du district
     * @param source l'id du centre de départ
     * @return une map associant l'id de chaque centre à sa durée minimale depuis {@code source}
     *         (l'infini si le centre n'est pas accessible)
     */
    public static java.util.Map<String, Double> toutesDurees(MonGraphe g, String source) {
        return dijkstraVersTous(g, source, "duree");
    }

    /**
     * Calcule la durée estimée minimale (durée pondérée par la fiabilité) depuis un centre
     * vers tous les autres. Les routes trop risquées (fiabilité &lt; 0.25) sont exclues.
     *
     * @param g      le graphe du district
     * @param source l'id du centre de départ
     * @return une map associant l'id de chaque centre à sa durée estimée minimale depuis
     *         {@code source} (l'infini si aucun chemin sûr n'existe)
     */
    public static java.util.Map<String, Double> toutesDureesEstimees(MonGraphe g, String source) {
        preparerPoidsSur(g);
        return dijkstraVersTous(g, source, "poidsSur");
    }

    // Renseigne l'attribut "poidsSur" de chaque arête : sa durée estimée, ou +∞ si la route
    // est trop risquée (fiabilité < 0.25, soit < 3 sur l'échelle /10) et ne doit pas être empruntée.
    private static void preparerPoidsSur(MonGraphe g) {
        for (Edge e : g.getEachEdge()) {
            int fiab = (int) e.getAttribute("fiabilite");
            double poids = (fiab < 3)
                ? Double.POSITIVE_INFINITY
                : (double) e.getAttribute("dureeEstimee");
            e.setAttribute("poidsSur", (Object) poids);
        }
    }

    // Dijkstra GraphStream qui renvoie le coût vers chaque sommet
    private static java.util.Map<String, Double> dijkstraVersTous(MonGraphe g, String source, String attribut) {
        Dijkstra dijkstra = new Dijkstra(Dijkstra.Element.EDGE, null, attribut);
        dijkstra.init(g);
        dijkstra.setSource(g.getNode(source));
        dijkstra.compute();

        java.util.Map<String, Double> resultat = new java.util.HashMap<>();
        for (Node n : g.getEachNode()) {
            resultat.put(n.getId(), dijkstra.getPathLength(n));
        }
        dijkstra.clear();
        return resultat;
    }

    // Petite classe pour transporter le résultat (chemin + coût)
    public static class Resultat {
        public final Path chemin;
        public final double cout;
        public final boolean possible;

        private Resultat(Path chemin, double cout) {
            this.chemin = chemin;
            this.cout = cout;
            this.possible = true;
        }

        private Resultat() {
            this.chemin = null;
            this.cout = Double.POSITIVE_INFINITY;
            this.possible = false;
        }

        public static Resultat impossible() {
            return new Resultat();
        }

        // Renvoie le chemin sous forme "S1 -> S4 -> S7"
        public String cheminTexte() {
            if (!possible) return "Aucun chemin possible";
            StringBuilder sb = new StringBuilder();
            boolean premier = true;
            for (Node n : chemin.getNodePath()) {
                if (!premier) sb.append(" -> ");
                sb.append(n.getId());
                premier = false;
            }
            return sb.toString();
        }
    }
}