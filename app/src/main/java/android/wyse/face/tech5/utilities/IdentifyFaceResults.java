package android.wyse.face.tech5.utilities;

/**
 * created by sonu at 4 July 2020
 */

import ai.tech5.sdk.abis.face.t5face.IdentifyFaceResult;

public class IdentifyFaceResults {

    public float mask;
    public float glasses;
    public float smile;
    public float gender;
    public IdentifyFaceResult[] identifyFaceResultl;

    public IdentifyFaceResults(IdentifyFaceResult[] identifyFaceResultl,float mask,float smile,float glasses,float gender) {
        this.identifyFaceResultl=identifyFaceResultl;
        this.mask=mask;
        this.glasses = glasses;
        this.smile = smile;
        this.gender = gender;
    }
}
