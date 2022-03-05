package android.wyse.face.tech5.db;


import android.content.Context;
import android.os.Environment;
import android.wyse.face.tech5.db.FaceRecord;
import android.wyse.face.tech5.db.FaceRecordsDAO;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;




@Database(entities = {FaceRecord.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {


    private static final String DB_NAME = "FaceCache.db";
    private static AppDatabase sInstance;

    public static AppDatabase getInstance(Context context) {

        if (sInstance == null) {
            sInstance = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, DB_NAME).build();
        }

        return sInstance;

    }


    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            //  database.execSQL("TRUNCATE TABLE templates");
        }
    };

    public abstract FaceRecordsDAO faceRecordsDAO();


}