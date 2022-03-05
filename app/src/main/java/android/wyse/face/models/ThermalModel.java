package android.wyse.face.models;

import android.wyse.face.CaraManager;

import java.math.RoundingMode;
import java.text.DecimalFormat;

public class ThermalModel {

    private double heat;
    private double refHeat;
    private double difference;
    private double fahrenheitHeat;

    public ThermalModel(double heat, double refHeat, double difference){
        this.heat=heat;
        this.refHeat=refHeat;
        this.difference=difference;
    }


    public double getFahrenheitHeat() {

        fahrenheitHeat = (float) (heat*1.8) + 32;
        if (fahrenheitHeat>0) {
        DecimalFormat df=new DecimalFormat("#.#");
        df.setRoundingMode(RoundingMode.UP);
          return Float.valueOf(df.format(fahrenheitHeat));
        }

        return fahrenheitHeat;
    }

    public double getHeat() {
        //temperature in degree celsius
        /*if (heat>0){
            DecimalFormat df=new DecimalFormat(precision);
            //df.setRoundingMode(RoundingMode.UP);
            return Float.valueOf(df.format(heat));
        }*/
        return heat;
    }

    public double getRefHeat() {
        DecimalFormat df=new DecimalFormat("#.##");
        return Float.valueOf(df.format(refHeat));
        //return refHeat;
    }

}
