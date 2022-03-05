package android.wyse.face.tech5.db;

import android.content.Context;
import android.database.Cursor;
import android.wyse.face.Utility;

import java.util.List;
import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class LocalCacheManager {

    private AppDatabase db;
    private CompositeDisposable compositeDisposable;

    public LocalCacheManager(Context context) {
        db = AppDatabase.getInstance(context);
        if (compositeDisposable == null || !compositeDisposable.isDisposed()) {
            compositeDisposable = new CompositeDisposable();
        }
    }

    public void addFaceRecordToDb(String id, byte[] template, byte[] faceImage) {
        db.faceRecordsDAO().insertFaceRecordToCache(new FaceRecord(id, template, faceImage));
    }

    public void removeUserById(String id){
       db.faceRecordsDAO().deleteUserById(id);
    }

    public void addFaceRecordToCache(final DatabaseCallback databaseCallback, FaceRecord faceRecord) {
        Completable.fromAction(() -> db.faceRecordsDAO().insertFaceRecordToCache(faceRecord)).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io()).subscribe(new CompletableObserver() {
            @Override
            public void onSubscribe(Disposable d) {
                compositeDisposable.add(d);
            }

            @Override
            public void onComplete() {
                databaseCallback.onFaceRecordAddedToCache(true);
            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
                databaseCallback.onFaceRecordAddedToCache(false);
            }

        });
    }

    public void getAllFaceRecordsInCache(final DatabaseCallback databaseCallback) {
        Disposable ds = db.faceRecordsDAO().getAllRecordsInCache().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer<List<FaceRecord>>() {
            @Override
            public void accept(@io.reactivex.annotations.NonNull List<FaceRecord> records) {
                databaseCallback.onfaceRecordsLoaded(records);
            }

        });
        compositeDisposable.add(ds);
    }


    public void getFaceImage(final DatabaseCallback databaseCallback, String id) {


        Disposable ds = db.faceRecordsDAO().getfaceImageById(id).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe((FaceRecord record) -> databaseCallback.onFaceImageFetched(record));

        compositeDisposable.add(ds);
    }


    public List<FaceRecord> getAllRecords() {
        return db.faceRecordsDAO().getAllRecords();
    }

    public FaceRecord getFaceRecordById(String id) {
        return db.faceRecordsDAO().getFaceRecordById(id);
    }

    public void deleteAllFaceRecords() {
        db.faceRecordsDAO().delete();
    }


    public void closeDbConnection() {
        try {
//            if (db.isOpen()) {
//                db.close();
//            }
        } catch (Exception e) {
            Utility.printStack(e);
        }

        try {

            if (compositeDisposable != null && !compositeDisposable.isDisposed()) {
                compositeDisposable.dispose();
            }

        } catch (Exception e) {
            Utility.printStack(e);
        }
    }

}