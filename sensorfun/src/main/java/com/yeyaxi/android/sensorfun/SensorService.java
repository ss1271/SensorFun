package com.yeyaxi.android.sensorfun;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.yeyaxi.android.sensorfun.util.SensorDataUtility;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * 
 * @author Yaxi Ye
 * @since Oct.11.2013
 */
public class SensorService extends Service implements SensorEventListener{

//	private String msg = "Test Msg";

	private SensorManager mSensorManager;
//	private List<Sensor> deviceSensors;
	private static final String TAG = SensorService.class.getSimpleName();
//	private LocalBroadcastManager mLocalBroadcastManager;
	
	private float[] accVals = new float[3];
	private float[] tempVal = new float[3];
	private float[] gravVals = new float[3];
	private float[] gyroVals = new float[3];
	private float[] lightVal = new float[3];
	private float[] lineAccVals = new float[3];
	private float[] magVals = new float[3];
	private float[] presVal = new float[3];
	private float[] proxVal = new float[3];
	private float[] rhVal = new float[3];
	// Rotation Vector contains 5 elements
	private float[] rotVals = new float[5];

    private float[] stepCountVals = new float[2];
    private float[] stepDetectVals = new float[2];
    private float[] gameRotVals = new float[5];
    private float[] geoMagRotVals = new float[5];
	// This is the main switch of writing data to storage
	private boolean isRecord = false;

	// These are the separate switches
	private boolean accToggle = false;
	private boolean gyroToggle = false;
	private boolean gravityToggle = false;
	private boolean linAccToggle = false;
	private boolean magToggle = false;
	private boolean rotVecToggle = false;
	private boolean tempToggle = false;
	private boolean lightToggle = false;
	private boolean pressureToggle = false;
	private boolean proxiToggle = false;
	private boolean relaHumToggle = false;
    private boolean stepDetectorToggle = false;
    private boolean stepCounterToggle = false;
    private boolean sigMotionToggle = false;
    private boolean gameRotToggle = false;
    private boolean geoMagRotToggle = false;
//    private boolean allToogle = false;
    // sensor to be monitored
    private int sensorType;

    private NotificationManager notificationManager;

    private int SENSOR_RECORDING_NOTIFICATION_ID = 1271000;

	// This is the object that receives interactions from clients.  See
	// RemoteService for a more complete example.
	private final IBinder mBinder = new SensorBinder();
	
	public class SensorBinder extends Binder {
		SensorService getService() {
			return SensorService.this;
		}
	}

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();

            if (action.equals(BaseActivity.ACTION_STOP)) {
                Log.d(TAG, "Sensor Service stopped via notification.");
                AlarmScheduler.cancelAlarm(SensorService.this);
                // cancel notification
                notificationManager.cancel(SENSOR_RECORDING_NOTIFICATION_ID);
                stopSelf();
            }

            if (action.equals(BaseActivity.ACTION_RECORD)) {
                Log.d(TAG, "Record command received.");
                toggleRecord(true);
                // unreg the listeners and wait for the alarm
                // TODO commented out alarm service
//                mSensorManager.unregisterListener(SensorService.this);
                showNotification();
            }

