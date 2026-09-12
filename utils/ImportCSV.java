package utils;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;
import modele.MonGraphe;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// Lit un fichier CSV (matrice d'adjacence) et remplit un MonGraphe.
public class ImportCSV {

    // Remplit le graphe g à partir du fichier CSV donné.
    public static void remplir(MonGraphe g, String nomFichier) throws IOException {
        List<String[]> lignes = lireLignesUtiles(nomFichier);

        if (lignes.isEmpty())
            throw new IllegalArgumentException("Fichier vide ou sans données : " + nomFichier);

        // Passe 1 : les noeuds
        for (String[] cols : lignes) {
            if (cols.length < 2)
                throw new IllegalArgumentException("Ligne mal formée (id ou type manquant) : " + String.join(";", cols));
            String id   = cols[0].trim();
            String type = cols[1].trim();
            Node node = g.addNode(id);
            node.setAttribute("type", (Object) type);
            node.setAttribute("ui.label", (Object) id);
            node.setAttribute("ui.class", (Object) type);
        }

        // Passe 2 : les arêtes (triangle supérieur)
        int edgeId = 0;
        for (int i = 0; i < lignes.size(); i++) {
            String[] cols = lignes.get(i);
            for (int j = i + 1; j < lignes.size(); j++) {
                int col = 2 + j;
                if (col >= cols.length) continue;
                String cell = cols[col].trim();
                if (cell.equals("0") || cell.isEmpty()) continue;

                String[] vals = cell.split(",");
                if (vals.length != 3)
                    throw new IllegalArgumentException(
                        "Triplet invalide ligne " + (i + 1) + " : " + cell);

                try {
                    int fiab = Integer.parseInt(vals[0].trim());
                    int dist = Integer.parseInt(vals[1].trim());
                    int dur  = Integer.parseInt(vals[2].trim());

                    Edge edge = g.addEdge("e" + edgeId++, cols[0].trim(), lignes.get(j)[0].trim());
                    edge.setAttribute("fiabilite",    (Object) fiab);
                    edge.setAttribute("distance",     (Object) dist);
                    edge.setAttribute("duree",        (Object) dur);
                    edge.setAttribute("dureeEstimee", (Object) (dur * (20.0 - fiab) / 10.0));
                    edge.setAttribute("ui.label",     (Object) (dur + "min"));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                        "Valeur non numérique ligne " + (i + 1) + " : " + cell, e);
                }
            }
        }
    }

    // Lit le fichier, ignore les lignes vides et les commentaires (//), découpe par ";".
    private static List<String[]> lireLignesUtiles(String nomFichier) throws IOException {
        List<String[]> lignes = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(nomFichier))) {
            String ligne;
            while ((ligne = reader.readLine()) != null) {
                ligne = ligne.trim();
                if (!ligne.isEmpty() && !ligne.startsWith("//"))
                    lignes.add(ligne.split(";"));
            }
        }
        return lignes;
    }
}