package android.wyse.face.models;

public class SelectorTypes {

    private String catName;
    private String catId;
    public SelectorTypes(String catId,String catName){
        this.catId=catId;
        this.catName=catName;
    }

    public String getCatId() {
        return catId;
    }

    public String getCatName() {
        return catName;
    }
}
