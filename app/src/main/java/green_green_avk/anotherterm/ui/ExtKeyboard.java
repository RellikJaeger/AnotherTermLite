package green_green_avk.anotherterm.ui;
/*
 * Copyright (C) 2008-2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

/* Changed by Aleksandr Kiselev */

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.Keyboard;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.XmlRes;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import green_green_avk.anotherterm.R;

/**
 * Loads an XML description of a keyboard and stores the attributes of the keys. A keyboard
 * consists of rows of keys. Unlike the {@link Keyboard} class from Android SDK, this version stores only
 * static UI description: no current user interaction state.
 */
public class ExtKeyboard {

    static final String TAG = "Keyboard";

    // Keyboard XML Tags
    private static final String TAG_KEYBOARD = "Keyboard";
    private static final String TAG_ROW = "Row";
    private static final String TAG_KEY = "Key";

    public static final int EDGE_LEFT = 0x01;
    public static final int EDGE_RIGHT = 0x02;
    public static final int EDGE_TOP = 0x04;
    public static final int EDGE_BOTTOM = 0x08;

    public static final int KEYCODE_NONE = 0;

    /**
     * Keyboard label
     */
//    private CharSequence mLabel;

    /**
     * Horizontal gap default for all rows
     */
    private int mDefaultHorizontalGap;

    /**
     * Default key width
     */
    private int mDefaultWidth;

    /**
     * Default key height
     */
    private int mDefaultHeight;

    /**
     * Default gap between rows
     */
    private int mDefaultVerticalGap;

    /**
     * Current key width, while loading the keyboard
     */
//    private int mKeyWidth;

    /**
     * Current key height, while loading the keyboard
     */
//    private int mKeyHeight;

    /**
     * Total height of the keyboard, including the padding and keys
     */
    private int mTotalHeight;

    /**
     * Total width of the keyboard, including left side gaps and keys, but not any gaps on the
     * right side.
     */
    private int mTotalWidth;

    /**
     * List of keys in this keyboard
     */
    private final List<Key> mKeys = new ArrayList<>();

    /**
     * Keys by code
     */
    private final SparseArray<Set<Key>> mKeysByCode = new SparseArray<>();

    /**
     * Width of the screen available to fit the keyboard
     */
    private int mDisplayWidth;

    /**
     * Height of the screen
     */
    private int mDisplayHeight;

    /**
     * Keyboard mode, or zero, if none.
     */
    private int mKeyboardMode;

    // Variables for pre-computing nearest keys.

    private static final int GRID_WIDTH = 10;
    private static final int GRID_HEIGHT = 5;
    private static final int GRID_SIZE = GRID_WIDTH * GRID_HEIGHT;
    private int mCellWidth;
    private int mCellHeight;
    private int[][] mGridNeighbors;
    private int mProximityThreshold;
    /**
     * Number of key widths from current touch point to search for nearest keys.
     */
    private static float SEARCH_DISTANCE = 1.8f;

    private final ArrayList<Row> rows = new ArrayList<>();

    /**
     * Container for keys in the keyboard. All keys in a row are at near the same Y-coordinate.
     */
    public static class Row {
        /**
         * Default width of a key in this row.
         */
        public int defaultWidth;
        /**
         * Default height of a key in this row.
         */
        public int defaultHeight;
        /**
         * Default horizontal gap between keys in this row.
         */
        public int defaultHorizontalGap;
        /**
         * Vertical gap following this row.
         */
        public int verticalGap;

        final ArrayList<Key> mKeys = new ArrayList<>();

        /**
         * Edge flags for this row of keys. Possible values that can be assigned are
         * {@link ExtKeyboard#EDGE_TOP EDGE_TOP} and {@link ExtKeyboard#EDGE_BOTTOM EDGE_BOTTOM}
         */
        public int rowEdgeFlags;

        /**
         * The keyboard mode for this row
         */
        public int mode;

        private ExtKeyboard parent;

        public Row(ExtKeyboard parent) {
            this.parent = parent;
        }

