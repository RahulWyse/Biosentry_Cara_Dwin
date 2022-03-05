package android.wyse.face.tech5.initsdk;


import android.wyse.face.tech5.utilities.Fileresponse;

public interface InitSDKView {

    void showProgress();

    void hideProgress();

    void onSDKInitializeError(Throwable e);

    void onSDKinitialized(boolean isInitialized);

    void onFileReaded(Fileresponse fileresponse);

    void errorReadingFile(Throwable e);

    void onTemplateReaded(byte[] bytes);

    void onImageRotated(Fileresponse fileresponse);
}
