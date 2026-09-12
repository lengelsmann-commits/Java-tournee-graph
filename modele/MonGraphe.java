package modele;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;

import java.io.IOException;
import java.util.Random;

public class MonGraphe extends SingleGraph {

    private static final String STYLE =
        "node { size: 18px; text-size: 12; }" +
        "node.M { fill-color: #E63946; }" +   // maternité : rouge
        "node.O { fill-color: #457B9D; }" +   // bloc opératoire : bleu
        "node.N { fill-color: #F4A261; }" +   // nutrition : orange
        "edge { fill-color: #aaaaaa; }" +
        "edge.risquee { fill-color: #E63946; size: 3px; }" +
        "edge.chemin { fill-color: #2A9D8F; size: 4px; }" +
        "edge.t0 { fill-color: #2A9D8F; size: 4px; }" +   // tournée camion 1 : vert
        "edge.t1 { fill-color: #9D4EDD; size: 4px; }";    // tournée camion 2 : violet

    public MonGraphe(String id) {
        super(id);
        setAttribute("ui.stylesheet", STYLE);
    }

    public static MonGraphe chargerGraphe(String nomFichier) throws IOException {
        MonGraphe g = new MonGraphe(nomFichier);
        utils.ImportCSV.remplir(g, nomFichier);
        return g;
    }

    public static MonGraphe construireGrapheAleatoire(int nbSommets) {
        Random rand = new Random();
        MonGraphe g = new MonGraphe("aleatoire");

        for (int i = 1; i <= nbSommets; i++) {
            String type = tirerType(rand);
            Node node = g.addNode("S" + i);
            node.setAttribute("type",     (Object) type);
            node.setAttribute("ui.label", (Object) ("S" + i));
            node.setAttribute("ui.class", (Object) type);
        }

        int edgeId = 0;

        // 1) Arbre couvrant : on relie chaque nouveau sommet à un sommet déjà placé
        //    -> garantit que le graphe est connexe (tout est relié entre soi)
        for (int i = 2; i <= nbSommets; i++) {
            int j = 1 + rand.nextInt(i - 1); // un sommet parmi S1..S(i-1)
            ajouterAreteAleatoire(g, "e" + edgeId++, "S" + i, "S" + j, rand);
        }

        // 2) Arêtes supplémentaires aléatoires (proba 1/5), sans recréer une arête existante
        for (int i = 1; i <= nbSommets; i++) {
            for (int j = i + 1; j <= nbSommets; j++) {
                if (g.getNode("S" + i).hasEdgeBetween("S" + j)) continue;
                if (rand.nextInt(5) == 0) {
                    ajouterAreteAleatoire(g, "e" + edgeId++, "S" + i, "S" + j, rand);
                }
            }
        }

        return g;
    }

    // Affiche une combinaison d'infos sur les arêtes selon les cases cochées
    // Exemple : duree=true, distance=true -> "50min | 25km"
    public void setAffichageAretes(boolean duree, boolean distance, boolean fiabilite) {
        for (Edge e : getEachEdge()) {
            StringBuilder sb = new StringBuilder();
            if (duree)     ajout(sb, e.getAttribute("duree") + "min");
            if (distance)  ajout(sb, e.getAttribute("distance") + "km");
            if (fiabilite) ajout(sb, e.getAttribute("fiabilite") + "/10");
            e.setAttribute("ui.label", (Object) sb.toString());
        }
    }

    private void ajout(StringBuilder sb, String valeur) {
        if (sb.length() > 0) sb.append(" | ");
        sb.append(valeur);
    }

    // Réinitialise la couleur de toutes les arêtes (enlève le surlignage de chemin/risque)
    public void resetAretes() {
        for (Edge e : getEachEdge()) e.removeAttribute("ui.class");
    }

    // Colore en vert les arêtes d'un chemin donné (liste de nœuds dans l'ordre)
    public void colorerChemin(org.graphstream.graph.Path chemin) {
        if (chemin == null) return;
        for (Edge e : chemin.getEdgePath()) {
            e.setAttribute("ui.class", (Object) "chemin");
        }
    }

    // Colore les arêtes d'un segment de tournée selon le camion (0 = vert, 1 = violet)
    public void colorerTournee(org.graphstream.graph.Path chemin, int camion) {
        if (chemin == null) return;
        String classe = "t" + camion;
        for (Edge e : chemin.getEdgePath()) {
            e.setAttribute("ui.class", (Object) classe);
        }
    }

    // Met en rouge les routes dont la fiabilité est < seuil (seuil entier x10, ex: 5 = 50%)
    public void highlightRisquees(int seuil) {
        for (Edge e : getEachEdge()) {
            int fiab = (int) e.getAttribute("fiabilite");
            if (fiab <= seuil) e.setAttribute("ui.class", (Object) "risquee");
            else              e.removeAttribute("ui.class");
        }
    }

    // Crée une arête avec des valeurs aléatoires (fiabilité 2-10, distance 10-50, durée 10-120)
    private static void ajouterAreteAleatoire(MonGraphe g, String id, String a, String b, Random rand) {
        int fiab = 2 + rand.nextInt(9);
        int dist = 10 + rand.nextInt(41);
        int dur  = 10 + rand.nextInt(111);
        Edge edge = g.addEdge(id, a, b);
        edge.setAttribute("fiabilite", (Object) fiab);
        edge.setAttribute("distance",  (Object) dist);
        edge.setAttribute("duree",     (Object) dur);
        edge.setAttribute("dureeEstimee", (Object) (dur * (20.0 - fiab) / 10.0));
        edge.setAttribute("ui.label",  (Object) (dur + "min"));
    }

    private static String tirerType(Random rand) {
        int t = rand.nextInt(5);
        if (t < 3) return "M";
        if (t == 3) return "O";
        return "N";
    }
}