        public Row(Resources res, ExtKeyboard parent, XmlResourceParser parser) {
            this.parent = parent;
            TypedArray a = res.obtainAttributes(Xml.asAttributeSet(parser),
                    R.styleable.ExtKeyboard);
            defaultWidth = getDimensionOrFraction(a,
                    R.styleable.ExtKeyboard_keyWidth,
                    parent.mDisplayWidth, parent.mDefaultWidth);
            defaultHeight = getDimensionOrFraction(a,
                    R.styleable.ExtKeyboard_keyHeight,
                    parent.mDisplayHeight, parent.mDefaultHeight);
            defaultHorizontalGap = getDimensionOrFraction(a,
                    R.styleable.ExtKeyboard_horizontalGap,
                    parent.mDisplayWidth, parent.mDefaultHorizontalGap);
            verticalGap = getDimensionOrFraction(a,
                    R.styleable.ExtKeyboard_verticalGap,
                    parent.mDisplayHeight, parent.mDefaultVerticalGap);
            a.recycle();
            a = res.obtainAttributes(Xml.asAttributeSet(parser),
                    R.styleable.ExtKeyboard_Row);
            rowEdgeFlags = a.getInt(R.styleable.ExtKeyboard_Row_rowEdgeFlags, 0);
            mode = a.getResourceId(R.styleable.ExtKeyboard_Row_keyboardMode,
                    0);
            a.recycle();
        }
    }

    /**
     * Class for describing the position and characteristics of a single key in the keyboard.
     */
    public static class Key {
        /**
         * All the key codes (unicode or custom code) and labels
         * that this key could generate, zero'th
         * being the most important.
         */
        public final List<KeyFcn> functions = new ArrayList<>();
        /**
         * Width of the key, not including the gap
         */
        public int width;
        /**
         * Height of the key, not including the gap
         */
        public int height;
        /**
         * The horizontal gap before this key
         */
        public int gap;
        /**
         * Whether this key is sticky, i.e., a toggle key
         */
        public boolean sticky;
        /**
         * X coordinate of the key in the keyboard layout
         */
        public int x;
        /**
         * Y coordinate of the key in the keyboard layout
         */
        public int y;

        /**
         * Flags that specify the anchoring to edges of the keyboard for detecting touch events
         * that are just out of the boundary of the key. This is a bit mask of
         * {@link ExtKeyboard#EDGE_LEFT}, {@link ExtKeyboard#EDGE_RIGHT}, {@link ExtKeyboard#EDGE_TOP} and
         * {@link ExtKeyboard#EDGE_BOTTOM}.
         */
        public int edgeFlags;
        /**
         * Whether this is a modifier key, such as Shift or Alt
         */
        public boolean modifier;
        /**
         * The keyboard that this key belongs to
         */
        private ExtKeyboard keyboard;
        /**
         * If this key pops up a mini keyboard, this is the resource id for the XML layout for that
         * keyboard.
         */
        public int popupResId;
        /**
         * Whether this key repeats itself when held down
         */
        public boolean repeatable;

        public boolean showBothLabels = false;

        private final static int[] KEY_STATE_NORMAL_ON = {
                android.R.attr.state_checkable,
                android.R.attr.state_checked
        };

        private final static int[] KEY_STATE_PRESSED_ON = {
                android.R.attr.state_pressed,
                android.R.attr.state_checkable,
                android.R.attr.state_checked
        };

        private final static int[] KEY_STATE_NORMAL_OFF = {
                android.R.attr.state_checkable
        };

        private final static int[] KEY_STATE_PRESSED_OFF = {
                android.R.attr.state_pressed,
                android.R.attr.state_checkable
        };

        private final static int[] KEY_STATE_NORMAL = {
        };

        private final static int[] KEY_STATE_PRESSED = {
                android.R.attr.state_pressed
        };

        private final static int[][] KEY_STATES = {
                {},
                {android.R.attr.state_pressed},
                {android.R.attr.state_checked},
                {android.R.attr.state_checked, android.R.attr.state_pressed}
        };

        public static int[] getKeyState(boolean pressed, boolean on) {
            return KEY_STATES[(pressed ? 1 : 0) | (on ? 2 : 0)];
        }

        /**
         * Create an empty key with no attributes.
         */
        public Key(Row parent) {
            keyboard = parent.parent;
            height = parent.defaultHeight;
            width = parent.defaultWidth;
            gap = parent.defaultHorizontalGap;
            edgeFlags = parent.rowEdgeFlags;
        }

