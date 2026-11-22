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

    public void bind(MediaImageModel model) {
        if (model == null) return;

        aspectRatio = model.width() > 0 && model.height() > 0 ? (float) model.width() / model.height() : 1f;
        setContentDescription(model.altText() == null ? model.caption() : model.altText());

        String url = model.deliveryUrl();

        // যদি রিলেটিভ পাথ আসে (যেমন "/api/v1/media/..."), তবে বেস ডোমেইন যুক্ত করে নেবে
        if (url != null && url.startsWith("/")) {
            url = "http://192.168.0.107:8080" + url;
        }

        android.util.Log.d("MEDIA_DEBUG", "Loading Image URL: " + url);

        if (url != null && !url.isEmpty()) {
            Glide.with(this)
                    .load(url)
                    .centerInside()
                    .into(this);
        }
    }


    public float getAspectRatio(){return aspectRatio;}
}
