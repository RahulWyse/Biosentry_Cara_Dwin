package android.wyse.face.adaptors;

public class UserIdNameModel {

  String userId;
  String userName;

  public UserIdNameModel(String userName, String userId){
    this.userId=userId;
    this.userName=userName;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getUserId() {
    return userId;
  }

  public String getUserName() {
    return userName;
  }

}
