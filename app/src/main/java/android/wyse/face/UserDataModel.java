package android.wyse.face;

public class UserDataModel {

    private String userid;
    private String name;
    private String status;
    public UserDataModel(String userid,String name){
        this.userid=userid;
        this.name=name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    public String getUserid() {
        return userid;
    }
}
