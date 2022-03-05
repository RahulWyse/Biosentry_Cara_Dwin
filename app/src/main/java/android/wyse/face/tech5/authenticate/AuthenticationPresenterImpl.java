package android.wyse.face.tech5.authenticate;

import android.content.Context;
import android.wyse.face.tech5.db.FaceRecord;
import android.wyse.face.tech5.db.LocalCacheManager;
import android.wyse.face.tech5.utilities.LogUtils;

import java.util.ArrayList;


import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class AuthenticationPresenterImpl implements AuthenticationPresenter {

    AuthView enrollView;
    Context context;
    private CompositeDisposable compositeDisposable;

    LocalCacheManager cacheManager;

    @Override
    public void setView(Context context, AuthView view) {
        enrollView = view;
        this.context = context;

        if (compositeDisposable == null || !compositeDisposable.isDisposed()) {
            compositeDisposable = new CompositeDisposable();
        }
    }

    @Override
    public void destroy() {
        if (compositeDisposable != null && !compositeDisposable.isDisposed()) {
            compositeDisposable.dispose();
        }
        context = null;
        enrollView = null;
    }


    private Observable<AuthResponse> authenticateFaceObservable(String id, byte[] faceImage, String fileName) {

        return Observable.create((ObservableEmitter<AuthResponse> emitter) -> {
            try {

                AuthResponse response = authenticateFace(id, faceImage, fileName);


                if (response != null) {
                    emitter.onNext(response);
                    emitter.onComplete();
                } else {
                    LogUtils.debug("TAG", "response null");

                    emitter.onError(new Throwable("error in enroll"));
                }
            } catch (Exception e) {
                e.printStackTrace();
                emitter.onError(e);
            }

        });
    }

    private AuthResponse authenticateFace(String id, byte[] faceImage, String fileName) {

        AuthResponse response = new AuthResponse();

        try {
            cacheManager = new LocalCacheManager(context);
            FaceRecord faceRecord = cacheManager.getFaceRecordById(id);

            if (faceRecord != null && faceRecord.template != null && faceRecord.template.length > 0) {


                ArrayList<byte[]> faces = new ArrayList<>();
                faces.add(faceImage);

               // OneShotResult faceTcResult = Listener.mOneShotProcessor.processImage(faceImage);


//                if (faceTcResult != null && faceTcResult.template != null && faceTcResult.template.length > 0) {
//
//                    Utilities.writeTemplateToFile(faceTcResult.template, fileName);
//
//
//                    float score = Listener.t5TemplateMatcher.MatchWithId(faceTcResult.template, id);
//
//
//                    response.score = score;
//                    response.faceImage = faceRecord.faceImage;
//                    response.errorMesssage = null;
//
//                } else {
//
//                    response.errorMesssage = "Unable to create face template";
//                }

            } else {
                response.errorMesssage = " Id  " + id + " not exists ";
            }


        } catch (Exception e) {
            e.printStackTrace();

            response.errorMesssage = e.getLocalizedMessage();

        }

        return response;

    }


    public void authenticate(String id, byte[] faceImage, String fileName) {
        enrollView.showProgress();

        authenticateFaceObservable(id, faceImage, fileName).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<AuthResponse>() {
            @Override
            public void onSubscribe(Disposable d) {
                compositeDisposable.add(d);
            }

            @Override
            public void onError(Throwable e) {
                enrollView.hideProgress();
                enrollView.onError(e);
            }

            @Override
            public void onComplete() {

            }

            @Override
            public void onNext(AuthResponse enrollResponse) {
                enrollView.hideProgress();
                enrollView.onAuthenticated(enrollResponse);
            }
        });

    }


}