            if (action.equals(BaseActivity.ACTION_RECORD_ALL)) {
                // record all
                Log.d(TAG, "Record all command received.");
                toggleRecord(true);
                // unreg the listeners and wait for the alarm
                // TODO commented out alarm service
//                mSensorManager.unregisterListener(SensorService.this);
                showNotification();
            }
            if (action.equals(BaseActivity.ACTION_WAKE)) {
//                isPlotting = false;
                Log.d(TAG, "Alarm received." + " Type: " + sensorType);

                toggleRecord(!isRecord);

                if (isRecord) {
                    regSensorListeners();
                } else {
                    mSensorManager.unregisterListener(SensorService.this);
                }
            }

        }
    };

	@Override
	public void onCreate() {
		super.onCreate();

		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BaseActivity.ACTION_STOP);
        intentFilter.addAction(BaseActivity.ACTION_RECORD);
        intentFilter.addAction(BaseActivity.ACTION_RECORD_ALL);
        intentFilter.addAction(BaseActivity.ACTION_WAKE);
        registerReceiver(broadcastReceiver, intentFilter);

        Log.d(TAG, "Sensor Service Created.");
    }
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
        regSensorListeners();

        sensorType = intent.getIntExtra("sensorType", 0);

        Log.i(TAG, "Received onStartCommand id " + startId + ": " + " Type: " + sensorType);

        switch (sensorType) {
            case Sensor.TYPE_LIGHT:
                lightToggle = true;
                break;
            case Sensor.TYPE_RELATIVE_HUMIDITY:
                relaHumToggle = true;
                break;
            case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
                gyroToggle = true;
                break;
            case Sensor.TYPE_GYROSCOPE:
                gyroToggle = true;
                break;
            case Sensor.TYPE_STEP_DETECTOR:
                stepDetectorToggle = true;
                break;
            case Sensor.TYPE_GRAVITY:
                gravityToggle = true;
                break;
            case Sensor.TYPE_STEP_COUNTER:
                stepCounterToggle = true;
                break;
            case Sensor.TYPE_SIGNIFICANT_MOTION:
                sigMotionToggle = true;
                break;
            case Sensor.TYPE_PROXIMITY:
                proxiToggle = true;
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                magToggle = true;
                break;
            case Sensor.TYPE_ROTATION_VECTOR:
                rotVecToggle = true;
                break;
            case Sensor.TYPE_PRESSURE:
                pressureToggle = true;
                break;
            case Sensor.TYPE_ACCELEROMETER:
                accToggle = true;
                break;
            case Sensor.TYPE_AMBIENT_TEMPERATURE:
                tempToggle = true;
                break;
            case Sensor.TYPE_GAME_ROTATION_VECTOR:
                gameRotToggle = true;
                break;
            case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:
                geoMagRotToggle = true;
                break;
            case Sensor.TYPE_LINEAR_ACCELERATION:
                linAccToggle = true;
                break;
            case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                magToggle = true;
                break;
            case Sensor.TYPE_ALL:
                toggleAllSensors(true);
                break;
        }

        return START_STICKY;
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		Log.d(TAG, "onBind Service");
        return mBinder;
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "Sensor Service onDestroy");
        super.onDestroy();

        // Tell the user we stopped.
        Toast.makeText(this, "Sensor Service Stopped.", Toast.LENGTH_SHORT).show();
        notificationManager.cancel(SENSOR_RECORDING_NOTIFICATION_ID);
        mSensorManager.unregisterListener(this);
        unregisterReceiver(broadcastReceiver);
        AlarmScheduler.cancelAlarm(this);
        stopSelf();

	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {

		Sensor mSensor = event.sensor;
		
		if (mSensor.getType() == Sensor.TYPE_ACCELEROMETER && accToggle) {
			
			accVals = SensorDataUtility.lowPass(event.values, accVals, 0.1f);
			sendMessage(String.valueOf(Sensor.TYPE_ACCELEROMETER), accVals);
			recordToFile("accelerometer", accVals);
			
		} else if (mSensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE && tempToggle) {

			// By giving alpha as 1.0f, we're receiving the value without any filter
			tempVal = SensorDataUtility.lowPass(event.values, tempVal, 1.0f);
			sendMessage(String.valueOf(Sensor.TYPE_AMBIENT_TEMPERATURE), tempVal);
			recordToFile("ambient_temperature", tempVal);

		} else if (mSensor.getType() == Sensor.TYPE_GRAVITY && gravityToggle) {
			
			gravVals = SensorDataUtility.lowPass(event.values, gravVals, 0.6f);
			sendMessage(String.valueOf(Sensor.TYPE_GRAVITY), gravVals);
			recordToFile("gravity", gravVals);
			
		} else if (mSensor.getType() == Sensor.TYPE_GYROSCOPE && gyroToggle) {
			
			gyroVals = SensorDataUtility.lowPass(event.values, gyroVals, 0.6f);
			sendMessage(String.valueOf(Sensor.TYPE_GYROSCOPE), gyroVals);
			recordToFile("gyroscope", gyroVals);
			
		} else if (mSensor.getType() == Sensor.TYPE_LIGHT && lightToggle) {
			
			// By giving alpha as 1.0f, we're receiving the value without any filter
			lightVal = SensorDataUtility.lowPass(event.values, lightVal, 1.0f);
			sendMessage(String.valueOf(Sensor.TYPE_LIGHT), lightVal);
			recordToFile("light", lightVal);
			
		} else if (mSensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION && linAccToggle) {
			
			lineAccVals = SensorDataUtility.lowPass(event.values, lineAccVals, 0.6f);
			sendMessage(String.valueOf(Sensor.TYPE_LINEAR_ACCELERATION), lineAccVals);
			recordToFile("linear_acceleration", lineAccVals);
			
		} else if (mSensor.getType() == Sensor.TYPE_MAGNETIC_FIELD && magToggle) {
			
			magVals = SensorDataUtility.lowPass(event.values, magVals, 0.2f);
			sendMessage(String.valueOf(Sensor.TYPE_MAGNETIC_FIELD), magVals);
			recordToFile("magnetic_field", magVals);
			
		} else if (mSensor.getType() == Sensor.TYPE_PRESSURE && pressureToggle) {
			
			presVal = SensorDataUtility.lowPass(event.values, presVal, 0.5f);
			sendMessage(String.valueOf(Sensor.TYPE_PRESSURE), presVal);
			recordToFile("pressure", presVal);
			
		} else if (mSensor.getType() == Sensor.TYPE_PROXIMITY && proxiToggle) {
			
			proxVal = SensorDataUtility.lowPass(event.values, proxVal, 1.0f);
			sendMessage(String.valueOf(Sensor.TYPE_PROXIMITY), proxVal);
			recordToFile("proximity", proxVal);
			
		} else if (mSensor.getType() == Sensor.TYPE_RELATIVE_HUMIDITY && relaHumToggle) {
			
			rhVal = SensorDataUtility.lowPass(event.values, rhVal, 1.0f);
			sendMessage(String.valueOf(Sensor.TYPE_RELATIVE_HUMIDITY), rhVal);
			recordToFile("relative_humidity", rhVal);
			
		} else if (mSensor.getType() == Sensor.TYPE_ROTATION_VECTOR && rotVecToggle) {

			rotVals = SensorDataUtility.lowPass(event.values, rotVals, 0.6f);
			sendMessage(String.valueOf(Sensor.TYPE_ROTATION_VECTOR), rotVals);
			recordToFile("rotation_vector", rotVals);

		} else if (mSensor.getType() == Sensor.TYPE_STEP_COUNTER && stepCounterToggle) {
            // no filter needed
            stepCountVals = event.values;
            sendMessage(String.valueOf(Sensor.TYPE_STEP_COUNTER), stepCountVals);
            recordToFile("step_counter", stepCountVals);

        } else if (mSensor.getType() == Sensor.TYPE_STEP_DETECTOR && stepDetectorToggle) {

            stepDetectVals = event.values;
            sendMessage(String.valueOf(Sensor.TYPE_STEP_DETECTOR), stepDetectVals);
            recordToFile("step_detector", stepDetectVals);

        } else if (mSensor.getType() == Sensor.TYPE_SIGNIFICANT_MOTION && sigMotionToggle) {
            //
        } else if (mSensor.getType() == Sensor.TYPE_GAME_ROTATION_VECTOR && gameRotToggle) {

            gameRotVals = SensorDataUtility.lowPass(event.values, gameRotVals, 0.6f);
            sendMessage(String.valueOf(Sensor.TYPE_GAME_ROTATION_VECTOR), gameRotVals);
            recordToFile("game_rotation_vector", gameRotVals);

        } else if (mSensor.getType() == Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR && geoMagRotToggle) {

            geoMagRotVals = SensorDataUtility.lowPass(event.values, geoMagRotVals, 0.6f);
            sendMessage(String.valueOf(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR), geoMagRotVals);
            recordToFile("geomagnetic_rotation_vector", geoMagRotVals);
        }

	}

    /**
     * Show a notification while this service is running.
     */
    public void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        Intent intentStop = new Intent(BaseActivity.ACTION_STOP);
        PendingIntent piStop = PendingIntent.getBroadcast(this, 0, intentStop, PendingIntent.FLAG_UPDATE_CURRENT);

        // Set the icon, scrolling text and timestamp
        NotificationCompat.Builder notiBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_action_phone)
                .setContentTitle("Recording Sensor Data")
                // set progress to be indeterminate
                .setProgress(0, 0, true)
                .addAction(R.drawable.ic_action_stop, "Stop", piStop);

            //TODO set different coloured icons for lower version Android
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
//        } else {
//        }

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        // Set the info for the views that show in the notification panel.
//        notification.setLatestEventInfo(this, getText(R.string.local_service_label),
//                text, contentIntent);
        notiBuilder.setContentIntent(contentIntent);
        Notification notification = notiBuilder.build();
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        // Send the notification
        notificationManager.notify(SENSOR_RECORDING_NOTIFICATION_ID, notification);
    }


	@SuppressLint("InlinedApi")
	private void regSensorListeners() {
		if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
//			Log.i(TAG, "Accelerometer");
			mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(
                    Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
		}
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			if (mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) != null) {
				//			Log.i(TAG, "Ambient Temperature");
				mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(
                        Sensor.TYPE_AMBIENT_TEMPERATURE), SensorManager.SENSOR_DELAY_NORMAL);
			}
		}

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR) != null) {
            mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(
                    Sensor.TYPE_GAME_ROTATION_VECTOR), SensorManager.SENSOR_DELAY_NORMAL);

        }

		if (mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) != null) {
//			Log.i(TAG, "Gravity");
			mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(
                    Sensor.TYPE_GRAVITY), SensorManager.SENSOR_DELAY_NORMAL);

		}
		
		if (mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null) {
//			Log.i(TAG, "Gyroscope");
			mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(
                    Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_NORMAL);

		}

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED) != null) {
            mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(
                    Sensor.TYPE_GYROSCOPE_UNCALIBRATED), SensorManager.SENSOR_DELAY_NORMAL);

        }

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR) != null) {
            mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(
                    Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR), SensorManager.SENSOR_DELAY_NORMAL);

        }

		if (mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) != null) {
//			Log.i(TAG, "Light");
			mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(
                    Sensor.TYPE_LIGHT), SensorManager.SENSOR_DELAY_NORMAL);

		}
		
		if (mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null) {
//			Log.i(TAG, "Linear Acceleration");
			mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(
                    Sensor.TYPE_LINEAR_ACCELERATION), SensorManager.SENSOR_DELAY_NORMAL);

		}
		
		if (mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null) {
//			Log.i(TAG, "Magnetic Field");
			mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(
                    Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);

		}

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED) != null) {
            mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(
                    Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED), SensorManager.SENSOR_DELAY_NORMAL);

        }
		
