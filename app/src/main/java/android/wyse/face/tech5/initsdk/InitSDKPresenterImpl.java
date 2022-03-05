package android.wyse.face.tech5.initsdk;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.wyse.face.tech5.utilities.AppSharedPreference;
import android.wyse.face.tech5.utilities.Fileresponse;
import android.wyse.face.tech5.utilities.LogUtils;
import android.wyse.face.tech5.utilities.Utilities;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class InitSDKPresenterImpl implements InitSDKPresnter {


    InitSDKView enrollView;
    Context context;
    private Utilities util;
    private CompositeDisposable compositeDisposable;
    private AppSharedPreference appSharedPreference = null;

    @Override
    public void setView(Context context, InitSDKView view) {
        enrollView = view;
        this.context = context;
        util = new Utilities(context, "");
        appSharedPreference = new AppSharedPreference(context);
        if (compositeDisposable == null || !compositeDisposable.isDisposed()) {
            compositeDisposable = new CompositeDisposable();
        }
    }

//    @Override
//    public void initSDK() {
//        enrollView.showProgress();
//
//        initSDKObservable().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<Boolean>() {
//            @Override
//            public void onSubscribe(Disposable d) {
//                compositeDisposable.add(d);
//            }
//
//            @Override
//            public void onError(Throwable e) {
//                enrollView.hideProgress();
//                enrollView.onSDKInitializeError(e);
//            }
//
//            @Override
//            public void onComplete() {
//
//            }
//
//            @Override
//            public void onNext(Boolean enrollResponse) {
//                enrollView.hideProgress();
//                enrollView.onSDKinitialized(enrollResponse);
//            }
//        });
//    }


//    private Observable<Boolean> initSDKObservable() {
//
//        return Observable.create((ObservableEmitter<Boolean> emitter) -> {
//            try {
//
//
//                long t1 = System.currentTimeMillis();
//                if (!appSharedPreference.isResourceDeleted()) {
//                    appSharedPreference.setResourceDeleted(true);
//                    boolean isSDKDirDeleted = util.deleteSDKDir("share");
//
//                    long t2 = System.currentTimeMillis();
//
//                }
//                boolean flag = util.loadBinFiles(context,context.getResources().getString(R.string.bin_files_path), context.getResources().getString(R.string.zip_file_name));
//                long t2 = System.currentTimeMillis();
//                System.err.println("Time Taken for BIN LOADERS:::" + (t2 - t1));
//
//
//                //  copyFaceSdkFilesToInternalStorage();
//
//                int version = Integer.parseInt(context.getResources().getString(R.string.bin_version));
//
//
//                System.loadLibrary("c++_shared");
//                System.loadLibrary("face_sdk");
//                // System.loadLibrary("mxnet");
//                System.loadLibrary("T5FaceNativeJNI");
//                System.loadLibrary("passportizer_jni");
//                setenv("FACE_SDK_BIN_ROOT", context.getResources().getString(R.string.bin_files_path), true);
//               // Listener.initSDK(version, context.getResources().getString(R.string.matcher_table_code));
//
//
////"/mnt/sdcard/share/face_sdk"
//              //  Utilities.enrollFromCache(context);
//
//                //enrollView.onSDKinitialized(true);
//                emitter.onNext(Listener.isSDKInitialized);
//                emitter.onComplete();
//            } catch (Exception e) {
//                e.printStackTrace();
//                LogUtils.debug(null, e.getLocalizedMessage());
//                // enrollView.onSDKinitialized(true);
//                emitter.onError(e);
//            }
//
//        });
//    }

    @Override
    public void getImageByteArrayFromUri(Uri uri, int camerafacing) {

        enrollView.showProgress();

        LogUtils.debug("InitSDKPresenterImpl", "getImageByteArrayFromUri()");


        filerederObservable(uri, camerafacing).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<Fileresponse>() {
            @Override
            public void onSubscribe(Disposable d) {
                compositeDisposable.add(d);
            }

            @Override
            public void onNext(Fileresponse fileresponse) {
                enrollView.hideProgress();
                enrollView.onFileReaded(fileresponse);
            }

            @Override
            public void onError(Throwable e) {
                enrollView.hideProgress();
                enrollView.errorReadingFile(e);

            }

            @Override
            public void onComplete() {

            }
        });

    }

    private Observable<Fileresponse> filerederObservable(Uri uri, int cameraFacing) {

        return Observable.create((ObservableEmitter<Fileresponse> emitter) -> {
            Fileresponse fileresponse = null;
            try {

                byte[] capturedFaceBytes = Utilities.getByteArrayFromUri(context, uri);


                if (null != capturedFaceBytes) {

                    int rotation = Utilities.getExifRotation(capturedFaceBytes);
                    // rotation=0;

                    LogUtils.debug("TAG", "rotation " + rotation);
                    if (rotation == 0) {
                        // Bitmap capturedFaceBitmap = BitmapFactory.decodeByteArray(capturedFaceBytes, 0, capturedFaceBytes.length);

                        fileresponse = new Fileresponse();
                        fileresponse.fileBytes = capturedFaceBytes;
                        // fileresponse.bitmap = capturedFaceBitmap;
                    } else {

                        Bitmap rotatedImage2 = null;
                        if (cameraFacing == 0) {
                            rotatedImage2 = Utilities.rotateImage(360 - rotation, capturedFaceBytes);
                        } else {
                            rotatedImage2 = Utilities.rotateImage(rotation, capturedFaceBytes);
                        }

                        capturedFaceBytes = Utilities.getBytesFromImage(rotatedImage2);
                        fileresponse = new Fileresponse();
                        fileresponse.fileBytes = capturedFaceBytes;
                        // fileresponse.bitmap = rotatedImage2;
                    }
                    System.err.println("capturedFaceBytes :: " + capturedFaceBytes.length);
                } else {
                    System.err.println("Failed to capture image..Please try again :: ");
                }
                emitter.onNext(fileresponse);
                emitter.onComplete();
            } catch (Exception e) {
                emitter.onError(e);
            }


        });
    }

    @Override
    public void readFromUri(Uri uri) {

        enrollView.showProgress();
        Observable.create((ObservableEmitter<byte[]> emitter) -> {
            try {
                byte[] capturedFaceBytes = Utilities.getByteArrayFromUri(context, uri);
                emitter.onNext(capturedFaceBytes);
                emitter.onComplete();
            } catch (Exception e) {
                emitter.onError(e);
            }

        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<byte[]>() {
            @Override
            public void onSubscribe(Disposable d) {
                compositeDisposable.add(d);

            }

            @Override
            public void onNext(byte[] bytes) {
                enrollView.hideProgress();
                enrollView.onTemplateReaded(bytes);
            }

            @Override
            public void onError(Throwable e) {
                enrollView.hideProgress();
                enrollView.errorReadingFile(e);
            }

            @Override
            public void onComplete() {

            }
        });
    }


    @Override
    public void rotateImage(float degrees, byte[] faceBytes) {

        enrollView.showProgress();
        Observable.create((ObservableEmitter<Fileresponse> emitter) -> {
            try {
                Bitmap rotatedImage = Utilities.rotateImage(degrees, faceBytes);

                byte[] capturedFaceBytes = Utilities.getBytesFromImage(rotatedImage);
                Fileresponse fileresponse = new Fileresponse();
                fileresponse.fileBytes = capturedFaceBytes;
                // fileresponse.bitmap = rotatedImage;
                emitter.onNext(fileresponse);
                emitter.onComplete();
            } catch (Exception e) {
                emitter.onError(e);
            }

        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<Fileresponse>() {
            @Override
            public void onSubscribe(Disposable d) {
                compositeDisposable.add(d);

            }

            @Override
            public void onNext(Fileresponse fileresponse) {
                enrollView.hideProgress();
                enrollView.onImageRotated(fileresponse);
            }

            @Override
            public void onError(Throwable e) {
                enrollView.hideProgress();
                // enrollView.errorReadingFile(e);
            }

            @Override
            public void onComplete() {

            }
        });
    }

    @Override
    public void destroy() {
        if (compositeDisposable != null && !compositeDisposable.isDisposed()) {
            compositeDisposable.dispose();
        }
        context = null;
        enrollView = null;
    }


    private void copyFaceSdkFilesToInternalStorage() {

        LogUtils.debug("TAG", " copyFaceSdkFilesToInternalStorage ");

        String internalStoragePath = Environment.getExternalStorageDirectory().getAbsolutePath();

        String faceSDKPath = internalStoragePath + File.separator + "share" + File.separator + "face_sdk";


        File alignmentFile = new File(faceSDKPath + File.separator + "alignment" + File.separator + "103.bin");
        File builderFile = new File(faceSDKPath + File.separator + "builder" + File.separator + "200.bin");
        File faceDetectorFile = new File(faceSDKPath + File.separator + "face_detector" + File.separator + "200.bin");
        File matcherFile = new File(faceSDKPath + File.separator + "matcher" + File.separator + "200" + File.separator + "gn.bin");
        File licenseFile = new File(faceSDKPath + File.separator + "face_sdk.lic");

        if (!alignmentFile.exists()) {
            copyFileFromAssets(context, "face_sdk" + File.separator + "alignment" + File.separator + "103.bin", alignmentFile.getAbsolutePath());
        }

        if (!builderFile.exists()) {
            copyFileFromAssets(context, "face_sdk" + File.separator + "builder" + File.separator + "200.bin", builderFile.getAbsolutePath());
        }

        if (!faceDetectorFile.exists()) {
            copyFileFromAssets(context, "face_sdk" + File.separator + "face_detector" + File.separator + "200.bin", faceDetectorFile.getAbsolutePath());
        }

        if (!matcherFile.exists()) {
            copyFileFromAssets(context, "face_sdk" + File.separator + "matcher" + File.separator + "200" + File.separator + "gn.bin", matcherFile.getAbsolutePath());
        }

        if (!licenseFile.exists()) {
            copyFileFromAssets(context, "face_sdk" + File.separator + "face_sdk.lic", licenseFile.getAbsolutePath());
        }


    }


    private void copyFileFromAssets(Context context, String filename, String path) {
        AssetManager assetManager = context.getAssets();

        InputStream in = null;
        OutputStream out = null;
        try {
            in = assetManager.open(filename);
            File destFile = new File(path);
            File directory = new File(destFile.getParentFile().getAbsolutePath());
            if (!directory.exists()) {
                directory.mkdirs();
            }

            out = new FileOutputStream(path);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }

        } catch (Exception e) {
            LogUtils.debug("tag", e.getMessage());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {

                }
            }

            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {

                }
            }
        }

    }
}
