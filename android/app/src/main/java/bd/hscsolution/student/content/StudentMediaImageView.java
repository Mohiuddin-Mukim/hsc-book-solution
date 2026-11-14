package bd.hscsolution.student.content;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import com.bumptech.glide.Glide;

/**
 * Reusable student-reader image contract:
 * - loads only the server delivery route returned by the content API
 * - preserves the server-provided aspect ratio
 * - supports tap-to-zoom via the host screen
 * - never needs storage credentials
 */
public class StudentMediaImageView extends androidx.appcompat.widget.AppCompatImageView {
    private float aspectRatio = 1f;

    public StudentMediaImageView(Context context){super(context);}
    public StudentMediaImageView(Context context,@Nullable AttributeSet attrs){super(context,attrs);}
    public StudentMediaImageView(Context context,@Nullable AttributeSet attrs,int defStyleAttr){super(context,attrs,defStyleAttr);}

    public void bind(MediaImageModel model){
        aspectRatio = model.width() > 0 && model.height() > 0 ? (float)model.width()/model.height() : 1f;
        setContentDescription(model.altText()==null?model.caption():model.altText());
        Glide.with(this).load(model.deliveryUrl()).centerInside().into(this);
    }

    public float getAspectRatio(){return aspectRatio;}
}