        /**
         * Create a key with the given top-left coordinate and extract its attributes from
         * the XML parser.
         *
         * @param res    resources associated with the caller's context
         * @param parent the row that this key belongs to. The row must already be attached to
         *               a {@link ExtKeyboard}.
         * @param x      the x coordinate of the top-left
         * @param y      the y coordinate of the top-left
         * @param parser the XML parser containing the attributes for this key
         */
        public Key(Resources res, Row parent, int x, int y, XmlResourceParser parser) {
            this(parent);

            this.x = x;
            this.y = y;

            TypedArray a = res.obtainAttributes(Xml.asAttributeSet(parser),
                    R.styleable.ExtKeyboard);
            try {
                width = getDimensionOrFraction(a,
                        R.styleable.ExtKeyboard_keyWidth,
                        keyboard.mDisplayWidth, parent.defaultWidth);
                height = getDimensionOrFraction(a,
                        R.styleable.ExtKeyboard_keyHeight,
                        keyboard.mDisplayHeight, parent.defaultHeight);
                gap = getDimensionOrFraction(a,
                        R.styleable.ExtKeyboard_horizontalGap,
                        keyboard.mDisplayWidth, parent.defaultHorizontalGap);
            } finally {
                a.recycle();
            }

            a = res.obtainAttributes(Xml.asAttributeSet(parser),
                    R.styleable.ExtKeyboard_Key);
            try {
                this.x += gap;

                int code = a.getInt(
                        R.styleable.ExtKeyboard_Key_code, KEYCODE_NONE);
                popupResId = a.getResourceId(
                        R.styleable.ExtKeyboard_Key_popupKeyboard, 0);
                modifier = a.getBoolean(
                        R.styleable.ExtKeyboard_Key_isModifier, false);
                repeatable = a.getBoolean(
                        R.styleable.ExtKeyboard_Key_isRepeatable, !modifier);
                sticky = a.getBoolean(
                        R.styleable.ExtKeyboard_Key_isSticky, false);
                edgeFlags = a.getInt(R.styleable.ExtKeyboard_Key_keyEdgeFlags, 0);
                edgeFlags |= parent.rowEdgeFlags;

                Drawable icon = a.getDrawable(
                        R.styleable.ExtKeyboard_Key_keyIcon);
                if (icon != null) {
                    icon.setBounds(0, 0, icon.getIntrinsicWidth(), icon.getIntrinsicHeight());
                }
                CharSequence label = a.getText(R.styleable.ExtKeyboard_Key_keyLabel);
                CharSequence text = a.getText(R.styleable.ExtKeyboard_Key_keyOutputText);

                if (code == KEYCODE_NONE && label != null) {
                    for (int i = 0; i < label.length(); ++i) {
                        final KeyFcn fcn = new KeyFcn();
                        fcn.code = -label.charAt(i);
                        fcn.label = Character.toString(label.charAt(i));
                        functions.add(fcn);
                    }
                } else {
                    final KeyFcn fcn = new KeyFcn();
                    fcn.code = code;
                    fcn.label = label;
                    fcn.icon = icon;
                    fcn.text = text;
                    functions.add(fcn);
                }

                showBothLabels = a.getBoolean(R.styleable.ExtKeyboard_Key_showBothLabels,
                        functions.size() >= 2 &&
                                functions.get(0).label.toString().compareToIgnoreCase(functions.get(1).label.toString()) != 0);
            } finally {
                a.recycle();
            }
        }
    }

    public static class KeyFcn {
        /**
         * Key code (unicode or custom code) that this key will generate
         */
        public int code = KEYCODE_NONE;
        /**
         * Label to display
         */
        public CharSequence label = null;
        /**
         * Icon to display instead of a label. Icon takes precedence over a label
         */
        public Drawable icon = null;
        /**
         * Text to output when pressed. This can be multiple characters, like ".com"
         */
        public CharSequence text = null;
    }

    /**
     * Creates a keyboard from the given xml key layout file.
     *
     * @param context        the application or service context
     * @param xmlLayoutResId the resource file that contains the keyboard layout and keys.
     */
    public ExtKeyboard(Context context, @XmlRes int xmlLayoutResId) {
        this(context, xmlLayoutResId, 0);
    }