//		if (mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION) != null)
//			Log.i(TAG, "Orientation");
		if (mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE) != null) {
//			Log.i(TAG, "Pressure");
			mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(
                    Sensor.TYPE_PRESSURE), SensorManager.SENSOR_DELAY_NORMAL);

		}
		
		if (mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) != null) {
//			Log.i(TAG, "Proximity");
			mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(
                    Sensor.TYPE_PROXIMITY), SensorManager.SENSOR_DELAY_NORMAL);

		}
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			if (mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY) != null) {
//			Log.i(TAG, "Relative Humidity");
				mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(
                        Sensor.TYPE_RELATIVE_HUMIDITY), SensorManager.SENSOR_DELAY_NORMAL);
			}
		}
		
		if (mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null) {
//			Log.i(TAG, "Rotation Vector");
			mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(
                    Sensor.TYPE_ROTATION_VECTOR), SensorManager.SENSOR_DELAY_NORMAL);

		}

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION) != null) {
            mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(
                    Sensor.TYPE_SIGNIFICANT_MOTION), SensorManager.SENSOR_DELAY_NORMAL);

        }

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null) {
            mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(
                    Sensor.TYPE_STEP_COUNTER), SensorManager.SENSOR_DELAY_NORMAL);

        }

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) != null) {
            mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(
                    Sensor.TYPE_STEP_DETECTOR), SensorManager.SENSOR_DELAY_NORMAL);

        }
	}

    private void toggleAllSensors(boolean toggle) {
        accToggle =
        gyroToggle =
        gravityToggle =
        linAccToggle =
        magToggle =
        rotVecToggle =
        tempToggle =
        lightToggle =
        pressureToggle =
        proxiToggle =
        relaHumToggle =
        stepDetectorToggle =
        stepCounterToggle =
        sigMotionToggle =
        gameRotToggle =
        geoMagRotToggle = toggle;
    }

	public void toggleRecord(boolean toggle) {
		this.isRecord = toggle;
	}
	
