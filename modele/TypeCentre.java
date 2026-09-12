package modele;
 
public enum TypeCentre {
 
    MATERNITE("M"),
    OPERATOIRE("O"),
    NUTRITION("N");
 
    private final String code;
 
    TypeCentre(String code) {
        this.code = code;
    }
 
    public String getCode() { return code; }
 
    public static TypeCentre fromCode(String code) {
        for (TypeCentre t : values()) {
            if (t.code.equalsIgnoreCase(code.trim())) return t;
        }
        throw new IllegalArgumentException("Type de centre inconnu : " + code);
    }
}