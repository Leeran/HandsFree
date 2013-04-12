package edu.washington.cs.opencvtests;

import java.io.File;
import java.io.IOException;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;

public class ClapDetector {

    private static final String TAG = "Clapper";

    private static final long DEFAULT_CLIP_TIME = 200;
    private long clipTime = DEFAULT_CLIP_TIME;
    
    private static final int MAX_NUMBER_OF_CLIPS = 100;

    private boolean continueRecording;

    /**
     * how much louder is required to hear a clap 10000, 18000, 25000 are good
     * values
     */
    private int amplitudeThreshold;

    /**
     * requires a little of noise by the user to trigger, background noise may
     * trigger it
     */
    public static final int AMPLITUDE_DIFF_LOW = 10000;
    public static final int AMPLITUDE_DIFF_MED = 18000;
    /**
     * requires a lot of noise by the user to trigger. background noise isn't
     * likely to be this loud
     */
    public static final int AMPLITUDE_DIFF_HIGH = 25000;

    private static final int DEFAULT_AMPLITUDE_DIFF = AMPLITUDE_DIFF_MED;

    private MediaRecorder recorder;

    private String tmpAudioFile;

    public ClapDetector() throws IOException
    {
        this(DEFAULT_CLIP_TIME, File.createTempFile("tmp", "3gp").getAbsolutePath(), DEFAULT_AMPLITUDE_DIFF);
    }

    public ClapDetector(long snipTime, String tmpAudioFile,
            int amplitudeDifference)
    {
        this.clipTime = snipTime;
        this.amplitudeThreshold = amplitudeDifference;
        this.tmpAudioFile = tmpAudioFile;
    }

    public synchronized boolean recordClap() throws Exception
    {
        Log.d(TAG, "record clap");
        boolean clapDetected = false;

        try
        {
            recorder = prepareRecorder(tmpAudioFile);
        }
        catch (IOException io)
        {
            Log.d(TAG, "failed to prepare recorder ", io);
            throw new Exception("failed to create recorder");
        }

        recorder.start();
        int startAmplitude = recorder.getMaxAmplitude();
        Log.d(TAG, "starting amplitude: " + startAmplitude);
        continueRecording = true;
        
        int numClips = 0;

        do
        {
            Log.d(TAG, "waiting while recording...");
            waitSome();
            int finishAmplitude = recorder.getMaxAmplitude();
            
            int ampDifference = finishAmplitude - startAmplitude;
            if (ampDifference >= amplitudeThreshold)
            {
                Log.d(TAG, "heard a clap!");
                clapDetected = true;
            }
            Log.d(TAG, "finishing amplitude: " + finishAmplitude + " diff: "
                    + ampDifference);
            
            numClips++;
            
        } while (continueRecording && !clapDetected && numClips < MAX_NUMBER_OF_CLIPS);

        Log.d(TAG, "stopped recording");
        done();

        return clapDetected;
    }

    private void waitSome()
    {
        try
        {
            // wait a while
            Thread.sleep(clipTime);
        } catch (InterruptedException e)
        {
            Log.d(TAG, "interrupted");
        }
    }

    /**
     * need to call this when completely done with recording
     */
    public void done()
    {
        Log.d(TAG, "stop recording");
        if (recorder != null)
        {
            if (isRecording())
            {
                stopRecording();
            }
            //now stop the media player
            recorder.stop();
            recorder.release();
            // delete the file
            new File(tmpAudioFile).delete();
        }
    }

    public boolean isRecording()
    {
        return continueRecording;
    }

    public void stopRecording()
    {
        continueRecording = false;
        synchronized(this) {
        	return;
        }
    }
    
    // helper functions
    private static MediaRecorder prepareRecorder(String sdCardPath)
            throws IOException
    {
        if (!isStorageReady())
        {
            throw new IOException("SD card is not available");
        }

        MediaRecorder recorder = new MediaRecorder();

        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        Log.d(TAG, "recording to: " + sdCardPath);
        recorder.setOutputFile(sdCardPath);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        recorder.prepare();
        return recorder;
    }

    private static boolean isStorageReady() 
    {
        String cardstatus = Environment.getExternalStorageState();
        if (cardstatus.equals(Environment.MEDIA_REMOVED)
            || cardstatus.equals(Environment.MEDIA_UNMOUNTED)
            || cardstatus.equals(Environment.MEDIA_UNMOUNTABLE)
            || cardstatus.equals(Environment.MEDIA_MOUNTED_READ_ONLY))
        {
            return false;
        }
        else
        {
            if (cardstatus.equals(Environment.MEDIA_MOUNTED))
            {
                return true;
            }
            else
            {
                return false;
            }
        }
    }
}