    /**
     * Creates a keyboard from the given xml key layout file. Weeds out rows
     * that have a keyboard mode defined but don't match the specified mode.
     *
     * @param context        the application or service context
     * @param xmlLayoutResId the resource file that contains the keyboard layout and keys.
     * @param modeId         keyboard mode identifier
     * @param width          sets width of keyboard
     * @param height         sets height of keyboard
     */
    public ExtKeyboard(Context context, @XmlRes int xmlLayoutResId, int modeId, int width,
                       int height) {
        mDisplayWidth = width;
        mDisplayHeight = height;

        mDefaultHorizontalGap = 0;
        mDefaultWidth = mDisplayWidth / 10;
        mDefaultVerticalGap = 0;
        mDefaultHeight = mDefaultWidth;
        mKeyboardMode = modeId;
        loadKeyboard(context, context.getResources().getXml(xmlLayoutResId));
        keyMap.refresh(context);
    }

    /**
     * Creates a keyboard from the given xml key layout file. Weeds out rows
     * that have a keyboard mode defined but don't match the specified mode.
     *
     * @param context        the application or service context
     * @param xmlLayoutResId the resource file that contains the keyboard layout and keys.
     * @param modeId         keyboard mode identifier
     */
    public ExtKeyboard(Context context, @XmlRes int xmlLayoutResId, int modeId) {
        final DisplayMetrics dm = context.getResources().getDisplayMetrics();
        mDisplayWidth = dm.widthPixels;
        mDisplayHeight = dm.heightPixels;
        //Log.v(TAG, "keyboard's display metrics:" + dm);

        mDefaultHorizontalGap = 0;
        mDefaultWidth = mDisplayWidth / 10;
        mDefaultVerticalGap = 0;
        mDefaultHeight = mDefaultWidth;
        mKeyboardMode = modeId;
        loadKeyboard(context, context.getResources().getXml(xmlLayoutResId));
        keyMap.refresh(context);
    }

    /**
     * <p>Creates a blank keyboard from the given resource file and populates it with the specified
     * characters in left-to-right, top-to-bottom fashion, using the specified number of columns.
     * </p>
     * <p>If the specified number of columns is -1, then the keyboard will fit as many keys as
     * possible in each row.</p>
     *
     * @param context             the application or service context
     * @param layoutTemplateResId the layout template file, containing no keys.
     * @param characters          the list of characters to display on the keyboard. One key will be created
     *                            for each character.
     * @param columns             the number of columns of keys to display. If this number is greater than the
     *                            number of keys that can fit in a row, it will be ignored. If this number is -1, the
     *                            keyboard will fit as many keys as possible in each row.
     */
    public ExtKeyboard(Context context, @XmlRes int layoutTemplateResId,
                       CharSequence characters, int columns, int horizontalPadding) {
        this(context, layoutTemplateResId);
        int x = 0;
        int y = 0;
        int column = 0;
        mTotalWidth = 0;

        final Row row = new Row(this);
        row.defaultHeight = mDefaultHeight;
        row.defaultWidth = mDefaultWidth;
        row.defaultHorizontalGap = mDefaultHorizontalGap;
        row.verticalGap = mDefaultVerticalGap;
        row.rowEdgeFlags = EDGE_TOP | EDGE_BOTTOM;
        final int maxColumns = columns == -1 ? Integer.MAX_VALUE : columns;
        for (int i = 0; i < characters.length(); i++) {
            char c = characters.charAt(i);
            if (column >= maxColumns
                    || x + mDefaultWidth + horizontalPadding > mDisplayWidth) {
                x = 0;
                y += mDefaultVerticalGap + mDefaultHeight;
                column = 0;
            }
            final Key key = new Key(row);
            key.x = x;
            key.y = y;
            final KeyFcn fcn = new KeyFcn();
            fcn.label = String.valueOf(c);
            fcn.code = -c;
            key.functions.add(fcn);
            column++;
            x += key.width + key.gap;
            mKeys.add(key);
            row.mKeys.add(key);
            addKeyByCode(key);
            if (x > mTotalWidth) {
                mTotalWidth = x;
            }
        }
        mTotalHeight = y + mDefaultHeight;
        rows.add(row);
        keyMap.refresh(context);
    }

