package edu.washington.cs.opencvtests;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class AudioTest {
	private static final String TAG = "AudioTest";
	
	private static final int SAMPLE_RATE = 44100;
	private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
	private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
	private static final int PREFERRED_BUFFER_SIZE = 2048;
	
	private AudioRecord mAudioRecorder;
	private FastFourierTransform mFFT;
	
	private int mBufferSize;
	
	private short [] mRawBuffer;
	private double [] mFloatingPointBuffer;
	private double [] mFrequencySpectrum;
	
	private Runnable mReadAudioDataRunnable = new Runnable() {

		@Override
		public void run() {
			while(mAudioRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
				mAudioRecorder.read(mRawBuffer, 0, mRawBuffer.length);
				
				// convert the raw buffer into floating points
				for(int i = 0; i < mRawBuffer.length; i++) {
					mFloatingPointBuffer[i] = (double)mRawBuffer[i] / (double)Short.MAX_VALUE;
					mFrequencySpectrum[i] = 0.0;
				}
				
				// get the frequency spectrum
				mFFT.fft(mFloatingPointBuffer, mFrequencySpectrum);
				
				// look at 1800 - 2200 hz. That's 83 - 102 approximately
				/*double average = 0.0;
				for(int i = 83; i < 103; i++) {
					average += mFrequencySpectrum[i];
				}
				average /= 103 - 83;
				Log.d(TAG, String.valueOf(mFrequencySpectrum[93]));*/
				
				// find the max value
				double max = mFrequencySpectrum[0];
				int maxFrequencyBand = 0;
				for(int i = 1; i < mFrequencySpectrum.length; i++) {
					if(max < mFrequencySpectrum[i]) {
						max = mFrequencySpectrum[i];
						maxFrequencyBand = i;
					}
				}
				
				if(max > 20)
					Log.d(TAG, String.format("Band: %d, Amplitude: %f", maxFrequencyBand, max));
				
			}
		}
		
	};
	Thread mReadAudioDataThread;
	
	public AudioTest() {
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
		mRawBuffer = new short[mBufferSize / 2];
		mFloatingPointBuffer = new double[mRawBuffer.length];
		mFrequencySpectrum = new double[mFloatingPointBuffer.length];
		
		// initialize the read thread
		mReadAudioDataThread = new Thread(mReadAudioDataRunnable);
		
		mFFT = new FastFourierTransform(mFrequencySpectrum.length);
	}
	
	public void start() {
		mAudioRecorder.startRecording();
		mReadAudioDataThread.start();
	}
	
	public void stop() {
		mAudioRecorder.stop();
		try {
			mReadAudioDataThread.join();
		} catch (InterruptedException e) {
			// this should not occur
		}
	}
}
