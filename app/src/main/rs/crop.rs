#pragma version(1)
#pragma rs java_package_name(com.xxx.yyy)
#pragma rs_fp_relaxed

int32_t width;
int32_t height;

rs_allocation croppedImg;
uint xStart, yStart;

void __attribute__((kernel)) doCrop(uchar4 in,uint32_t x, uint32_t y) {
    rsSetElementAt_uchar4(croppedImg,in, x-xStart, y-yStart);
}