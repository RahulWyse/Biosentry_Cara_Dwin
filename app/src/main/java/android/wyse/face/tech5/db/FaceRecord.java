package android.wyse.face.tech5.db;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "templates")
public class FaceRecord implements Parcelable {

    @NonNull
    @PrimaryKey
    public String id;
    public byte[] template;

    public byte[] faceImage;


    public FaceRecord() {
    }


    @Ignore
    public FaceRecord(String id, byte[] template, byte[] faceImage) {
        this.id = id;
        this.template = template;
        this.faceImage = faceImage;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeByteArray(this.template);
        dest.writeByteArray(this.faceImage);
    }

    protected FaceRecord(Parcel in) {
        this.id = in.readString();
        this.template = in.createByteArray();
        this.faceImage = in.createByteArray();
    }

    public static final Creator<FaceRecord> CREATOR = new Creator<FaceRecord>() {
        @Override
        public FaceRecord createFromParcel(Parcel source) {
            return new FaceRecord(source);
        }

        @Override
        public FaceRecord[] newArray(int size) {
            return new FaceRecord[size];
        }
    };
}
