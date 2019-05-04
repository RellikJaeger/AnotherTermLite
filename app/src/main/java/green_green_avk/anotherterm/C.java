package green_green_avk.anotherterm;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public final class C {

    private C() {
    }

    public static final String JAVA_PKG_NAME = C.class.getName().replaceFirst("\\..*?$", "");
    public static final String APP_ID = BuildConfig.APPLICATION_ID;
    public static final String IFK_MSG_NEW = BuildConfig.APPLICATION_ID + ".MSG_NEW";
    public static final String IFK_MSG_NAME = BuildConfig.APPLICATION_ID + ".MSG_NAME";
    public static final String IFK_MSG_SESS_KEY = BuildConfig.APPLICATION_ID + ".MSG_SESS_KEY";
    public static final String IFK_MSG_SESS_TAIL = BuildConfig.APPLICATION_ID + ".MSG_SESS_TAIL";
    public static final String IFK_MSG_ID = BuildConfig.APPLICATION_ID + ".MSG_ID";
    public static final String IFK_MSG_INTENT = BuildConfig.APPLICATION_ID + ".MSG_INTENT";
    public static final String IFK_ACTION_NEW = BuildConfig.APPLICATION_ID + ".ACTION_NEW";
    public static final String IFK_ACTION_CANCEL = BuildConfig.APPLICATION_ID + ".ACTION_CANCEL";
    public static final String TERMSH_USER_TAG = "TERMSH_USER";
    public static final String REQUEST_USER_TAG = "REQUEST_USER";
    public static final String UNNAMED_FILE_NAME = "unnamed";
    public static final List<String> charsetList = new ArrayList<>(Charset.availableCharsets().keySet());
}