//	public void setBackground(boolean state) {
//		this.isBackground = state;
//	}
	
	private void sendMessage(String sensorName, float[] values) {
		Intent intent = new Intent(BaseActivity.MSG_SENSOR_DATA);
		intent.putExtra(sensorName, values);
        intent.putExtra("timestamp", new Date().getTime());
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}
	
	/**
	 * 
	 * @param sensorName
	 * @param values
	 * @throws IOException
	 */
	private void recordToFile(String sensorName, float[] values){
		// First check the storage state
		if (checkStorageState() == true && isRecord == true) {
            try {
                File f = new File(getStorageDirectory(), sensorName + ".csv");
                CSVWriter csvWriter = new CSVWriter(new FileWriter(f, true));
//			    String[] header = "Time#x#y#z".split("#");
//			    csvWriter.writeNext(header);
                long date = new Date().getTime();
                csvWriter.writeNext(new String[]{Long.toString(date),
                        Float.toString(values[0]),
                        Float.toString(values[1]),
                        Float.toString(values[2])});
                csvWriter.close();

                Log.d(TAG, "Record: " + date);

            } catch (IOException e) {
                e.printStackTrace();
            }
		}

	}
	
	/**
	 * Check the External Storage state
	 * @return true if the external storage is available and readable;
	 * 			false if the external storage state is either unavailable or not readable
	 */
	private boolean checkStorageState() {
		boolean mExternalStorageAvailable = false;
		boolean mExternalStorageWriteable = false;
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
			// We can read and write the media
			mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			// We can only read the media
			mExternalStorageAvailable = true;
			mExternalStorageWriteable = false;
		} else {
			// Something else is wrong. It may be one of many other states, but all we need
			//  to know is we can neither read nor write
			mExternalStorageAvailable = mExternalStorageWriteable = false;
            Toast.makeText(this, "No SD card or damaged SD card.", Toast.LENGTH_LONG).show();
		}
		
		return (mExternalStorageAvailable && mExternalStorageWriteable);
	}
	
	private File getStorageDirectory() {
//		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
//			return getExternalFilesDir(null);
//		} else {
//			return new File(Environment.getExternalStorageDirectory(), 
//					"/Android/data/" + this.getPackageName() + "/files/");
//		}
		// This will create a public folder under the sdcard root
		File f = new File(Environment.getExternalStorageDirectory(), "/SensorFun/");
		if (f.exists() == false) {
			if (f.mkdirs() == true) {
				return f;
			} else {
				return null;
			}
		} else {
			return f;
		}
		 
	}
}
