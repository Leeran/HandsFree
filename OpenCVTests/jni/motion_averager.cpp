#include <jni.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>
#include <vector>

using namespace std;
using namespace cv;

// generic version
#define GENERIC_ELEM(type,start,step,size,xpos,ypos) *((type*)(start+step*(ypos)+(xpos)*size))

// specific version
#define ELEM(mat,xpos,ypos) *((unsigned char*)(mat.data+mat.step*(ypos)+(xpos)))

extern "C" {

    const double EPSILON = 0.00001;
    
    const int KERNEL_SIZE       = 3;
    const int KERNEL_SIZE2      = KERNEL_SIZE * KERNEL_SIZE;
    const int HALF_KERNEL_SIZE  = KERNEL_SIZE / 2;
    
JNIEXPORT jobject JNICALL Java_edu_washington_cs_opencvtests_MovementDetection_DetectMovementPosition(JNIEnv* env, jobject,
                                                                                                      jlong currentFrameAddr,
                                                                                                      jlong previousFrameAddr);

JNIEXPORT jobject JNICALL Java_edu_washington_cs_opencvtests_MovementDetection_DetectMovementPosition(JNIEnv* env, jobject,
                                                                                                      jlong currentFrameAddr,
                                                                                                      jlong previousFrameAddr)
{
    Mat& currentFrame  = *(Mat*)currentFrameAddr;
    Mat& previousFrame  = *(Mat*)previousFrameAddr;
    
    Point2d avg(-1.0, -1.0);
    double pointsCounted = 0.0;
    
    // In order to blur, we keep track of the sum of the current KERNEL_SIZE X KERNEL_SIZE area around the the pixel,
    // plus a queue of the sum of columns so that we can subtract the last column out and add the next column in every
    // x pixel.
    
    int columnSumQueue[KERNEL_SIZE];
    int currQueueIndex;
    int runningSum;

    for(int y = HALF_KERNEL_SIZE; y < currentFrame.rows - HALF_KERNEL_SIZE; y++)
    {
        // calculate the area around the current y for blurring purposes
        runningSum = 0;
        for(int i = 0; i < KERNEL_SIZE-1; i++) {
            columnSumQueue[i] = 0;
            for(int j = 0; j < KERNEL_SIZE; j++) {
                int ly = y+j-HALF_KERNEL_SIZE;
                columnSumQueue[i] += abs(ELEM(currentFrame, i, ly) - ELEM(previousFrame, i, ly));
            }
            runningSum += columnSumQueue[i];
        }
        columnSumQueue[KERNEL_SIZE-1] = 0;
        currQueueIndex = KERNEL_SIZE-2;
        
        for(int x = HALF_KERNEL_SIZE; x < currentFrame.cols - HALF_KERNEL_SIZE; x++) {
            currQueueIndex = (currQueueIndex + 1) % KERNEL_SIZE;
            
            runningSum -= columnSumQueue[currQueueIndex];
            columnSumQueue[currQueueIndex] = 0;
            for(int j = 0; j < KERNEL_SIZE; j++) {
                int lx = x+HALF_KERNEL_SIZE, ly = y+j-HALF_KERNEL_SIZE;
                columnSumQueue[currQueueIndex] += abs(ELEM(currentFrame, lx, ly) - ELEM(previousFrame, lx, ly));
            }
            runningSum += columnSumQueue[currQueueIndex];
            
            int currPixel = runningSum / KERNEL_SIZE2;
            
            if(currPixel > 50) {
                avg.x = (avg.x * pointsCounted + (double)x) / (pointsCounted + 1.0);
                avg.y = (avg.y * pointsCounted + (double)y) / (pointsCounted + 1.0);
                
                pointsCounted++;
            }
        }
    }
    
    // create the point object to be returned
    jobject object;
    jmethodID constructor;
    jclass cls;
    
    cls = env->FindClass("edu/washington/cs/opencvtests/MotionDetectionReturnValue");
    constructor = env->GetMethodID(cls, "<init>", "(DDD)V");
    object = env->NewObject(cls, constructor, avg.x, avg.y, pointsCounted / (double)(currentFrame.rows * currentFrame.cols));

    return object;
}

}
