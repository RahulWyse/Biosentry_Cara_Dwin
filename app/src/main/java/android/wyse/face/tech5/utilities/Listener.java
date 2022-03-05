package android.wyse.face.tech5.utilities;

import android.wyse.face.Helper;
import android.wyse.face.Utility;

import ai.tech5.sdk.abis.face.t5face.FaceSDKFactory;
import ai.tech5.sdk.abis.face.t5face.IFaceSDKTemplateExtractor;
import ai.tech5.sdk.abis.face.t5face.IFaceSDKTemplateMatcher;
import ai.tech5.sdk.abis.face.t5face.TemplateExtractorConfig;
import ai.tech5.sdk.abis.face.t5face.TemplateMatcherConfig;

public class Listener {

    public static IFaceSDKTemplateExtractor t5TemplateCreator;
    public static IFaceSDKTemplateMatcher t5TemplateMatcher;
    // public static IOneShotProcessor mOneShotProcessor;
    public static boolean isSDKInitialized = false;

    public static boolean initSDK(String licPath,int detectorVer,int builderVersion,
                                  String matcherTableCode, float detectorConfidence) {
        try {
            if (!isSDKInitialized) {
                long t1 = System.currentTimeMillis();
                LogUtils.debug("Listener", "Before init() !!!");
                TemplateExtractorConfig tcConfig = new TemplateExtractorConfig();

                tcConfig.FaceDetectorVersion = detectorVer;
                // tcConfig.AlignmentVersion = 103;
                tcConfig.BuilderVersion = builderVersion;
                tcConfig.AgeGenderVersion = -1;
                tcConfig.AlignmentVersion = -1;
                //tcConfig.FaceDetectorVersion = 102;
                //tcConfig.AlignmentVersion = 103;
                //tcConfig.BuilderVersion = 102;
                tcConfig.FaceDetectorConfidence = detectorConfidence;
                tcConfig.RemoteLicensingCachePath = licPath;
                tcConfig.UseRemoteLicensing = true;
                tcConfig.RemoteLicensingToken = Helper.getTech5Token();

                tcConfig.UseMaskChecker = true;
                tcConfig.QualityVersion = 100;
                tcConfig.UseFaceColorChecker = false;
                tcConfig.UseBackgroundChecker = false;
                tcConfig.UseOverexposureChecker = true;
                tcConfig.UseBlurChecker = true;
                tcConfig.UseGlassesSmileOcclusionChecker = false;  //for jni below 0.12
                tcConfig.UseRotationChecker = false;
                tcConfig.UseHotSpotsChecker = false;
                tcConfig.UseRedEyesChecker = false;

                LogUtils.debug("Listener", "init step 1");
                t5TemplateCreator = FaceSDKFactory.CreateTemplateExtractor();
                LogUtils.debug("Listener", "init step 2");

                t5TemplateCreator.Init(tcConfig);


                LogUtils.debug("Listener", "face template creator loaded successfully !!!");
                long t2 = System.currentTimeMillis();
                LogUtils.debug("Listener", "@@@@@@@Time Taken to load Template Creator :: " + (t2 - t1));
                LogUtils.debug("TAG", "Initializing matcher with BuilderVersion " + builderVersion + " matcherTableCode " + matcherTableCode + " detector confidence  " + detectorConfidence);

                TemplateMatcherConfig mConfig = new TemplateMatcherConfig();
                mConfig.BuilderVersion = builderVersion;
                // mConfig.BuilderVersion = 102;

                mConfig.MatcherFirListHint = 5000;
                mConfig.MatcherTableCode = matcherTableCode;
                mConfig.RemoteLicensingCachePath = licPath;
                mConfig.UseRemoteLicensing = true;
                mConfig.RemoteLicensingToken = Helper.getTech5Token();


                LogUtils.debug("Listener", "init step 3");
                t5TemplateMatcher = FaceSDKFactory.CreateTemplateMatcher();
                t5TemplateMatcher.Init(mConfig);

                LogUtils.debug("Listener", "init step 4");

                long t3 = System.currentTimeMillis();
                LogUtils.debug("Listener", "@@@@@@@Time Taken to load Face Matcher :: " + (t3 - t2));
                LogUtils.debug("Listener", "face matcher loaded successfully !!!");
                isSDKInitialized = true;

            }
        } catch (Exception e) {
            /*LogUtils.debug("TAG", e.getLocalizedMessage());
            StackTraceElement[] stackTraceElements = e.getStackTrace();
            for (StackTraceElement elmnt : stackTraceElements) {
                LogUtils.debug("TAG", elmnt.toString());
            }*/
            Utility.printStack(e);
            isSDKInitialized = false;

        }
        return isSDKInitialized;
    }
}
