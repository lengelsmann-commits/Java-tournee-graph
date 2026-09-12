package vue;

import algo.Itineraire;
import modele.MonGraphe;
import org.graphstream.graph.Node;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Boîte de dialogue affichant les informations détaillées d'un centre (Q5)
public class FenetreInfoCentre extends JDialog {

    /**
     * Construit la boîte de dialogue modale d'informations d'un centre (Q5) : type du centre,
     * centres les plus proches de chaque type (en durée et durée estimée), et tableaux des
     * distances et des durées vers tous les autres centres, triés par type.
     *
     * @param parent   la fenêtre parente (pour le centrage et la modalité)
     * @param g        le graphe du district
     * @param idCentre l'id du centre dont on affiche les informations
     */
    public FenetreInfoCentre(Frame parent, MonGraphe g, String idCentre) {
        super(parent, "Infos centre " + idCentre, true);
        setSize(560, 620);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        String type = (String) g.getNode(idCentre).getAttribute("type");

        // Calculs des durées / durées estimées / distances vers tous les centres
        Map<String, Double> durees   = Itineraire.toutesDurees(g, idCentre);
        Map<String, Double> dureesEst = Itineraire.toutesDureesEstimees(g, idCentre);
        Map<String, Double> distances = Itineraire.toutesDistances(g, idCentre);

        // ----- En-tête : id + type + plus proches par type -----
        JPanel haut = new JPanel();
        haut.setLayout(new BoxLayout(haut, BoxLayout.Y_AXIS));
        haut.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel titre = new JLabel("Centre " + idCentre + " — " + libelleType(type));
        titre.setFont(titre.getFont().deriveFont(Font.BOLD, 16f));
        haut.add(titre);
        haut.add(Box.createVerticalStrut(10));

        haut.add(lignePlusProche(g, "Maternité la plus proche",      "M", durees, dureesEst, idCentre));
        haut.add(lignePlusProche(g, "Bloc opératoire le plus proche", "O", durees, dureesEst, idCentre));
        haut.add(lignePlusProche(g, "Centre nutrition le plus proche", "N", durees, dureesEst, idCentre));

        add(haut, BorderLayout.NORTH);

        // ----- Onglets : tableau distances et tableau durées -----
        JTabbedPane onglets = new JTabbedPane();
        onglets.addTab("Distances (km)", tableauTrieParType(g, distances, idCentre, "km"));
        onglets.addTab("Durées (min)",   tableauTrieParType(g, durees, idCentre, "min"));
        add(onglets, BorderLayout.CENTER);

        JButton fermer = new JButton("Fermer");
        fermer.addActionListener(e -> dispose());
        JPanel bas = new JPanel();
        bas.add(fermer);
        add(bas, BorderLayout.SOUTH);
    }

    // Ligne "Maternité la plus proche : S7 (durée 45 min, durée estimée 63.0 min)"
    private JLabel lignePlusProche(MonGraphe g, String label, String typeCible,
                                   Map<String, Double> durees, Map<String, Double> dureesEst,
                                   String soiMeme) {
        String meilleur = null;
        double meilleureDuree = Double.POSITIVE_INFINITY;

        for (Node n : g.getEachNode()) {
            if (n.getId().equals(soiMeme)) continue;
            if (!typeCible.equals(n.getAttribute("type"))) continue;
            double d = durees.getOrDefault(n.getId(), Double.POSITIVE_INFINITY);
            if (d < meilleureDuree) { meilleureDuree = d; meilleur = n.getId(); }
        }

        String texte;
        if (meilleur == null || Double.isInfinite(meilleureDuree)) {
            texte = label + " : aucun accessible";
        } else {
            double de = dureesEst.getOrDefault(meilleur, Double.POSITIVE_INFINITY);
            String deTexte = Double.isInfinite(de) ? "∞ (aucune route sûre)" : String.format("%.1f min", de);
            texte = String.format("%s : %s  (durée %.0f min, durée estimée %s)",
                                  label, meilleur, meilleureDuree, deTexte);
        }
        JLabel l = new JLabel(texte);
        l.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        return l;
    }

    // Tableau (Id | Valeur) trié par type de centre, avec séparateurs de section
    private JScrollPane tableauTrieParType(MonGraphe g, Map<String, Double> valeurs,
                                           String soiMeme, String unite) {
        DefaultTableModel modele = new DefaultTableModel(new String[]{"Type", "Id", unite}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        for (String type : new String[]{"M", "O", "N"}) {
            List<Node> duType = new ArrayList<>();
            for (Node n : g.getEachNode()) {
                if (n.getId().equals(soiMeme)) continue;
                if (type.equals(n.getAttribute("type"))) duType.add(n);
            }
            // Tri par valeur croissante
            duType.sort((a, b) -> Double.compare(
                    valeurs.getOrDefault(a.getId(), Double.POSITIVE_INFINITY),
                    valeurs.getOrDefault(b.getId(), Double.POSITIVE_INFINITY)));

            boolean premier = true;
            for (Node n : duType) {
                double v = valeurs.getOrDefault(n.getId(), Double.POSITIVE_INFINITY);
                String valTexte = Double.isInfinite(v) ? "∞" : String.format("%.0f", v);
                modele.addRow(new Object[]{ premier ? libelleType(type) : "", n.getId(), valTexte });
                premier = false;
            }
        }

        JTable table = new JTable(modele);
        table.setRowHeight(22);
        table.getColumnModel().getColumn(0).setPreferredWidth(170);
        return new JScrollPane(table);
    }

    private String libelleType(String code) {
        switch (code) {
            case "M": return "Maternité";
            case "O": return "Bloc opératoire";
            case "N": return "Centre de nutrition";
            default:  return code;
        }
    }
}