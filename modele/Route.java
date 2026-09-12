package modele;
 
public class Route {
 
    private final Centre origine;
    private final Centre destination;
    private final int fiabilite;   // entier x10, ex: 4 = 0.4 = 40%
    private final int distanceKm;
    private final int dureeMn;
 
    public Route(Centre origine, Centre destination, int fiabilite, int distanceKm, int dureeMn) {
        this.origine     = origine;
        this.destination = destination;
        this.fiabilite   = fiabilite;
        this.distanceKm  = distanceKm;
        this.dureeMn     = dureeMn;
    }
 
    public Centre getOrigine()     { return origine; }
    public Centre getDestination() { return destination; }
    public int getFiabilite()      { return fiabilite; }
    public int getDistanceKm()     { return distanceKm; }
    public int getDureeMn()        { return dureeMn; }
 
    public Centre getAutreBout(Centre c) {
        if (c.equals(origine))     return destination;
        if (c.equals(destination)) return origine;
        return null;
    }
 
    @Override
    public String toString() {
        return origine.getId() + " <-> " + destination.getId()
            + " [fiab=" + fiabilite + "/10, dist=" + distanceKm + "km, durée=" + dureeMn + "min]";
    }
}