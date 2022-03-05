package android.wyse.face.tech5.initsdk;

import android.content.Context;
import android.net.Uri;

public interface InitSDKPresnter {

//    void initSDK();

    void setView(Context context, InitSDKView view);

    void getImageByteArrayFromUri(Uri uri, int cameraFacing);

    void readFromUri(Uri uri);

    void rotateImage(float degrees, byte[] faceBytes);

    void destroy();
}
