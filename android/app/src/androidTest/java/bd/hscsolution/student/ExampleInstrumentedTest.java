package bd.hscsolution.student;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertEquals;
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
 @Test public void appContext_hasExpectedPackageName() {
  assertEquals("bd.hscsolution.student", androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getTargetContext().getPackageName());
 }
}
