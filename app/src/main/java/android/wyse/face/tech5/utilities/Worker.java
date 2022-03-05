package android.wyse.face.tech5.utilities;

import android.os.Handler;
import android.util.Log;
import android.wyse.face.CaraManager;
import android.wyse.face.FaceManager;
import android.wyse.face.models.ThermalModel;
import android.wyse.face.tech5.authenticate.AuthResponse;

import java.util.ArrayList;
import java.util.List;

import ai.tech5.sdk.abis.face.t5face.IdentifyFaceResult;


public class Worker extends Thread {
    public List<OneShotProcessorTask> queue = new ArrayList<OneShotProcessorTask>();

    private final int MAX_QUEUE_SIZE = 10;
    private final int MAX_THREAD_COUNT = 1;

    private static Worker worker = null;
    private Handler handler;

    // HashMap<String, Float> identifyResultsmap = null;
    IdentifyFaceResults identifyResults = null;
    AuthResponse authResponse = null;
    String errorMessage = null;
    EnrollResponse enrollResponse = null;
    ResultObject matchResult;
    boolean isSDKINitialied = false;
    boolean isGoodPicture=false;
    private ThermalModel thermalModel;

    private Worker() {

    }

    public static Worker getInstance() {
        if (worker == null) {
            worker = new Worker();
            worker.start();
        }

        return worker;
    }

    public void sethandler(Handler handler) {
        this.handler = handler;
    }


    @Override
    public void start() {

        Runnable task = () -> {

            while (true) {
                OneShotProcessorTask oneShotProcessorTask = pullTask();
                errorMessage = null;

                final String type = oneShotProcessorTask.getTaskType();
                if (type.equalsIgnoreCase(Constants.TYPE_IDENTIFY)) {
                    identifyResults = null;
                    try {
                        identifyResults = oneShotProcessorTask.identifyFace();
                    } catch (Exception e) {
                        errorMessage = e.getLocalizedMessage();
                    }
                } else if (type.equalsIgnoreCase(Constants.TYPE_AUTHENTICATE)) {
                    authResponse = null;
                    authResponse = oneShotProcessorTask.authenticateFace();
                } else if (type.equalsIgnoreCase(Constants.TYPE_ENROLL)) {
                    enrollResponse = null;
                    enrollResponse = oneShotProcessorTask.enrollFace();
                } else if (type.equalsIgnoreCase(Constants.TYPE_MATCH)) {
                    matchResult = null;
                    try {
                        matchResult = oneShotProcessorTask.matchFaceImages();
                    } catch (Exception e) {
                        errorMessage = e.getLocalizedMessage();
                    }
                } else if (type.equalsIgnoreCase(Constants.TYPE_INIT_SDK)) {
                    isSDKINitialied = false;
                    try {
                        isSDKINitialied = oneShotProcessorTask.initSDK();
                    } catch (Exception e) {
                        isSDKINitialied = false;
                    }
                } else if (type.equals(Constants.TYPE_QUALITY_CHECK)){
                     isGoodPicture=false;
                    if (isSDKINitialied && !FaceManager.getInstance().isFaces()){
                        isGoodPicture=oneShotProcessorTask.checkQuality();
                    }
                } else if (type.equals(Constants.TYPE_THERMAL_SCAN)){
                    if (CaraManager.getInstance().isThermal()){

                        thermalModel = oneShotProcessorTask.scanTemperature();
                    }
                } else if (type.equals(Constants.TYPE_LIVENESS)){
                        handler.sendMessage(handler.obtainMessage(Constants.LIVENESS_SUCCESS,
                                FaceManager.getInstance().faceSpoofDetector("-1")));

                } else if (type.equals(Constants.TYPE_FACE_DETECT)){
                    //opencv based face detection for android 7.1+
                    handler.sendMessage(handler.obtainMessage(Constants.FACE_DETECT_SUCCESS,
                            FaceManager.getInstance().detectLiveFaces()));
                }

                Runnable myRunnable = () -> {
                    if (type.equalsIgnoreCase(Constants.TYPE_IDENTIFY)) {
                        if (errorMessage == null && identifyResults!=null) {
                            if (identifyResults.identifyFaceResultl != null) {
                                //LogUtils.debug("TAG", "total results " + identifyResults.identifyFaceResultl.length);
                            }
                            handler.sendMessage(handler.obtainMessage(Constants.IDENTIFY_SUCCESS, identifyResults));
                        } else {
                            handler.sendMessage(handler.obtainMessage(Constants.IDENTIFY_FAILURE, errorMessage));
                        }
                    } else if (type.equalsIgnoreCase(Constants.TYPE_AUTHENTICATE)) {

                        handler.sendMessage(handler.obtainMessage(Constants.AUTH_SUCCESS, authResponse));

                    } else if (type.equalsIgnoreCase(Constants.TYPE_ENROLL)) {

                        handler.sendMessage(handler.obtainMessage(Constants.ENROLL_SUCCESS, enrollResponse));

                    } else if (type.equalsIgnoreCase(Constants.TYPE_MATCH)) {

                        if (errorMessage == null&&matchResult!=null) {

                            handler.sendMessage(handler.obtainMessage(Constants.MATCH_SUCCESS, matchResult));
                        }else {
                            handler.sendMessage(handler.obtainMessage(Constants.MATCH_FAILURE, errorMessage));
                        }

                    } else if (type.equalsIgnoreCase(Constants.TYPE_INIT_SDK)) {
                        handler.sendMessage(handler.obtainMessage(Constants.INIT_SDK_SUCCESS, isSDKINitialied));
                    } else if (type.equals(Constants.TYPE_QUALITY_CHECK)){
                        handler.sendMessage(handler.obtainMessage(Constants.QUALITY_CHECK_SUCCESS, isGoodPicture));
                    } else if (type.equals(Constants.TYPE_THERMAL_SCAN)){
                       // Log.d("thermal","scan result has been sent");
                        handler.sendMessage(handler.obtainMessage(Constants.THERMAL_SCAN_SUCCESS, thermalModel));
                    }
                };
                handler.post(myRunnable);
            }
        };

        // Create a Group of Threads for processing
        for (int i = 0; i < MAX_THREAD_COUNT; i++) {
            new Thread(task).start();
        }

    }

    // Pulls a message from the queue
    // Only returns when a new message is retrieves
    // from the queue.
    private synchronized OneShotProcessorTask pullTask() {
        while (queue.isEmpty()) {
            try {
                wait();
            } catch (InterruptedException e) { }
        }
        return queue.remove(0);
    }

    // Push a new message to the tail of the queue if
    // the queue has available positions
    public synchronized void addTask(OneShotProcessorTask task) {
        if (queue.size() < MAX_QUEUE_SIZE) {
            queue.add(task);
            notifyAll();
        }else {
            if (!CaraManager.getInstance().isReadyForCapture()){
                queue.clear();
                notifyAll();
            }
            Log.d("thermal","queue max size reached");
        }
    }

}