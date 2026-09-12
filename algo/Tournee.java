package algo;

import modele.MonGraphe;
import org.graphstream.graph.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Calcul de tournées de livraison (Q6).
 *
 * <p>Un ou deux camions partent tous du centre {@value #DEPOT} et doivent livrer un ensemble
 * de dispensaires (tous, ou ceux d'un type donné). Les camions peuvent repasser par des
 * centres déjà visités : on raisonne donc sur les plus courts chemins en durée entre centres.
 * L'objectif est de minimiser la durée du camion le plus lent.</p>
 *
 * <p>L'algorithme de base est un glouton « plus proche voisin », amélioré par une optimisation
 * locale 2-opt. Pour deux camions, on répartit les livraisons en faisant systématiquement
 * avancer le camion le moins chargé vers son dispensaire non livré le plus proche.</p>
 */
public class Tournee {

    /** Centre de départ imposé pour tous les camions. */
    public static final String DEPOT = "S1";

    private static final double EPS = 1e-9;

    /** Résultat d'un calcul de tournée : ordre de visite et durée pour chaque camion. */
    public static class Resultat {
        /** Pour chaque camion, l'ordre des centres livrés (commence par le dépôt). */
        public final List<List<String>> ordres;
        /** Pour chaque camion, la durée totale de sa tournée (en minutes). */
        public final List<Double> couts;
        /** Durée du camion le plus lent (la valeur à minimiser). */
        public final double valeur;
        /** Vrai si une tournée valide a été trouvée. */
        public final boolean possible;

        private Resultat(List<List<String>> ordres, List<Double> couts) {
            this.ordres = ordres;
            this.couts = couts;
            this.valeur = couts.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
            this.possible = true;
        }

        private Resultat() {
            this.ordres = null;
            this.couts = null;
            this.valeur = Double.POSITIVE_INFINITY;
            this.possible = false;
        }

        /** @return un résultat indiquant qu'aucune tournée n'est possible. */
        public static Resultat impossible() { return new Resultat(); }
    }

    /**
     * Calcule une tournée pour livrer les dispensaires demandés.
     *
     * @param g          le graphe du district
     * @param nbCamions  le nombre de camions (1 ou 2)
     * @param typeFiltre le type de centre à livrer ("M", "O" ou "N"), ou {@code null} pour tous
     * @return la tournée trouvée, ou {@link Resultat#impossible()} si un centre à livrer est
     *         inaccessible depuis le dépôt
     */
    public static Resultat calculer(MonGraphe g, int nbCamions, String typeFiltre) {
        if (g.getNode(DEPOT) == null) return Resultat.impossible();

        // Matrice des plus courtes durées entre tous les centres.
        Map<String, Map<String, Double>> dist = matriceDurees(g);

        // Centres à livrer (selon le filtre de type). Le dépôt est livré au départ : on le retire.
        List<String> cibles = new ArrayList<>();
        for (Node n : g.getEachNode()) {
            if (typeFiltre == null || typeFiltre.equals(n.getAttribute("type"))) {
                cibles.add(n.getId());
            }
        }
        cibles.remove(DEPOT);

        // Si un centre à livrer est inaccessible depuis le dépôt, pas de tournée possible.
        for (String c : cibles) {
            if (Double.isInfinite(dureeEntre(dist, DEPOT, c))) return Resultat.impossible();
        }

        return (nbCamions <= 1)
            ? unCamion(dist, cibles)
            : deuxCamions(dist, cibles);
    }

    // --- Un camion : glouton plus proche voisin, puis amélioration 2-opt ---
    private static Resultat unCamion(Map<String, Map<String, Double>> dist, List<String> cibles) {
        List<String> ordre = glouton(dist, DEPOT, new ArrayList<>(cibles));
        //ameliorer2opt(dist, ordre);

        List<List<String>> ordres = new ArrayList<>();
        ordres.add(ordre);
        List<Double> couts = new ArrayList<>();
        couts.add(cout(dist, ordre));
        return new Resultat(ordres, couts);
    }

    // --- Deux camions : on fait avancer à chaque étape le camion le moins chargé ---
    private static Resultat deuxCamions(Map<String, Map<String, Double>> dist, List<String> cibles) {
        List<List<String>> ordres = new ArrayList<>();
        ordres.add(new ArrayList<>(List.of(DEPOT)));
        ordres.add(new ArrayList<>(List.of(DEPOT)));
        double[] totaux = {0.0, 0.0};
        String[] courant = {DEPOT, DEPOT};

        Set<String> restants = new HashSet<>(cibles);
        while (!restants.isEmpty()) {
            int t = (totaux[0] <= totaux[1]) ? 0 : 1;     // le camion le moins chargé joue
            String prochain = plusProche(dist, courant[t], restants);
            if (prochain == null) {                       // injoignable pour ce camion : l'autre essaie
                t = 1 - t;
                prochain = plusProche(dist, courant[t], restants);
                if (prochain == null) return Resultat.impossible();
            }
            ordres.get(t).add(prochain);
            totaux[t] += dureeEntre(dist, courant[t], prochain);
            courant[t] = prochain;
            restants.remove(prochain);
        }

        // Optimisation locale de chaque tournée indépendamment.
        ameliorer2opt(dist, ordres.get(0));
        ameliorer2opt(dist, ordres.get(1));

        List<Double> couts = new ArrayList<>();
        couts.add(cout(dist, ordres.get(0)));
        couts.add(cout(dist, ordres.get(1)));
        return new Resultat(ordres, couts);
    }

    // Glouton : depuis "depart", visite à chaque fois le centre restant le plus proche en durée.
    private static List<String> glouton(Map<String, Map<String, Double>> dist,
                                        String depart, List<String> restants) {
        List<String> ordre = new ArrayList<>();
        ordre.add(depart);
        String courant = depart;
        while (!restants.isEmpty()) {
            String prochain = plusProche(dist, courant, new HashSet<>(restants));
            ordre.add(prochain);
            restants.remove(prochain);
            courant = prochain;
        }
        return ordre;
    }

    // Centre de "candidats" le plus proche de "depuis" (null si aucun n'est accessible).
    private static String plusProche(Map<String, Map<String, Double>> dist,
                                     String depuis, Set<String> candidats) {
        String meilleur = null;
        double meilleureDuree = Double.POSITIVE_INFINITY;
        for (String c : candidats) {
            double d = dureeEntre(dist, depuis, c);
            if (d < meilleureDuree) { meilleureDuree = d; meilleur = c; }
        }
        return (meilleur != null && !Double.isInfinite(meilleureDuree)) ? meilleur : null;
    }

    // 2-opt sur un chemin ouvert dont le premier élément (le dépôt) est fixe.
    // Inverse des segments tant que cela raccourcit la durée totale.
    private static void ameliorer2opt(Map<String, Map<String, Double>> dist, List<String> ordre) {
        int n = ordre.size();
        if (n < 4) return; // rien à réarranger
        boolean ameliore = true;
        while (ameliore) {
            ameliore = false;
            for (int i = 1; i < n - 1; i++) {
                for (int j = i + 1; j < n; j++) {
                    String a = ordre.get(i - 1);
                    String b = ordre.get(i);
                    String c = ordre.get(j);
                    String d = (j + 1 < n) ? ordre.get(j + 1) : null;

                    double avant = dureeEntre(dist, a, b) + (d != null ? dureeEntre(dist, c, d) : 0);
                    double apres = dureeEntre(dist, a, c) + (d != null ? dureeEntre(dist, b, d) : 0);

                    if (apres < avant - EPS) {
                        inverser(ordre, i, j);
                        ameliore = true;
                    }
                }
            }
        }
    }

    private static void inverser(List<String> liste, int i, int j) {
        while (i < j) {
            String tmp = liste.get(i);
            liste.set(i, liste.get(j));
            liste.set(j, tmp);
            i++; j--;
        }
    }

    // Durée totale d'une tournée (somme des plus courtes durées entre centres consécutifs).
    private static double cout(Map<String, Map<String, Double>> dist, List<String> ordre) {
        double total = 0;
        for (int i = 0; i < ordre.size() - 1; i++) {
            total += dureeEntre(dist, ordre.get(i), ordre.get(i + 1));
        }
        return total;
    }

    private static double dureeEntre(Map<String, Map<String, Double>> dist, String a, String b) {
        return dist.get(a).getOrDefault(b, Double.POSITIVE_INFINITY);
    }

    // Plus courtes durées entre chaque paire de centres (un Dijkstra par sommet source).
    private static Map<String, Map<String, Double>> matriceDurees(MonGraphe g) {
        Map<String, Map<String, Double>> matrice = new HashMap<>();
        for (Node n : g.getEachNode()) {
            matrice.put(n.getId(), Itineraire.toutesDurees(g, n.getId()));
        }
        return matrice;
    }
}
