package android.wyse.face.models;

public class UsersModel {

    private String name;
    private String userid;
    public UsersModel(String name,String userid){
        this.name=name;
        this.userid=userid;
    }

    public String getUserid() {
        return userid;
    }

    public String getName() {
        return name;
    }
}
