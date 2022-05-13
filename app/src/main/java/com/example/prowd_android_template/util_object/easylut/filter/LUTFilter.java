package com.example.prowd_android_template.util_object.easylut.filter;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.widget.ImageView;
import com.example.prowd_android_template.util_object.easylut.lutimage.CoordinateToColor;
import com.example.prowd_android_template.util_object.easylut.lutimage.LUTImage;
import com.example.prowd_android_template.util_object.easylut.lutimage.LutAlignment;

public abstract class LUTFilter implements Filter {

    private final BitmapStrategy strategy;
    private final CoordinateToColor.Type coordinateToColorType;
    private final LutAlignment.Mode lutAlignmentMode;

    protected LUTFilter(BitmapStrategy strategy,
                        CoordinateToColor.Type coordinateToColorType,
                        LutAlignment.Mode lutAlignmentMode) {
        this.strategy = strategy;
        this.coordinateToColorType = coordinateToColorType;
        this.lutAlignmentMode = lutAlignmentMode;
    }

    @Override
    public Bitmap apply(Bitmap src) {
        Bitmap lutBitmap = getLUTBitmap();
        LUTImage lutImage = LUTImage.createLutImage(lutBitmap, coordinateToColorType, lutAlignmentMode);
        return strategy.applyLut(src, lutImage);
    }

    protected abstract Bitmap getLUTBitmap();

    @Override
    public void apply(ImageView imageView) {
        BitmapDrawable imageDrawable = (BitmapDrawable) imageView.getDrawable();
        Bitmap source = imageDrawable.getBitmap();
        Bitmap bitmap = apply(source);
        imageView.setImageBitmap(bitmap);
    }

    public static abstract class Builder<B> {
        protected BitmapStrategy strategy = new CreatingNewBitmap();
        protected CoordinateToColor.Type coordinateToColorType = CoordinateToColor.Type.GUESS_AXES;
        protected LutAlignment.Mode lutAlignmentMode = LutAlignment.Mode.SQUARE;

        public B withStrategy(BitmapStrategy.Type strategy) {
            switch (strategy) {
                case APPLY_ON_ORIGINAL_BITMAP:
                    this.strategy = new ApplyOnOriginal();
                    break;
                case CREATING_NEW_BITMAP:
                    this.strategy = new CreatingNewBitmap();
                    break;
            }
            return self();
        }

        public B withColorAxes(CoordinateToColor.Type coordinateToColorType) {
            this.coordinateToColorType = coordinateToColorType;
            return self();
        }

        public B withAlignmentMode(LutAlignment.Mode lutAlignmentMode) {
            this.lutAlignmentMode = lutAlignmentMode;
            return self();
        }

        protected abstract B self();
    }
}
