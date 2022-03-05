package android.wyse.face;

import java.math.RoundingMode;
import java.text.DecimalFormat;

public class UserModel {

    private static UserModel userModel;
    private String userId;
    private byte[] faceImg;
    private String userName;
    private String matchScore;
    private String InOutStatus;
    private String heat="0";
    private String uploadStatus="0";
    private String degF ="°F";
    private String degree="°C";
    private String recordHeat;
    private UserModel(String userId,byte[] faceImg){}

    public static UserModel getInstance(){
        if (userModel==null){
            return userModel=new UserModel("",null);
        }
        return  userModel;
    }

    public void clearAll(){
        userName="";
        matchScore="";
        faceImg=null;
        userId="";
        InOutStatus="";
        heat="0.0";
    }

    public String getUnit(String type) {
        if (type.equals("F"))
        return degF;

        return degree;
    }

    public void setHeat(String heat) {
        this.heat = heat;
    }

    public void setRecordHeat(double recordHeat) {
        DecimalFormat df = new DecimalFormat("#.##");
        this.recordHeat = df.format(recordHeat);
    }

    public String getRecordHeat() {
        return recordHeat;
    }

    public float getHeat(String unit) {
            DecimalFormat df = new DecimalFormat("#.#");
            float newHeat = Float.parseFloat(heat);
            if (unit.equals("F")) {
                float fahrenheitHeat = (float) (newHeat * 1.8) + 32;
                if (fahrenheitHeat > 0) {
                    df.setRoundingMode(RoundingMode.UP);
                    return Float.parseFloat(df.format(fahrenheitHeat));
                }
            }

            return Float.parseFloat(df.format(newHeat));
    }

    public void setUploadStatus(String uploadStatus) {
        this.uploadStatus = uploadStatus;
    }

    public String getUploadStatus() {
        return uploadStatus;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserName() {
        return userName;
    }

    public void setMatchScore(String matchScore) {
        this.matchScore = matchScore;
    }

    public void setInOutStatus(String inOutStatus) {
        InOutStatus = inOutStatus;
    }

    public String getInOutStatus() {
        return InOutStatus;
    }

    public String getMatchScore() {
        return matchScore;
    }


    public void setFaceImg(byte[] faceImg) {
        this.faceImg = faceImg;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public byte[] getFaceImg() {
        return faceImg;
    }

    public String getUserId() {
        return userId;
    }
}
