package edu.washington.cs.handsfreelibrary;

import java.util.LinkedList;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class ClapSensor implements Runnable {
	private static final String TAG = "ClapSensor";
	
	private static final int SAMPLE_RATE = 44100;
	private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
	private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
	private static final int BITS_PER_SAMPLE = 16; // make sure this matches audio format;
	private static final int PREFERRED_BUFFER_SIZE = 2048;
	
	private static final int NUMBER_IN_LIST = 8;
	private static final int MAX_LENGTH_OF_CLAP = 7; // in buffer size chunks
	private static final int TIME_TO_STAY_AVERAGE = 3; // also in buffer size chunks
	private static final double MIN_CLAP_TO_SILENCE_RATIO = 8.0;
	private static final double MIN_PRE_TO_POST_RATIO = 0.3;
	
	// how many samples do we need before average can be ascertained
	private AudioRecord mAudioRecorder;
	private boolean mIsStarted;
	
	private ClickListener mListener;
	
	private int mBufferSize;
	
	private byte [] mRawBuffer;
	
		
	Thread mReadAudioDataThread;
	
	private LinkedList<Double> mSampleAvgValueList;
	private double mRunningAverage;
	private double mOldAverage;
	
	public ClapSensor() {
		
		// check if our preferred buffer is smaller than the min, and if it is, use the min
		mBufferSize = Math.max(AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT), PREFERRED_BUFFER_SIZE);
		
		// round buffersize up to the nearest power of 2
		mBufferSize = (int) Math.pow(2, Math.ceil((Math.log(mBufferSize / 2)/Math.log(2))));
		mBufferSize *= 2;
		
		// initialize the audio recorder using the given audio properties
		mAudioRecorder = new AudioRecord(
				MediaRecorder.AudioSource.MIC,
				SAMPLE_RATE,
				CHANNEL_CONFIG,
				AUDIO_FORMAT,
				mBufferSize);
		
		// allocate the data for the buffer such that sizeof(mBuffer) == mBufferSize
		// (allocate mBufferSize / 2 because we're working with 16 bit shorts)
		mRawBuffer = new byte[mBufferSize];
		
		mListener = null;
		
		mIsStarted = false;
		
		mSampleAvgValueList = new LinkedList<Double>();
		mRunningAverage = 0.0;
		mOldAverage = -1.0;
	}
	
	public void start() {
		if(!mIsStarted) {
			mAudioRecorder.startRecording();
			
			mReadAudioDataThread = new Thread(this);
			mReadAudioDataThread.start();
			
			mIsStarted = true;
		}
	}
	
	public void stop() {
		if(mIsStarted) {
			mAudioRecorder.stop();
			try {
				mReadAudioDataThread.join();
			} catch (InterruptedException e) {
				// TEMP: this should not occur
			}
			mIsStarted = false;
		}
	}
	
	public void setListener(ClickListener listener) {
		mListener = listener;
	}
	
	// returns 1.0 if v1 == v2, and smaller numbers the further apart they are
	private double getRatioBetween(double v1, double v2) {
		if(v1 > v2) {
			return v2 / v1;
		} else {
			return v1 / v2;
		}
	}
	
	private int clapCounter = 0;
	private int breakCounter = 0;
	
	@Override
	public void run() {
		while(mAudioRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
			// read in the next set of clap data
			mAudioRecorder.read(mRawBuffer, 0, mRawBuffer.length);
			
			// average the amplitudes of the data set
			short sample = 0;
			int totalAbsValue = 0;
			double averageAbsValue = 0.0;
			
			for(int i = 0; i < mRawBuffer.length; i+=2) {
				sample = (short)((mRawBuffer[i]) | mRawBuffer[i + 1] << 8);
				totalAbsValue += Math.abs(sample);
			}
			averageAbsValue = totalAbsValue / mRawBuffer.length / 2.0;
			
			if(breakCounter == 0) {
				// now, let's check if our latest number is far from the norm
				if(mSampleAvgValueList.size() == NUMBER_IN_LIST && clapCounter == 0) {
					if(averageAbsValue > mRunningAverage * MIN_CLAP_TO_SILENCE_RATIO) {
						// potential clap detected!
						mOldAverage = mRunningAverage;
						clapCounter++;
					}
				} else if(clapCounter >= MAX_LENGTH_OF_CLAP) {
					// the clap lasted too long. Let's let it be.
					clapCounter = 0;
					breakCounter = NUMBER_IN_LIST;
				} else if(clapCounter > 0) {
					// see if we're still at a "clap" point
					if(getRatioBetween(averageAbsValue, mOldAverage) < MIN_PRE_TO_POST_RATIO)
						clapCounter++;
					else {
						// we're out of the clap, I believe
						mSampleAvgValueList.clear();
						
						clapCounter = -TIME_TO_STAY_AVERAGE;
					}
				} else if(clapCounter < 0) {
					if(getRatioBetween(averageAbsValue, mOldAverage) >= MIN_PRE_TO_POST_RATIO) {
						clapCounter++;
						if(clapCounter == 0 && mListener != null)
							mListener.onSensorClick();
					}
					else clapCounter = 0;
				} 
			} else breakCounter--;
			
			// update the average
			mRunningAverage *= mSampleAvgValueList.size();
			
			mSampleAvgValueList.add(averageAbsValue);
			if(mSampleAvgValueList.size() > NUMBER_IN_LIST) {
				mRunningAverage -= mSampleAvgValueList.pop();
			}
			mRunningAverage = (mRunningAverage + averageAbsValue) / mSampleAvgValueList.size();
		}
	}
	
	public boolean isStarted() {
		return mIsStarted;
	}
}
