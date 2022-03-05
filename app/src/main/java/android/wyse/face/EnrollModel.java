package android.wyse.face;

import android.graphics.Bitmap;

public class EnrollModel {

    private String enrolId;
    private String name;
    private String img;
    private static EnrollModel enrollModel;

    private EnrollModel(String id,String name,String img){
        enrolId=id;
        this.name=name;
        this.img=img;
    }

    public String getEnrolId() {
        return enrolId;
    }

    public String getName() {
        return name;
    }

    public String getBaseImg() {
        return img;
    }

    public void setEnrolId(String enrolId) {
        this.enrolId = enrolId;
    }


    public void setImg(String img) {
        this.img = img;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void clearAll(){
        this.name="";
        this.enrolId="";
        this.img="";
    }

    public static EnrollModel getInstance(){
        if (enrollModel==null){
            return enrollModel=new EnrollModel("","",null);
        }
        return enrollModel;
    }
}
