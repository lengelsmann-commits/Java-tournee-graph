package vue;

import algo.Itineraire;
import algo.Tournee;
import modele.MonGraphe;
import org.graphstream.graph.Node;
import org.graphstream.ui.view.View;
import org.graphstream.ui.view.Viewer;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FenetrePrincipale extends JFrame {

    private MonGraphe graphe;
    private JPanel panelGraphe;

    private JCheckBox cbDuree, cbDistance, cbFiab;
    private JComboBox<String> comboDepart, comboArrivee;
    private JSlider sliderRisque;
    private JLabel labelRisque;
    private JTextArea zoneResultat;

    // Tournée (Q6)
    private JComboBox<Integer> comboCamions;
    private JComboBox<String> comboTypeLivraison;
    private JTextArea zoneTournee;

    private JLabel labelStats;

    public FenetrePrincipale() {
        super("Centres de santé - aide à la décision");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(creerPanelControles(), BorderLayout.WEST);

        panelGraphe = new JPanel(new BorderLayout());
        afficherPlaceholder();
        add(panelGraphe, BorderLayout.CENTER);

        setVisible(true);
    }

    // ----- Panneau de contrôles (gauche) -----
    private JComponent creerPanelControles() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        panel.add(blocCarte());
        panel.add(blocAffichage());
        panel.add(blocRisque());
        panel.add(blocItineraire());
        panel.add(blocTournee());
        panel.add(blocStats());

        JScrollPane scroll = new JScrollPane(panel);
        scroll.setPreferredSize(new Dimension(320, 0));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private JPanel blocCarte() {
        JPanel b = bloc("Carte");

        JPanel ligneN = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ligneN.add(new JLabel("Nombre de centres :"));
        JSpinner spinN = new JSpinner(new SpinnerNumberModel(15, 2, 200, 1));
        ligneN.add(spinN);
        b.add(ligneN);

        JButton btnAleatoire = new JButton("Générer un district aléatoire");
        btnAleatoire.addActionListener(e -> {
            graphe = MonGraphe.construireGrapheAleatoire((int) spinN.getValue());
            afficherGraphe();
        });
        b.add(btnAleatoire);

        JButton btnCharger = new JButton("Charger un fichier CSV");
        btnCharger.addActionListener(e -> {
            JFileChooser fc = new JFileChooser("data");
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    File f = fc.getSelectedFile();
                    graphe = MonGraphe.chargerGraphe(f.getAbsolutePath());
                    afficherGraphe();
                } catch (Exception ex) {
                    msg("Erreur de chargement : " + ex.getMessage());
                }
            }
        });
        b.add(btnCharger);
        return b;
    }

    private JPanel blocAffichage() {
        JPanel b = bloc("Afficher sur les routes");

        cbDuree    = new JCheckBox("Durée (min)", true);
        cbDistance = new JCheckBox("Distance (km)");
        cbFiab     = new JCheckBox("Fiabilité (/10)");
        for (JCheckBox cb : new JCheckBox[]{cbDuree, cbDistance, cbFiab}) {
            cb.addActionListener(e -> rafraichirAffichageAretes());
            b.add(cb);
        }
        return b;
    }

    private JPanel blocRisque() {
        JPanel b = bloc("Routes risquées");

        labelRisque = new JLabel("Surligner fiabilité < 50 %");
        b.add(labelRisque);

        sliderRisque = new JSlider(0, 100, 50);
        sliderRisque.setMajorTickSpacing(25);
        sliderRisque.setPaintTicks(true);
        sliderRisque.setPaintLabels(true);
        sliderRisque.addChangeListener(e -> {
            int pct = sliderRisque.getValue();
            labelRisque.setText("Surligner fiabilité < " + pct + " %");
            if (graphe != null) graphe.highlightRisquees(pct / 10);
        });
        b.add(sliderRisque);
        return b;
    }

    private JPanel blocItineraire() {
        JPanel b = bloc("Itinéraire");

        JPanel ligneDep = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ligneDep.add(new JLabel("Départ :"));
        comboDepart = new JComboBox<>();
        ligneDep.add(comboDepart);
        b.add(ligneDep);

        JPanel ligneArr = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ligneArr.add(new JLabel("Arrivée :"));
        comboArrivee = new JComboBox<>();
        ligneArr.add(comboArrivee);
        b.add(ligneArr);

        JButton btnItin = new JButton("Calculer les itinéraires");
        btnItin.addActionListener(e -> calculerItineraires());
        b.add(btnItin);

        zoneResultat = zoneTexte("Sélectionnez un départ et une arrivée.");
        b.add(new JScrollPane(zoneResultat));
        return b;
    }

    private JPanel blocTournee() {
        JPanel b = bloc("Tournée de livraison");

        JPanel ligneCam = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ligneCam.add(new JLabel("Camions :"));
        comboCamions = new JComboBox<>(new Integer[]{1, 2});
        ligneCam.add(comboCamions);
        b.add(ligneCam);

        JPanel ligneType = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ligneType.add(new JLabel("Livrer :"));
        comboTypeLivraison = new JComboBox<>(new String[]{
            "Tous les centres", "Maternités (M)", "Blocs opératoires (O)", "Centres nutrition (N)"});
        ligneType.add(comboTypeLivraison);
        b.add(ligneType);

        JButton btnTournee = new JButton("Calculer la tournée");
        btnTournee.addActionListener(e -> calculerTournee());
        b.add(btnTournee);

        zoneTournee = zoneTexte("Choisissez les camions et le type à livrer.");
        b.add(new JScrollPane(zoneTournee));
        return b;
    }

    private JPanel blocStats() {
        JPanel b = bloc("Statistiques");
        labelStats = new JLabel("Aucun graphe chargé.");
        b.add(labelStats);
        return b;
    }

    // ----- Logique -----

    private void afficherGraphe() {
        panelGraphe.removeAll();

        Viewer viewer = new Viewer(graphe, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
        viewer.enableAutoLayout();
        View view = viewer.addDefaultView(false);

        // Clic droit sur un centre -> fenêtre d'infos (Q5)
        final org.graphstream.ui.swingViewer.DefaultView dv =
                (org.graphstream.ui.swingViewer.DefaultView) view;
        dv.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent ev) {
                if (javax.swing.SwingUtilities.isRightMouseButton(ev)) {
                    org.graphstream.ui.graphicGraph.GraphicElement el =
                        dv.findNodeOrSpriteAt(ev.getX(), ev.getY());
                    if (el != null) {
                        new FenetreInfoCentre(FenetrePrincipale.this, graphe, el.getId())
                            .setVisible(true);
                    }
                }
            }
        });

        panelGraphe.add((Component) view, BorderLayout.CENTER);
        panelGraphe.revalidate();
        panelGraphe.repaint();

        remplirCombos();
        rafraichirAffichageAretes();
        graphe.highlightRisquees(sliderRisque.getValue() / 10);
        mettreAJourStats();
        zoneResultat.setText("Sélectionnez un départ et une arrivée.");
        zoneTournee.setText("Choisissez les camions et le type à livrer.");
    }

    private void remplirCombos() {
        List<String> ids = new ArrayList<>();
        for (Node n : graphe.getEachNode()) ids.add(n.getId());
        // Tri naturel S1, S2, ... S10 (par numéro)
        ids.sort((a, c) -> {
            try {
                return Integer.compare(Integer.parseInt(a.substring(1)),
                                       Integer.parseInt(c.substring(1)));
            } catch (Exception ex) { return a.compareTo(c); }
        });
        comboDepart.removeAllItems();
        comboArrivee.removeAllItems();
        for (String id : ids) { comboDepart.addItem(id); comboArrivee.addItem(id); }
        if (ids.size() > 1) comboArrivee.setSelectedIndex(1);
    }

    private void rafraichirAffichageAretes() {
        if (graphe != null)
            graphe.setAffichageAretes(cbDuree.isSelected(), cbDistance.isSelected(), cbFiab.isSelected());
    }

    private void calculerItineraires() {
        if (graphe == null) { msg("Chargez d'abord un graphe."); return; }
        String dep = (String) comboDepart.getSelectedItem();
        String arr = (String) comboArrivee.getSelectedItem();
        if (dep == null || arr == null) return;
        if (dep.equals(arr)) { msg("Choisissez deux centres différents."); return; }

        graphe.resetAretes();
        graphe.highlightRisquees(sliderRisque.getValue() / 10);

        Itineraire.Resultat rapide = Itineraire.plusCourtEnDuree(graphe, dep, arr);
        Itineraire.Resultat sur    = Itineraire.plusSurEtRapide(graphe, dep, arr);

        StringBuilder sb = new StringBuilder();
        sb.append("PLUS RAPIDE (duree)\n");
        if (rapide.possible)
            sb.append(rapide.cheminTexte()).append("\n")
              .append(String.format("= %.0f min%n%n", rapide.cout));
        else sb.append("Aucun chemin\n\n");

        sb.append("PLUS SUR (duree estimee)\n");
        if (sur.possible)
            sb.append(sur.cheminTexte()).append("\n")
              .append(String.format("= %.1f min", sur.cout));
        else sb.append("Aucun chemin");

        zoneResultat.setText(sb.toString());
        if (sur.possible) graphe.colorerChemin(sur.chemin);
    }

    private void calculerTournee() {
        if (graphe == null) { msg("Chargez d'abord un graphe."); return; }
        if (graphe.getNode(Tournee.DEPOT) == null) {
            msg("Le dépôt " + Tournee.DEPOT + " n'existe pas dans ce graphe."); return;
        }

        int nbCamions = (int) comboCamions.getSelectedItem();
        String filtre = filtreLivraison();

        Tournee.Resultat r = Tournee.calculer(graphe, nbCamions, filtre);
        if (!r.possible) {
            zoneTournee.setText("Aucune tournée possible : certains centres sont inaccessibles.");
            return;
        }

        // Visualisation : on colore les segments réellement empruntés par chaque camion.
        graphe.resetAretes();
        for (int t = 0; t < r.ordres.size(); t++) {
            List<String> ordre = r.ordres.get(t);
            for (int i = 0; i < ordre.size() - 1; i++) {
                Itineraire.Resultat seg = Itineraire.plusCourtEnDuree(graphe, ordre.get(i), ordre.get(i + 1));
                if (seg.possible) graphe.colorerTournee(seg.chemin, t);
            }
        }

        // Texte récapitulatif.
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Camion le plus lent : %.0f min%n%n", r.valeur));
        for (int t = 0; t < r.ordres.size(); t++) {
            sb.append("Camion ").append(t + 1)
              .append(String.format(" (%.0f min)%n", r.couts.get(t)));
            sb.append(String.join(" -> ", r.ordres.get(t))).append("\n\n");
        }
        zoneTournee.setText(sb.toString().trim());
    }

    // Renvoie le code de type sélectionné ("M"/"O"/"N") ou null pour "tous les centres".
    private String filtreLivraison() {
        switch (comboTypeLivraison.getSelectedIndex()) {
            case 1:  return "M";
            case 2:  return "O";
            case 3:  return "N";
            default: return null;
        }
    }

    private void mettreAJourStats() {
        int nbM = 0, nbO = 0, nbN = 0;
        for (Node n : graphe.getEachNode()) {
            String t = (String) n.getAttribute("type");
            if ("M".equals(t)) nbM++;
            else if ("O".equals(t)) nbO++;
            else if ("N".equals(t)) nbN++;
        }
        labelStats.setText("<html>Maternités (M) : " + nbM
            + "<br>Blocs opératoires (O) : " + nbO
            + "<br>Centres nutrition (N) : " + nbN + "</html>");
    }

    private void afficherPlaceholder() {
        panelGraphe.removeAll();
        JLabel ph = new JLabel("Générez ou chargez un district pour commencer", SwingConstants.CENTER);
        panelGraphe.add(ph, BorderLayout.CENTER);
    }

    // ----- Helpers UI -----

    private JPanel bloc(String titre) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createTitledBorder(titre));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    private JTextArea zoneTexte(String texteInitial) {
        JTextArea zone = new JTextArea(6, 18);
        zone.setEditable(false);
        zone.setLineWrap(true);
        zone.setWrapStyleWord(true);
        zone.setText(texteInitial);
        return zone;
    }

    private void msg(String texte) {
        JOptionPane.showMessageDialog(this, texte);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(FenetrePrincipale::new);
    }
}