    final void resize(@NonNull final Context context, int newWidth, int newHeight) {
        final int numRows = rows.size();
        for (int rowIndex = 0; rowIndex < numRows; ++rowIndex) {
            final Row row = rows.get(rowIndex);
            final int numKeys = row.mKeys.size();
            int totalGap = 0;
            int totalWidth = 0;
            for (int keyIndex = 0; keyIndex < numKeys; ++keyIndex) {
                final Key key = row.mKeys.get(keyIndex);
                if (keyIndex > 0) {
                    totalGap += key.gap;
                }
                totalWidth += key.width;
            }
            if (totalGap + totalWidth > newWidth) {
                int x = 0;
                float scaleFactor = (float) (newWidth - totalGap) / totalWidth;
                for (int keyIndex = 0; keyIndex < numKeys; ++keyIndex) {
                    Key key = row.mKeys.get(keyIndex);
                    key.width *= scaleFactor;
                    key.x = x;
                    x += key.width + key.gap;
                }
            }
        }
        mTotalWidth = newWidth;
        // TODO: This does not adjust the vertical placement according to the new size.
        // The main problem in the previous code was horizontal placement/size, but we should
        // also recalculate the vertical sizes/positions when we get this resize call.
        keyMap.refresh(context);
    }

    public List<Key> getKeys() {
        return mKeys;
    }

    public Set<Key> getKeysByCode(int code) {
        return mKeysByCode.get(code);
    }

    protected int getHorizontalGap() {
        return mDefaultHorizontalGap;
    }

    protected void setHorizontalGap(int gap) {
        mDefaultHorizontalGap = gap;
    }

    protected int getVerticalGap() {
        return mDefaultVerticalGap;
    }

    protected void setVerticalGap(int gap) {
        mDefaultVerticalGap = gap;
    }

    protected int getKeyHeight() {
        return mDefaultHeight;
    }

    protected void setKeyHeight(int height) {
        mDefaultHeight = height;
    }

    protected int getKeyWidth() {
        return mDefaultWidth;
    }

    protected void setKeyWidth(int width) {
        mDefaultWidth = width;
    }

    /**
     * Returns the total height of the keyboard
     *
     * @return the total height of the keyboard
     */
    public int getHeight() {
        return mTotalHeight;
    }

    public int getMinWidth() {
        return mTotalWidth;
    }

    /* Fast but unusual way to implement it */
    protected final class KeyMap {
        private final float dpi = 40;
        private Bitmap bmp = null;
        private int width = 0;
        private int height = 0;
        private Canvas canvas = null;
        private final Paint paint = new Paint();

        {
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
        }

        public void refresh(@NonNull final Context ctx) {
            final DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
            if (bmp == null || width != mTotalWidth || height != mTotalHeight) {
                width = mTotalWidth;
                height = mTotalHeight;
                // https://issuetracker.google.com/issues/36940792
                // https://stackoverflow.com/questions/9247369/alpha-8-bitmaps-and-getpixel
                // https://android.googlesource.com/platform/frameworks/base/+/6260b22501996d2e7a0323b493ae6c4badb93c28%5E%21/core/jni/android/graphics/Bitmap.cpp
                // TODO: Or copyPixelsToBuffer() solution is better?
                bmp = Bitmap.createBitmap(
                        (int) (width * dpi / dm.xdpi),
                        (int) (height * dpi / dm.ydpi),
                        Build.VERSION.SDK_INT < 23 ? Bitmap.Config.ARGB_8888 : Bitmap.Config.ALPHA_8
                );
                canvas = new Canvas(bmp);
                canvas.scale((float) bmp.getWidth() / width, (float) bmp.getHeight() / height);
            }
            canvas.drawColor(0xFFFFFFFF);
            for (int i = 0; i < mKeys.size(); ++i) {
                Key k = mKeys.get(i);
                paint.setAlpha(i);
                canvas.drawRect(k.x, k.y, k.x + k.width, k.y + k.height, paint);
            }
        }

        @Nullable
        public Key get(final int x, final int y) {
            if (x < 0 || y < 0 || x >= width || y >= height) return null;
            final int i = Color.alpha(bmp.getPixel(
                    x * bmp.getWidth() / width,
                    y * bmp.getHeight() / height
            ));
            if (i >= mKeys.size()) return null;
            return mKeys.get(i);
        }
    }

    protected final KeyMap keyMap = new KeyMap();

    @Nullable
    public Key getKey(final int x, final int y) {
        return keyMap.get(x, y);
    }

