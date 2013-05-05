package edu.washington.cs.handsfreelibrary;

import java.util.LinkedList;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.musicg.api.DetectionApi;
import com.musicg.wave.WaveHeader;

public class ClapSensor implements Runnable {
	private static final String TAG = "ClapSensor";
	
	private class SampleBufferArchive {
		public boolean isClap;
		public double averageValue;
		public SampleBufferArchive(boolean isclap, double avgval) {
			isClap = isclap;
			averageValue = avgval;
		}
		@Override
		public String toString() {
			return String.format("IsClap: " + (isClap ? "true" : "false") + ", avgValue: %f", averageValue);
		}
	}
	
	private static final int SAMPLE_RATE = 44100;
	private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
	private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
	private static final int BITS_PER_SAMPLE = 16; // make sure this matches audio format;
	private static final int PREFERRED_BUFFER_SIZE = 2048;
	
	private static final int NUMBER_IN_LIST = 20;
	private static double MIN_CLAP_TO_SILENCE_RATIO = 8.0;
	private static double MIN_PRE_TO_POST_RATIO = 0.6;
	
	// how many samples do we need before average can be ascertained
	private AudioRecord mAudioRecorder;
	private ClapDetector mClapDetector;
	private WaveHeader mWaveHeader;
	private boolean mIsStarted;
	
	private ClickListener mListener;
	
	private int mBufferSize;
	
	private byte [] mRawBuffer;
		
	Thread mReadAudioDataThread;
	
	private LinkedList<SampleBufferArchive> mClapList;
	
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
		
		// initialize the wave header/clap api
		mWaveHeader = new WaveHeader();
		mWaveHeader.setChannels(1);
		mWaveHeader.setBitsPerSample(BITS_PER_SAMPLE);
		mWaveHeader.setSampleRate(SAMPLE_RATE);
		mClapDetector = new ClapDetector(mWaveHeader);
		
		mListener = null;
		
		mIsStarted = false;
		
		mClapList = new LinkedList<SampleBufferArchive>();
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
	
	private int clapCounter = 0;
	
	@Override
	public void run() {
		while(mAudioRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
			mAudioRecorder.read(mRawBuffer, 0, mRawBuffer.length);
			boolean isClap = false;
			
			short sample = 0;
			int totalAbsValue = 0;
			double averageAbsValue = 0.0;
			
			for(int i = 0; i < mRawBuffer.length; i+=2) {
				sample = (short)((mRawBuffer[i]) | mRawBuffer[i + 1] << 8);
				totalAbsValue += Math.abs(sample);
			}
			averageAbsValue = totalAbsValue / mRawBuffer.length / 2.0;
			
			// check to see if we have data at all
			if(averageAbsValue >= 50.0 && mListener != null) {
				// we do, see if a clap is registered
				if(mClapDetector.isClap(mRawBuffer)) {
					isClap = true;
					if(clapCounter == 0 && mClapList.size() == NUMBER_IN_LIST)
						clapCounter = NUMBER_IN_LIST / 2;
				}
			}
			
			mClapList.add(new SampleBufferArchive(isClap, averageAbsValue));
			if(mClapList.size() > NUMBER_IN_LIST) mClapList.pop();
			
			if(clapCounter == 1) {
				for(SampleBufferArchive elem : mClapList)
					Log.d(TAG, elem.toString());
				
				// let's analyze to see if we're dealing with a real clap:
				double preClapAbsValue = mClapList.pop().averageValue, postClapAbsValue = 0.0, clapAbsValue;
				int preClapSampleBuffers = 1;
				for(int i = 1; i < NUMBER_IN_LIST / 2 - 2; i++) {
					double new_value = mClapList.pop().averageValue;
					if(new_value / (preClapAbsValue / preClapSampleBuffers) < 4.0) {
						preClapAbsValue += new_value;
						preClapSampleBuffers++;
					}
					else
						Log.d(TAG, "SOMETHING");
				}
				preClapAbsValue /= (double)preClapSampleBuffers;
				
				// we don't need the next value
				mClapList.pop();
				
				// max the next 2 for clap abs value
				clapAbsValue = Math.max(mClapList.pop().averageValue, mClapList.pop().averageValue);
				
				// average out the rest that aren't claps
				int postClapSampleBuffers = 0;
				while(!mClapList.isEmpty()) {
					SampleBufferArchive s = mClapList.pop();
					if(!s.isClap && s.averageValue < preClapAbsValue * 1.7) {
						postClapAbsValue += s.averageValue;
						postClapSampleBuffers++;
					}
				}
				if(postClapSampleBuffers != 0) {
					postClapAbsValue /= (double)postClapSampleBuffers;
				
					//now check to see if things work out
					double prePostRatio = Math.min(preClapAbsValue, postClapAbsValue) / Math.max(preClapAbsValue, postClapAbsValue);
					double clapToSilenceRatio = clapAbsValue / ((preClapAbsValue + postClapAbsValue) / 2.0);
					
					if(prePostRatio >= MIN_PRE_TO_POST_RATIO && clapToSilenceRatio >= MIN_CLAP_TO_SILENCE_RATIO) {
						// we have ourselves a clap!
						Log.d(TAG, "CLAP!");
						mListener.onSensorClick();
					}
					
					Log.d(TAG, "prePostRatio: " + prePostRatio + ", clapToSilenceRatio: " + clapToSilenceRatio);
				}
				clapCounter = 0;
			} else if(clapCounter > 1) clapCounter--;
		}
	}
	
	public boolean isStarted() {
		return mIsStarted;
	}
	
	private class ClapDetector extends DetectionApi{
        
        public ClapDetector(WaveHeader waveHeader) {
                super(waveHeader);
        }

        protected void init(){
                // settings for detecting a clap
                minFrequency = 500.0f;
                maxFrequency = Double.MAX_VALUE;
                
                // get the decay part of a clap
                minIntensity = 10000.0f;
                maxIntensity = 100000.0f;
                
                minStandardDeviation = 0.0f;
                maxStandardDeviation = 0.05f;
                
                highPass = 100;
                lowPass = 10000;
                
                minNumZeroCross = 100;
                maxNumZeroCross = 500;
                
                numRobust = 4;
        }
                
        public boolean isClap(byte[] audioBytes){
                return isSpecificSound(audioBytes);
        }
	}
}
