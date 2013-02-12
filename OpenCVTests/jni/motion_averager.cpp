#include <jni.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>
#include <vector>

using namespace std;
using namespace cv;

#define ELEM(type,start,step,size,xpos,ypos) *((type*)(start+step*(ypos)+(xpos)*size))

extern "C" {

    const double EPSILON = 0.00001;
    
JNIEXPORT jobject JNICALL Java_edu_washington_cs_opencvtests_MovementDetection_DetectMovementPosition(JNIEnv* env, jobject, jlong addrMat);

JNIEXPORT jobject JNICALL Java_edu_washington_cs_opencvtests_MovementDetection_DetectMovementPosition(JNIEnv* env, jobject, jlong addrMat)
{
    Mat& input  = *(Mat*)addrMat;
    
    Point2d avg(-1.0, -1.0);
    double pointsCounted = 0.0;

    for(int y = 0; y < input.rows; y++)
    {
        for(int x = 0; x < input.cols; x++) {
            if(ELEM(char, input.data, input.step, 1, x, y) == 0) {
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
    object = env->NewObject(cls, constructor, avg.x, avg.y, pointsCounted / (double)(input.rows * input.cols));

    return object;
}

}
