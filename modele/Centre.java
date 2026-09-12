package modele;
 
public class Centre {
 
    private final String id;
    private final TypeCentre type;
 
    public Centre(String id, TypeCentre type) {
        this.id   = id;
        this.type = type;
    }
 
    public String getId()        { return id; }
    public TypeCentre getType()  { return type; }
 
    @Override
    public String toString() { return id + " (" + type.getCode() + ")"; }
 
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Centre)) return false;
        return id.equals(((Centre) obj).id);
    }
 
    @Override
    public int hashCode() { return id.hashCode(); }
}
 