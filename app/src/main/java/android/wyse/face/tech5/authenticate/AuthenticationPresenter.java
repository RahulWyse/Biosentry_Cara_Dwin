package android.wyse.face.tech5.authenticate;

import android.content.Context;

public interface AuthenticationPresenter {


    void authenticate(String id, byte[] faceImage, String fileName);

    void setView(Context context, AuthView view);

    void destroy();

}
