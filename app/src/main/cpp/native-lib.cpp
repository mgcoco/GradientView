#include <jni.h>
#include <string>
#include <stdio.h>
#include <Android/Log.h>
#include <list>

#define TAG    "GradientHandler"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,TAG ,__VA_ARGS__)

using namespace std;

static int ORIENTATION_HORIZONTAL = 0;
static int ORIENTATION_VERTICAL = 1;

void setupCurvPoint(JNIEnv* env, int orientation, jobject cp2, jobject cp1, jobject cp, int viewWidth, int viewHeight, list<int> *tmpX, list<int> *tmpY){
    jclass cpClass = env->GetObjectClass(cp);

    jfieldID xField = env->GetFieldID(cpClass, "x","F");
    jfieldID yField = env->GetFieldID(cpClass, "y","F");
    float cp2x = (float) env->GetFloatField(cp2, xField);
    float cp2y = (float) env->GetFloatField(cp2, yField);

    float cp1x = (float) env->GetFloatField(cp1, xField);
    float cp1y = (float) env->GetFloatField(cp1, yField);

    float cpx = (float) env->GetFloatField(cp, xField);
    float cpy = (float) env->GetFloatField(cp, yField);

    int p2x = (int)(cp2x * viewWidth);
    int p2y = (int)(cp2y * viewHeight);

    int p1x = (int)(cp1x * viewWidth);
    int p1y = (int)(cp1y * viewHeight);

    int px = (int)(cpx * viewWidth);
    int py = (int)(cpy * viewHeight);

    int lastValue = -1;
    if(orientation == ORIENTATION_VERTICAL){
        if((*tmpX).size() > 0){
            lastValue = (*tmpX).back();
        }
        for(float t = (float) 0.25; t <= 0.75; t = t + (float) (0.05)) {
            int deltaX = (int) (px * (1 - t) * (1 - t) * (1 - t) + 3 * p1x * t * (1 - t) * (1 - t) + 3 * p1x * t * t * (1 - t) + p2x * t * t * t);
            int deltaY = (int) (py * (1 - t) * (1 - t) * (1 - t) + 3 * p1y * t * (1 - t) * (1 - t) + 3 * p1y * t * t * (1 - t) + p2y * t * t * t);

            if(lastValue != deltaX && deltaX < viewWidth) {
                (*tmpX).push_back(deltaX);
                (*tmpY).push_back(deltaY);
                lastValue = (int)deltaX;
            }
        }
    }
    else{
        if((*tmpY).size() > 0){
            lastValue = (*tmpY).back();
        }
        for(float t = (float) 0.25; t <= 0.75; t = t + (float) (0.05)) {
            int deltaX = (int) (px * (1 - t) * (1 - t) * (1 - t) + 3 * p1x * t * (1 - t) * (1 - t) + 3 * p1x * t * t * (1 - t) + p2x * t * t * t);
            int deltaY = (int) (py * (1 - t) * (1 - t) * (1 - t) + 3 * p1y * t * (1 - t) * (1 - t) + 3 * p1y * t * t * (1 - t) + p2y * t * t * t);

            if(deltaY > lastValue && deltaY < viewHeight) {
                (*tmpX).push_back(deltaX);
                (*tmpY).push_back(deltaY);
                lastValue = (int)deltaY;
            }
        }
    }
}

void generateBitmap(JNIEnv* env, int orientation, jint width, jint height, float *fullPositions, jintArray colors, jintArray *result){
    jint *tmpColor = env->GetIntArrayElements(colors, 0);

    int* color0 = new int[3];
    color0[0] = *tmpColor >> 16 & 0xff;
    color0[1] = *tmpColor >> 8 & 0xff;
    color0[2] = *tmpColor & 0xff;

    int *color1 = new int[3];
    color1[0] = *(tmpColor + 1) >> 16 & 0xff;
    color1[1] = *(tmpColor + 1) >> 8 & 0xff;
    color1[2] = *(tmpColor + 1) & 0xff;

    int *color2 = new int[3];
    color2[0] = *(tmpColor + 2) >> 16 & 0xff;
    color2[1] = *(tmpColor + 2) >> 8 & 0xff;
    color2[2] = *(tmpColor + 2) & 0xff;

    int* tmpArray = new int[width * height];
    if(orientation == ORIENTATION_VERTICAL) {
        for(int j = 0; j < width; j++) {
            int centerPosition = ((float) height)  * fullPositions[j];
            for (int i = 0; i < height; i++) {
                int red, green, blue;
                if(i < centerPosition) {
                    float ratio = (float)i / (float)centerPosition;
                    red = color0[0] + ((float)(color1[0] - color0[0]) * ratio);
                    green = color0[1] + ((float)(color1[1] - color0[1]) * ratio);
                    blue = color0[2] + ((float)(color1[2] - color0[2]) * ratio);
                }
                else{
                    int secondHalf = height - centerPosition;
                    float ratio = (float)(i - centerPosition) / (float)secondHalf;
                    red = color1[0] + ((float)(color2[0] - color1[0]) * ratio);
                    green = color1[1] + ((float)(color2[1] - color1[1]) * ratio);
                    blue = color1[2] + ((float)(color2[2] - color1[2]) * ratio);
                }
                tmpArray[i * width + j] = 255 << 24 | (red << 16) | (green << 8) | blue;
            }
        }

    }
    else{
        for(int j = 0; j < height; j++) {
            int centerPosition = ((float) width)  * fullPositions[j];
            for (int i = 0; i < width; i++) {
                int red, green, blue;
                if(i < centerPosition) {
                    float ratio = (float)i / (float)centerPosition;
                    red = color0[0] + ((float)(color1[0] - color0[0]) * ratio);
                    green = color0[1] + ((float)(color1[1] - color0[1]) * ratio);
                    blue = color0[2] + ((float)(color1[2] - color0[2]) * ratio);
                }
                else{
                    int secondHalf = width - centerPosition;
                    float ratio = (float)(i - centerPosition) / (float)secondHalf;
                    red = color1[0] + ((float)(color2[0] - color1[0]) * ratio);
                    green = color1[1] + ((float)(color2[1] - color1[1]) * ratio);
                    blue = color1[2] + ((float)(color2[2] - color1[2]) * ratio);
                }
                tmpArray[j * width + i] = 255 << 24 | (red << 16) | (green << 8) | blue;
            }
        }
    }
    env->SetIntArrayRegion(*result, 0, width * height, tmpArray);
}

