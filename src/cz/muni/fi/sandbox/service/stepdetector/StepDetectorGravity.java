package cz.muni.fi.sandbox.service.stepdetector;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import cz.muni.fi.sandbox.dsp.filters.CumulativeSignalPowerTD;
import cz.muni.fi.sandbox.dsp.filters.MovingAverageTD;
import cz.muni.fi.sandbox.dsp.filters.SignalPowerTD;

/**
 * MovingAverageStepDetector class, step detection filter based on two moving averages
 * with minimum signal power threshold 
 * 
 * @author Michal Holcik
 * 
 */
public class StepDetectorGravity extends StepDetector {
	
	private static final String TAG = "GravityStepDetector";
	private float[] mValue;
	private float mSignalPower;
	private CumulativeSignalPowerTD asp;
	private boolean mSwapState;
	private boolean stepDetected;
	private boolean signalPowerCutoff;
	private long mLastStepTimestamp;
	private double strideDuration;
	
	private static final long SECOND_IN_NANOSECONDS = (long) Math.pow(10, 9);
	private static final long POWER_WINDOW = SECOND_IN_NANOSECONDS / 10;
	public static final float POWER_CUTOFF_VALUE = 2000.0f;
	private static final double MAX_STRIDE_DURATION = 2.0; // in seconds
//	private long mWindowPower;
	private float mPowerCutoff;
	
	public StepDetectorGravity() {
		this(POWER_CUTOFF_VALUE);
	}

	public StepDetectorGravity(double powerCutoff) {
		mValue = new float[2];
		
		
		mPowerCutoff = (float)powerCutoff;
		
		mSwapState = true;
		
		asp = new CumulativeSignalPowerTD();
		stepDetected = false;
		signalPowerCutoff = true;
	}

	public class StepDetectorGravityState {
		float[] value;
		float power;
		public boolean[] states;
		double duration;

		StepDetectorGravityState(float[] value, float power, boolean[] states, double duration) {
			this.value = value;
			this.power = power;
			this.states = states;
		}
	}

	public StepDetectorGravityState getState() {
		return new StepDetectorGravityState(mValue, mSignalPower, new boolean[] {
				stepDetected, signalPowerCutoff }, strideDuration);
	}

	public float getPowerThreshold() {
		return mPowerCutoff;
	}

	private void processSensorValues(long timestamp, float[] values) {
		float value = (float) values[2];

		// detect crossover
		boolean newSwapState = (mValue[0] >= mValue[1]) && (mValue[1] <= value);
		stepDetected = false;
		if (newSwapState != mSwapState) {
			mSwapState = newSwapState;
			if (mSwapState) {
				stepDetected = true;
			}
		}
		
		asp.push(timestamp, value);
		
		mSignalPower = (float) asp.getValue();
		signalPowerCutoff = mSignalPower < mPowerCutoff;

		if (stepDetected) {
			asp.reset();
		}
		
		mValue[0] = mValue[1];
		mValue[1] = value;

		// step event
		if (stepDetected && !signalPowerCutoff) {
			strideDuration = getStrideDuration();
			notifyOnStep(new StepEvent(1.0, strideDuration));
		}
	}

	/**
	 * call has side-effects, must call only when step is detected.
	 * 
	 * @return stride duration if the duration is less than MAX_STRIDE_DURATION,
	 *         NaN otherwise
	 */
	private double getStrideDuration() {
		// compute stride duration
		long currentStepTimestamp = System.nanoTime();
		double strideDuration;
		strideDuration = (double) (currentStepTimestamp - mLastStepTimestamp)
				/ SECOND_IN_NANOSECONDS;
		if (strideDuration > MAX_STRIDE_DURATION) {
			strideDuration = Double.NaN;
		}
		mLastStepTimestamp = currentStepTimestamp;
		return strideDuration;
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// Log.d(TAG, "sensor: " + sensor + ", x: " + values[0] + ", y: " +
		// values[1] + ", z: " + values[2]);
		synchronized (this) {
			if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
				processSensorValues(event.timestamp, event.values);
			}
		}
	}
}
