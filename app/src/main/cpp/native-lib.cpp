#include <jni.h>
#include <opencv2/core.hpp>
#include<opencv2/opencv.hpp>


using namespace cv;

extern "C"
{
    JNIEXPORT void JNICALL
    Java_com_koi_arte_MainActivity_Canny(JNIEnv *env, jobject instance,
                                                          jlong photoAddr, jlong edgeAddr, jlong binaryAddr, jlong grayAddr) {


        Mat &photo = *(Mat*)photoAddr;
        Mat &edge = *(Mat*)edgeAddr;
        Mat &binary = *(Mat*)binaryAddr;
        Mat &gray = *(Mat*)grayAddr;


        cvtColor(photo, gray, CV_RGBA2GRAY);

        blur(gray, binary, Size(3,3));


        //선검출
        Canny(binary, edge, 55, 3, 3);

        //반전
        binary = ~edge;

    }

}