float* getSlop(int startIndex, list<int> *tmpX, list<int> *tmpY) {
    std::list<int>::iterator startX = (*tmpX).begin();
    std::list<int>::iterator startY = (*tmpY).begin();

    std::list<int>::iterator endX = (*tmpX).begin();
    std::list<int>::iterator endY = (*tmpY).begin();

    std::advance(startX, startIndex);
    std::advance(startY, startIndex);

    std::advance(endX, startIndex + 1);
    std::advance(endY, startIndex + 1);

    float* result = new float[5];
    result[0] = *startX;
    result[1] = *startY;
    result[2] = *endX;
    result[3] = *endY;
    result[4] = (float) (*endY - *startY) / (float)(*endX - *startX);
    return result;
}

void fetchupEndPoint(int orientation, int width, int height, int startIndex, list<int> *tmpX, list<int> *tmpY){
    float* result = getSlop(startIndex, tmpX, tmpY);
    int startX = result[0];
    int startY = result[1];
    int endX = result[2];
    int endY = result[3];
    float m = result[4];

    //fetchup start point
    if(startIndex == 0) {
        if(orientation == ORIENTATION_VERTICAL){
            float y = m * (- startX) + startY;
            (*tmpX).push_front(0);
            (*tmpY).push_front(y);
        }
        else{
            float x = ((- endY) / m) + endX;
            (*tmpX).push_front(x);
            (*tmpY).push_front(0);
        }
    }
    //fetchup end point
    else{
        if(orientation == ORIENTATION_VERTICAL){
            float y = m * (width - startX) + startY;
            (*tmpX).push_back(width);
            (*tmpY).push_back(y);
        }
        else{
            float x = ((height - endY) / m) + endX;
            (*tmpX).push_back(x);
            (*tmpY).push_back(height);
        }
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_mgcoco_gradientview_GradientView_setup(
        JNIEnv* env,
        jobject /* this */obj,
        jobjectArray controlPoints,
        jint orientation,
        jint width,
        jint height,
        jintArray colors,
        jintArray result) {

    try {
        list<int> tmpX;
        list<int> tmpY;

        int length = env->GetArrayLength(controlPoints);
        for (int i = 1; i < length; i++) {
            jobject cp1 = env->GetObjectArrayElement(controlPoints, i);
            jobject cp2 = env->GetObjectArrayElement(controlPoints, i - 1);
            if(i == 1){
                setupCurvPoint(env, orientation, cp1, cp2, cp2, width, height, &tmpX, &tmpY);
            }
            else {
                setupCurvPoint(env, orientation, cp1, cp2, env->GetObjectArrayElement(controlPoints, i - 2), width, height, &tmpX, &tmpY);
            }
        }

        if(tmpX.size() >= 2) {
            fetchupEndPoint(orientation, width, height, 0, &tmpX, &tmpY);
            fetchupEndPoint(orientation, width, height, tmpX.size() - 2, &tmpX, &tmpY);
        }

        float* colorPositions;
        if(orientation == ORIENTATION_VERTICAL){
            colorPositions = new float[width];
        }
        else{
            colorPositions = new float[height];
        }

        for (int i = 1; i < tmpX.size(); i++) {
            float* result = getSlop(i - 1, &tmpX, &tmpY);
            int startX = result[0];
            int startY = result[1];
            int endX = result[2];
            int endY = result[3];
            float m = result[4];

            if(orientation == ORIENTATION_VERTICAL){
                if(startX < width) {
                    int oldX = startX;
                    colorPositions[startX] = ((float) (startY) / height);

                    if (endX - startX > 1) {
                        for (int gi = startX + 1; gi < endX; gi++) {
                            float y = m * (gi - startX) + startY;
                            if (gi > oldX) {
                                colorPositions[gi] = y / height;
                                oldX = gi;
                            }
                        }
                    }
                }
            }
            else{
                if(startY < height) {
                    int oldY = startY;
                    colorPositions[startY] = ((float) (startX) / width);

                    if (endY - startY > 1) {
                        for (int gi = startY + 1; gi < endY; gi++) {
                            float x = ((gi - endY) / m) + endX;
                            if (gi > oldY && height > gi) {
                                colorPositions[gi] = x / width;
                                oldY = gi;
                            }
                        }
                    }
                }
            }
        }
        generateBitmap(env, orientation, width, height, colorPositions, colors, &result);
    }
    catch (char const* error) {
    }
}
