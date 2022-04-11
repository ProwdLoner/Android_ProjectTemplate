package com.example.prowd_android_template.util_object.easylut;

import com.example.prowd_android_template.util_object.easylut.filter.Filter;
import com.example.prowd_android_template.util_object.easylut.filter.FilterNon;
import com.example.prowd_android_template.util_object.easylut.filter.LutFilterFromBitmap;
import com.example.prowd_android_template.util_object.easylut.filter.LutFilterFromResource;

public class EasyLUT {

    public static LutFilterFromResource.Builder fromResourceId() {
        return new LutFilterFromResource.Builder();
    }

    public static LutFilterFromBitmap.Builder fromBitmap() {
        return new LutFilterFromBitmap.Builder();
    }

    public static Filter createNonFilter() {
        return new FilterNon();
    }

}