    private void addKeyByCode(Key key) {
        if (!key.modifier) return;
        for (KeyFcn fcn : key.functions) {
            if (fcn.code == KEYCODE_NONE) continue;
            Set<Key> keys = mKeysByCode.get(fcn.code);
            if (keys == null) {
                keys = new HashSet<>();
                mKeysByCode.append(fcn.code, keys);
            }
            keys.add(key);
        }
    }

    protected Row createRowFromXml(Resources res, XmlResourceParser parser) {
        return new Row(res, this, parser);
    }

    protected Key createKeyFromXml(Resources res, Row parent, int x, int y,
                                   XmlResourceParser parser) {
        return new Key(res, parent, x, y, parser);
    }

    private void loadKeyboard(Context context, XmlResourceParser parser) {
        boolean inKey = false;
        boolean inRow = false;
        int x = 0;
        int y = 0;
        Key key = null;
        Row currentRow = null;
        Resources res = context.getApplicationContext().getResources();

        try {
            int event;
            while ((event = parser.next()) != XmlResourceParser.END_DOCUMENT) {
                if (event == XmlResourceParser.START_TAG) {
                    String tag = parser.getName();
                    if (TAG_ROW.equals(tag)) {
                        inRow = true;
                        x = 0;
                        currentRow = createRowFromXml(res, parser);
                        rows.add(currentRow);
                        if (currentRow.mode != 0 && currentRow.mode != mKeyboardMode) {
                            skipToEndOfRow(parser);
                            inRow = false;
                        }
                    } else if (TAG_KEY.equals(tag)) {
                        inKey = true;
                        key = createKeyFromXml(res, currentRow, x, y, parser);
                        mKeys.add(key);
                        currentRow.mKeys.add(key);
                        addKeyByCode(key);
                    } else if (TAG_KEYBOARD.equals(tag)) {
                        parseKeyboardAttributes(res, parser);
                    }
                } else if (event == XmlResourceParser.END_TAG) {
                    if (inKey) {
                        inKey = false;
                        x += key.gap + key.width;
                        if (x > mTotalWidth) {
                            mTotalWidth = x;
                        }
                    } else if (inRow) {
                        inRow = false;
                        y += currentRow.verticalGap;
                        y += currentRow.defaultHeight;
                    } else {
                        // TODO: error or extend?
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Parse error:" + e);
            e.printStackTrace();
        }
        mTotalHeight = y - mDefaultVerticalGap;
    }

    private void skipToEndOfRow(XmlResourceParser parser)
            throws XmlPullParserException, IOException {
        int event;
        while ((event = parser.next()) != XmlResourceParser.END_DOCUMENT) {
            if (event == XmlResourceParser.END_TAG
                    && parser.getName().equals(TAG_ROW)) {
                break;
            }
        }
    }

    private void parseKeyboardAttributes(Resources res, XmlResourceParser parser) {
        TypedArray a = res.obtainAttributes(Xml.asAttributeSet(parser),
                R.styleable.ExtKeyboard);

        mDefaultWidth = getDimensionOrFraction(a,
                R.styleable.ExtKeyboard_keyWidth,
                mDisplayWidth, mDisplayWidth / 10);
        mDefaultHeight = getDimensionOrFraction(a,
                R.styleable.ExtKeyboard_keyHeight,
                mDisplayHeight, 50);
        mDefaultHorizontalGap = getDimensionOrFraction(a,
                R.styleable.ExtKeyboard_horizontalGap,
                mDisplayWidth, 0);
        mDefaultVerticalGap = getDimensionOrFraction(a,
                R.styleable.ExtKeyboard_verticalGap,
                mDisplayHeight, 0);
        mProximityThreshold = (int) (mDefaultWidth * SEARCH_DISTANCE);
        mProximityThreshold = mProximityThreshold * mProximityThreshold; // Square it for comparison
        a.recycle();
    }

    static int getDimensionOrFraction(TypedArray a, int index, int base, int defValue) {
        TypedValue value = a.peekValue(index);
        if (value == null) return defValue;
        if (value.type == TypedValue.TYPE_DIMENSION) {
            return a.getDimensionPixelOffset(index, defValue);
        } else if (value.type == TypedValue.TYPE_FRACTION) {
            // Round it to avoid values like 47.9999 from getting truncated
            return Math.round(a.getFraction(index, base, base, defValue));
        }
        return defValue;
    }
}
