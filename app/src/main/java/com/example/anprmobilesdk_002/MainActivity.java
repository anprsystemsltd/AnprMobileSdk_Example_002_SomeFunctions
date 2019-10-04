package com.example.anprmobilesdk_002;

import java.util.List;

import com.anprsystemsltd.sdk.mobile.ANPR;
import com.anprsystemsltd.sdk.mobile.CameraInput;
import com.anprsystemsltd.sdk.mobile.Event;
import com.anprsystemsltd.sdk.mobile.Result;
import com.anprsystemsltd.sdk.mobile.Tools;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class MainActivity extends Activity {


	private Context context = this;
	private ANPR sdkAnpr;
	private CameraInput cameraInput;
	private boolean inited = false;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		sdkAnpr = new ANPR(context, sdkEventListener);

		ANPR.Parameters paramsSdk = new ANPR.Parameters();
		paramsSdk.licenseMode = ANPR.Parameters.LICENSE_MODE_ONLINE;
//        parameters.licenseMode = ANPR.Parameters.LICENSE_MODE_OFFLINE;	// SDK is licenced for dedicated device

		paramsSdk.requestNationality = "HUN"; // at first run SDK will download the Hungarian ANPR native library into device file system (in background)
//        parameters.requestLoadFromSd = "/storage/sdcard0/PRB_Probe.so";	// at first run SDK will load the ANPR native library from SD card

		sdkAnpr.init(paramsSdk);	// init SDK
									// after end of initialization process eventListener will handle an event
	}

	private ANPR.EventListener sdkEventListener = new ANPR.EventListener() {

		@Override
		public void onEvent(Event event) {
			switch (event.type) {
				case ANPR.EventListener.EVENT_TYPE_LIBRARY:	// SDK opened the native ANPR library
					Log.i("Event Library", (String)event.object);	// Log the opened library name
					break;

				case ANPR.EventListener.EVENT_TYPE_LICENCE:		// SDK checked the licence
					Log.i("Event Licence", String.valueOf(event.success));	// Log licencing successful or not
					// if licencing not successful everything work but the first two character of recognized plate are replaced to "XX"
					break;

				case ANPR.EventListener.EVENT_TYPE_INIT:	 // SDK initialized
					Log.i("Event Init", String.valueOf(event.success));	// Log initializing successful or not
					if (event.success) {	// if initialization is successfull
						inited = true;
						setTitle("ANPR_SDK ver:" + sdkAnpr.getVersion() + " - " + sdkAnpr.getUsedLibraryName() + " ID:" + sdkAnpr.getDeviceId());
						startCamera();	// start camera
					} else {	// if initializing not successfull
						Handler handler = new Handler()
						{
							public void handleMessage(Message mes)
							{
								finish();	// exit
							}
						};
						Tools.ShowMessageDialog(context, event.result.errorMessage, event.result.data, handler);	// show error
					}
					break;
			}
		}
	};

	private void startCamera() {
		if (cameraInput != null) {
			return;
		}

		cameraInput = new CameraInput(context, cameraListener, CameraInput.MODE_WITH_PREVIEW);	// create new camera object with display camera live preview
		// create new camera object with display camera live preview
//        cameraInput = new CameraInput(context, CameraInput.MODE_WITHOUT_PREVIEW);	// create new camera object without line preview

		CameraInput.Preview preview = cameraInput.getCameraPreview();	// get the camera live preview object
		this.setContentView(preview);	// set preview to activity content
//        cameraInput.assignPreview(VIEWGROUP);	// assign preview to any layout in your activity

		CameraInput.Parameters cameraInputParameters = CameraInput.Parameters.CreateDefault(context);	// create camera initial parameters with default values
		if (cameraInputParameters.result.code == Result.OK)
		{

//        cameraInputParameters.cameraId = CameraInput.CAMERA_LOCATION_BACK;	// select camera (default backside)

//        cameraInputParameters.orientation = CameraInput.CAMERA_ORIENTATION_LANDSCAPE;	// set camera orientation (default detect activity orientation)

//        Camera.Parameters params = CameraInput.GetCameraParameters(cameraInputParameters.cameraId);	// get android camera parameters for selected camera
//        List<Size> resolutions = params.getSupportedPreviewSizes();	// get the supported resolutions
//        cameraInputParameters.resolution = resolutions.get(2);	// set the required camera resolution (default 640x480) (max 1024x800)

//        cameraInputParameters.infoFormat = new CameraInput.Parameters.WriteInfoFormat();		// set this to writing infos into pictures
//		  cameraInputParameters.infoFormat.format = CameraInput.Parameters.WriteInfoFormat.ELEMENT_PLATE + " Time:" + CameraInput.Parameters.WriteInfoFormat.ELEMENT_TIME + " " + CameraInput.Parameters.WriteInfoFormat.ELEMENT_LOCATION;	// info string format
//        cameraInputParameters.infoFormat.timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");	// info time format

//        cameraInputParameters.locationParameters = new LocationGPS.Parameters(LocationGPS.Parameters.MODE_BOTH);	// set this to using GPS (requires extra permissions in manifest)
//        cameraInputParameters.locationParameters.stringFormat.setModeNames(new String[]{"None", "NET", "SAT"});
//        cameraInputParameters.locationParameters.stringFormat.round = 4;	// round GPS coordinate to decimal

//        cameraInputParameters.anprSafeBufferSize = 5;	// set recognition safe
//        cameraInputParameters.anprSafeMinOccurded = 3;	// to found plate must 3 equal recognition from 5
			// default r equal from 3


			Result result = cameraInput.init(cameraInputParameters);	// init camera
			if (result.code == Result.OK)								// if initializing successed
			{
				cameraInput.startVideo();			// start camera preview
				cameraInput.startRecognizing();		// start recognition

				int supportedZoomMode = cameraInput.getSupportedZoomMode();	// supported zoom modes (NONE, DIRECT, SMOOTH)
				if (supportedZoomMode >= CameraInput.ZOOM_MODE_DIRECT)	// if supported zoom
				{
					cameraInput.showZoomDisplay();	// show current zoom value on preview
//                	cameraInput.setZoomDisplaySize(50);	// set zoom display size to 50px (default prview height * 0.05)
//                	cameraInput.setZoomDisplayColor(Color.YELLOW, Color.BLUE);	// set zoom display color to yellow, blue while selecting (default green, red)

					cameraInput.setZoomControlEnable(true);	// user can change zoom by swipe on prewiew

					List<Integer> zooms = cameraInput.getSupportedZoomRatios();	// supported zoom ratios (100 mean 1.00, etc)
					cameraInput.setZoomAsIndex(3, supportedZoomMode);	// set zoom to 3. zooms value
//                	cameraInput.setZoomAsValue(190, CameraInput.ZOOM_MODE_DIRECT);	// set zoom to 1.9 on direct mode (the result will be the nearest supported value)
				}
			}
			else
			{
				exitWithError(result.errorMessage, result.data);
			}
		}
		else
		{
			exitWithError(cameraInputParameters.result.errorMessage, cameraInputParameters.result.data);
		}

	}

	private CameraInput.EventListener cameraListener = new CameraInput.EventListener() {

		@Override
		public void onEvent(Event event) {
			switch (event.type) {
				case CameraInput.EventListener.EVENT_TYPE_CAMERA_PARAMETERS:		// camera initialized with this android camera parameters
					Camera.Parameters parameters = (Camera.Parameters)event.object;
					Size size = parameters.getPreviewSize();	// Log camera resolution
					Log.i("Camera parameter", size.width + "X" + size.height);

//					List<String> supportedScenes = parameters.getSupportedSceneModes();	// change cmaera scene mode for example
//					parameters.setSceneMode(supportedScenes.get(0));
					// you can change any parameters what does not require camera preview restart
					break;

				case CameraInput.EventListener.EVENT_TYPE_CAMERA_FRAME:	// event every camera preview frame
					CameraInput.Frame frame = (CameraInput.Frame)event.object;
					Log.i("Detect", String.valueOf(frame.anprResult.plateFound));
					break;

				case CameraInput.EventListener.EVENT_TYPE_CAMERA_ZOOM:		// when zoom changed
					int value = (Integer)event.object;	// current zoom value
					boolean finished = (Boolean)event.object2;	// zooming process finished
					Log.i("Camera zoom", value + " - " + finished);	// Log them
					break;

				case CameraInput.EventListener.EVENT_TYPE_CAMERA_PLATE_FOUND:	// when found licence plate
					CameraInput.Found found = (CameraInput.Found)event.object;	// found object
					final String plate = found.plate;
					Log.i("Plate found", found.plate);	// Log plate
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							setTitle(plate);	// set to title
						}
					});
					break;
			}
		}
	};

	private void stopCamera() {
		if (cameraInput != null) {
			cameraInput.close();
			cameraInput = null;
		}
	}


	@Override
	public void onPause() {
		super.onPause();
		stopCamera();
	}

	@Override
	public void onResume() {
		super.onResume();
		if (inited) {
			startCamera();
		}
	}

	private void exitWithError(String aTitle, String aMessage)
	{
		Handler handler = new Handler()
		{
			public void handleMessage(Message mes)
			{
				finish();	// exit
			}
		};
		Tools.ShowMessageDialog(context, aTitle, aMessage, handler);	// show error
	}

	protected void onDestroy()
	{
		super.onDestroy();
		stopCamera();
		if (sdkAnpr != null) {
			sdkAnpr.close();
			sdkAnpr = null;
		}

	}

}
