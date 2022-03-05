package android.wyse.face.tech5.db;


import androidx.room.Dao;
import androidx.room.Ignore;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.RoomWarnings;

import java.util.List;

import io.reactivex.Maybe;


@Dao
public interface FaceRecordsDAO {

    @Insert
    void insertFaceRecordToCache(FaceRecord faceRecord);


    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT id,template FROM templates")
    Maybe<List<FaceRecord>> getAllRecordsInCache();

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT id,template FROM templates")
    List<FaceRecord> getAllRecords();

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT id,faceImage FROM templates where id=:id")
    Maybe<FaceRecord> getfaceImageById(String id);


    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT * FROM templates where id=:id")
    FaceRecord getFaceRecordById(String id);


    @Query("DELETE FROM templates")
    void delete();

    @Query("DELETE FROM templates WHERE id=:id")
    void deleteUserById(String id);


}
