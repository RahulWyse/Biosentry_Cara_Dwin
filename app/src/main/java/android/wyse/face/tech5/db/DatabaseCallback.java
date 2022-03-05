package android.wyse.face.tech5.db;
import java.util.List;

public interface DatabaseCallback {

    void onfaceRecordsLoaded(List<FaceRecord> records);

    void onFaceRecordAddedToCache(boolean isAdded);



    void onFaceImageFetched(FaceRecord record);
}
