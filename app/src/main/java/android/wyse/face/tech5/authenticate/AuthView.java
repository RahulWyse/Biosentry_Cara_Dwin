package android.wyse.face.tech5.authenticate;


import android.wyse.face.tech5.initsdk.InitSDKView;

public interface AuthView extends InitSDKView {
    void onAuthenticated(AuthResponse response);
    void onError(Throwable e);
}
