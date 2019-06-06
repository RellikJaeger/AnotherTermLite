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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.KeyboardView;
import android.os.Build;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pools;
import android.support.v7.content.res.AppCompatResources;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;

import java.util.HashSet;
import java.util.Set;

import green_green_avk.anotherterm.R;
import green_green_avk.anotherterm.utils.WeakHandler;

/**
 * A view that renders a screen {@link ExtKeyboard}.
 * It handles rendering of keys and detecting key presses and touch movements.
 */
public abstract class ExtKeyboardView extends View /*implements View.OnClickListener*/ {

    /* To be overridden */
    public boolean getAutoRepeat() {
        return true;
    }

    /* We cannot use the View visibility property here
    because invisible View cannot receive focus and thus hardware keyboard events */
    protected boolean mHidden = false;
    protected ExtKeyboard mKeyboard = null;

    protected Typeface[] typefaces = {
            Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL),
            Typeface.create(Typeface.MONOSPACE, Typeface.BOLD),
            Typeface.create(Typeface.MONOSPACE, Typeface.ITALIC),
            Typeface.create(Typeface.MONOSPACE, Typeface.BOLD_ITALIC)
    };

    protected final Paint mPaint = new Paint();
    protected final Rect mKeyPadding = new Rect(0, 0, 0, 0);
    protected final Rect mLedPadding = new Rect(0, 0, 0, 0);

    protected boolean mAutoRepeatAllowed = true; // widget own option
    protected int mAutoRepeatDelay;
    protected int mAutoRepeatInterval;
    protected int mPopupDelay;
    protected int mPopupKeySize;
    protected ColorStateList mPopupKeyTextColor;
    protected int mPopupShadowColor;
    protected float mPopupShadowRadius;

    protected int mLabelTextSize;
    protected int mKeyTextSize;
    protected int mKeyTextColor;
    protected float mShadowRadius;
    protected int mShadowColor;
    protected Drawable mKeyBackground;
    protected Drawable mLedBackground;
    protected Drawable mPopupBackground;
    protected Drawable mPopupKeyBackground;

    protected int mVerticalCorrection;

    /**
     * Listener for {@link KeyboardView.OnKeyboardActionListener}.
     */
    protected KeyboardView.OnKeyboardActionListener mKeyboardActionListener = null;

    /**
     * The dirty region in the keyboard bitmap
     */
    protected Rect mDirtyRect = new Rect();
    /**
     * The keyboard bitmap for faster updates
     */
    protected Bitmap mBuffer = null;
    /**
     * The canvas for the above mutable keyboard bitmap
     */
    protected Canvas mCanvas;

    /**
     * Selected shifted state
     */
    protected int mAltKeysFcn = 0;

    /**
     * LEDs control
     */
//    protected final SparseBooleanArray leds = new SparseBooleanArray();
    protected final Set<Integer> leds = new HashSet<>();

    protected final KeyTouchMap mTouchedKeys = new KeyTouchMap();

    protected static final int MSG_REPEAT = 1;

    protected WeakHandler mHandler = new WeakHandler();

    public ExtKeyboardView(final Context context, final AttributeSet attrs) {
        this(context, attrs, R.attr.extKeyboardViewStyle);
    }

    public ExtKeyboardView(final Context context, final AttributeSet attrs,
                           final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, R.style.AppExtKeyboardViewStyle);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ExtKeyboardView(final Context context, final AttributeSet attrs,
                           final int defStyleAttr, final int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(final Context context, final AttributeSet attrs,
                      final int defStyleAttr, final int defStyleRes) {
        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.ExtKeyboardView, defStyleAttr, defStyleRes);
        try {
//            mKeyBackground = a.getDrawable(R.styleable.ExtKeyboardView_keyBackground);
            mKeyBackground = AppCompatResources.getDrawable(context,
                    a.getResourceId(R.styleable.ExtKeyboardView_keyBackground, 0));
            mLedBackground = AppCompatResources.getDrawable(context,
                    a.getResourceId(R.styleable.ExtKeyboardView_ledBackground, 0));
            mVerticalCorrection = a.getDimensionPixelOffset(R.styleable.ExtKeyboardView_verticalCorrection, 0);
            mKeyTextSize = a.getDimensionPixelSize(R.styleable.ExtKeyboardView_keyTextSize, 18);
            mKeyTextColor = a.getColor(R.styleable.ExtKeyboardView_keyTextColor, 0xFF000000);
            mLabelTextSize = a.getDimensionPixelSize(R.styleable.ExtKeyboardView_labelTextSize, 14);
            mShadowColor = a.getColor(R.styleable.ExtKeyboardView_shadowColor, 0);
            mShadowRadius = a.getFloat(R.styleable.ExtKeyboardView_shadowRadius, 0f);
            mAutoRepeatDelay = a.getInteger(R.styleable.ExtKeyboardView_autoRepeatDelay, 1000);
            mAutoRepeatInterval = a.getInteger(R.styleable.ExtKeyboardView_autoRepeatInterval, 100);
//            mPopupBackground = a.getDrawable(R.styleable.ExtKeyboardView_popupBackground);
            mPopupBackground = AppCompatResources.getDrawable(context,
                    a.getResourceId(R.styleable.ExtKeyboardView_popupBackground, 0));
            mPopupDelay = a.getInteger(R.styleable.ExtKeyboardView_popupDelay, 100);
            mPopupKeyBackground = AppCompatResources.getDrawable(context,
                    a.getResourceId(R.styleable.ExtKeyboardView_popupKeyBackground, 0));
            mPopupKeySize = a.getDimensionPixelSize(R.styleable.ExtKeyboardView_popupKeySize, 24);
            mPopupKeyTextColor = a.getColorStateList(R.styleable.ExtKeyboardView_popupKeyTextColor);
            mPopupShadowColor = a.getColor(R.styleable.ExtKeyboardView_popupShadowColor, 0);
            mPopupShadowRadius = a.getFloat(R.styleable.ExtKeyboardView_popupShadowRadius, 0f);
        } finally {
            a.recycle();
        }

        mPaint.setAntiAlias(true);
        mPaint.setColor(mKeyTextColor);
        mPaint.setTextSize(mKeyTextSize);
        mPaint.setTextAlign(Align.CENTER);
        mPaint.setAlpha(255);

        mKeyBackground.getPadding(mKeyPadding);
        mLedBackground.getPadding(mLedPadding);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mHandler = new WeakHandler() {
            @Override
            public void handleMessage(final Message msg) {
                switch (msg.what) {
                    case MSG_REPEAT: {
                        final KeyTouchState keyState = mTouchedKeys.getRepeatable();
                        if (keyState != null && keyState.isPressed && mAutoRepeatAllowed && getAutoRepeat()) {
                            sendEmptyMessageDelayed(MSG_REPEAT, mAutoRepeatInterval);
                            sendKeyUp(keyState.key, mAltKeysFcn);
                        }
                        break;
                    }
                }
            }
        };
    }

    @Override
    protected void onDetachedFromWindow() {
        mHandler.removeCallbacksAndMessages(null);
        mHandler = new WeakHandler();
        super.onDetachedFromWindow();
    }

    public void setFont(@NonNull final Typeface[] tfs) {
        typefaces = tfs;
        invalidateAllKeys();
    }

    public boolean isAutoRepeatAllowed() {
        return mAutoRepeatAllowed;
    }

    public int getAutoRepeatDelay() {
        return mAutoRepeatDelay;
    }

    public int getAutoRepeatInterval() {
        return mAutoRepeatInterval;
    }

    public void setAutoRepeatAllowed(final boolean autoRepeatAllowed) {
        this.mAutoRepeatAllowed = autoRepeatAllowed;
    }

    public void setAutoRepeatDelay(final int repeatDelay) {
        this.mAutoRepeatDelay = repeatDelay;
    }

    public void setAutoRepeatInterval(final int repeatInterval) {
        this.mAutoRepeatInterval = repeatInterval;
    }

    public boolean isHidden() {
        return mHidden;
    }

    public void setHidden(final boolean hidden) {
        mHidden = hidden;
        requestLayout();
        invalidateAllKeys();
    }

    /**
     * Attaches a keyboard to this view. The keyboard can be switched at any time and the
     * view will re-layout itself to accommodate the keyboard.
     *
     * @param keyboard the keyboard to display in this view
     * @see ExtKeyboard
     * @see #getKeyboard()
     */
    public void setKeyboard(final ExtKeyboard keyboard) {
        mHandler.removeMessages(MSG_REPEAT);
        mTouchedKeys.clear();
        mKeyboard = keyboard;
        requestLayout();
    }

    /**
     * Returns the current keyboard being displayed by this view.
     *
     * @return the currently attached keyboard
     * @see #setKeyboard(ExtKeyboard)
     */
    public ExtKeyboard getKeyboard() {
        return mKeyboard;
    }

    public void setOnKeyboardActionListener(final KeyboardView.OnKeyboardActionListener listener) {
        mKeyboardActionListener = listener;
    }

    /**
     * Returns the {@link KeyboardView.OnKeyboardActionListener} object.
     *
     * @return the listener attached to this keyboard
     */
    public KeyboardView.OnKeyboardActionListener getOnKeyboardActionListener() {
        return mKeyboardActionListener;
    }

    public void setAltKeys(final int v) {
        mAltKeysFcn = v;
        mTouchedKeys.invalidate();
    }

    public int getAltKeys() {
        return mAltKeysFcn;
    }

    public void setLedsByCode(final int code, final boolean on) {
        if (on) leds.add(code);
        else leds.remove(code);
    }

    /**
     * Requests a redraw of the entire keyboard. Calling {@link #invalidate} is not sufficient
     * because the keyboard renders the keys to an off-screen buffer and an invalidate() only
     * draws the cached buffer.
     *
     * @see #invalidateKey(ExtKeyboard.Key)
     */
    public void invalidateAllKeys() {
        mDirtyRect.union(0, 0, getWidth(), getHeight());
        invalidate();
    }

    public void invalidateModifierKeys(final int code) {
        if (mKeyboard != null)
            for (ExtKeyboard.Key key : mKeyboard.getKeysByCode(code))
                invalidateKey(key);
    }

    /**
     * Invalidates a key so that it will be redrawn on the next repaint. Use this method if only
     * one key is changing it's content. Any changes that affect the position or size of the key
     * may not be honored.
     *
     * @param key the key in the attached {@link ExtKeyboard}.
     * @see #invalidateAllKeys()
     */
    public void invalidateKey(@NonNull final ExtKeyboard.Key key) {
        invalidateKey(key, mTouchedKeys.isPressed(key));
    }

    protected void invalidateKey(@NonNull final ExtKeyboard.Key key, final boolean pressed) {
        onBufferDrawKey(key, pressed);
        invalidate(key.x + getPaddingLeft(), key.y + getPaddingTop(),
                key.x + key.width + getPaddingLeft(), key.y + key.height + getPaddingTop());
    }

    @Nullable
    private static ExtKeyboard.KeyFcn getKeyFcn(@NonNull final ExtKeyboard.Key key, final int id) {
        if (id < key.functions.size()) return key.functions.get(id);
        else if (key.functions.size() == 1) return key.functions.get(0);
        return null;
    }

    protected void sendKeyDown(@NonNull final ExtKeyboard.Key key, final int altKeysFcn) {
        if (mKeyboardActionListener == null) return;
        final ExtKeyboard.KeyFcn fcn = getKeyFcn(key, altKeysFcn);
        if (fcn != null && fcn.code != ExtKeyboard.KEYCODE_NONE)
            mKeyboardActionListener.onPress(fcn.code);
    }

    protected void sendKeyUp(@NonNull final ExtKeyboard.Key key, final int altKeysFcn) {
        if (mKeyboardActionListener == null) return;
        final ExtKeyboard.KeyFcn fcn = getKeyFcn(key, altKeysFcn);
        if (fcn != null) {
            if (fcn.code != ExtKeyboard.KEYCODE_NONE) {
                mKeyboardActionListener.onKey(fcn.code, null);
                mKeyboardActionListener.onRelease(fcn.code);
            }
            if (fcn.text != null)
                mKeyboardActionListener.onText(fcn.text);
        }
    }

    @Nullable
    protected ExtKeyboard.Key getKey(final float x, final float y) {
        return mKeyboard.getKey(
                (int) x + getPaddingLeft(),
                (int) y + getPaddingTop() + mVerticalCorrection
        );
    }

    protected final class KeyTouchMap {
        private final SparseArray<KeyTouchState> map = new SparseArray<>();
        private final Pools.Pool<KeyTouchState> pool = new Pools.SimplePool<>(16);

        @Nullable
        public KeyTouchState get(final int id) {
            return map.get(id);
        }

        public KeyTouchState put(final int id, @NonNull final ExtKeyboard.Key key,
                                 final float x, final float y) {
            KeyTouchState s = pool.acquire();
            if (s == null) {
                s = new KeyTouchState();
                s.popup = new Popup();
            }
            s.key = key;
            s.coords.x = x;
            s.coords.y = y;
            s.isPressed = true;
            s.popup.setKeyState(s);
            map.put(id, s);
            s.popup.show();
            return s;
        }

        public void remove(final int id) {
            final KeyTouchState s = map.get(id);
            if (s != null) {
                s.popup.hide();
                map.remove(id);
                pool.release(s);
            }
        }

        public void clear() {
            for (int i = 0; i < map.size(); ++i) {
                final KeyTouchState s = map.valueAt(i);
                s.popup.hide();
                pool.release(s);
            }
            map.clear();
        }

        public boolean isPressed(final ExtKeyboard.Key key) {
            for (int i = 0; i < map.size(); ++i) {
                final KeyTouchState s = map.valueAt(i);
                if (s.key == key && s.isPressed) return true;
            }
            return false;
        }

        @Nullable
        public KeyTouchState getRepeatable() {
            for (int i = 0; i < map.size(); ++i) {
                final KeyTouchState s = map.valueAt(i);
                if (s.key == null) continue;
                if (s.key.repeatable) return s;
            }
            return null;
        }

        public void invalidate() {
            for (int i = 0; i < map.size(); ++i) {
                final KeyTouchState s = map.valueAt(i);
                s.popup.invalidate();
            }
        }
    }

    protected static final class KeyTouchState {
        public ExtKeyboard.Key key = null;
        public final PointF coords = new PointF();
        public boolean isPressed = true;
        public Popup popup = null;
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        if (mKeyboard == null) return super.onTouchEvent(event);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_DOWN: {
                final int index = event.getActionIndex();
                final ExtKeyboard.Key key = getKey(event.getX(index), event.getY(index));
                if (key == null || key.type != ExtKeyboard.Key.KEY)
                    return super.onTouchEvent(event);
                final boolean first = !mTouchedKeys.isPressed(key);
                mTouchedKeys.put(event.getPointerId(index),
                        key, event.getX(index), event.getY(index));
                if (mTouchedKeys.getRepeatable() != null) {
                    mHandler.removeMessages(MSG_REPEAT);
                    mHandler.sendEmptyMessageDelayed(MSG_REPEAT, mAutoRepeatDelay);
                }
                if (first)
                    sendKeyDown(key, mAltKeysFcn);
                invalidateKey(key, true);
                return true;
            }
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP: {
                final int index = event.getActionIndex();
                final KeyTouchState keyState = mTouchedKeys.get(event.getPointerId(index));
                if (keyState == null) return super.onTouchEvent(event);
                mTouchedKeys.remove(event.getPointerId(index));
                if (keyState.isPressed && keyState.key != null && !mTouchedKeys.isPressed(keyState.key)) {
                    invalidateKey(keyState.key, false);
                }
                final int fcn = keyState.popup.getAltKeyFcn();
                if (fcn >= 0) {
                    sendKeyUp(keyState.key, fcn);
                }
                return true;
            }
            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < event.getPointerCount(); ++i) {
                    final KeyTouchState keyState = mTouchedKeys.get(event.getPointerId(i));
                    if (keyState == null) return super.onTouchEvent(event);
                    keyState.popup.addPointer(event.getX(i), event.getY(i));
                    final boolean oip = keyState.isPressed;
                    keyState.isPressed = mAltKeysFcn == keyState.popup.getAltKeyFcn();
                    if (keyState.key != null && keyState.isPressed != oip)
                        invalidateKey(keyState.key, keyState.isPressed);
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    public int getDesiredWidth() {
        return getPaddingLeft() + getPaddingRight() + ((mKeyboard == null) ? 0 : mKeyboard.getMinWidth());
    }

    public int getDesiredHeight() {
        return getPaddingTop() + getPaddingBottom() + ((mKeyboard == null) ? 0 : mKeyboard.getHeight());
    }

    protected static int getDefaultSize(int desiredSize, final int measureSpec, final int layoutSize) {
        final int specMode = MeasureSpec.getMode(measureSpec);
        final int specSize = MeasureSpec.getSize(measureSpec);
        if (layoutSize >= 0) desiredSize = layoutSize;
        switch (specMode) {
            case MeasureSpec.EXACTLY:
                return specSize;
            case MeasureSpec.AT_MOST:
                if (layoutSize == ViewGroup.LayoutParams.MATCH_PARENT) {
                    return specSize;
                }
                return Math.min(desiredSize, specSize);
            case MeasureSpec.UNSPECIFIED:
            default:
                return desiredSize;
        }
    }

    @Override
    public void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        if (mHidden) {
            setMeasuredDimension(0, 0);
            return;
        }
        setMeasuredDimension(
                getDefaultSize(getDesiredWidth(), widthMeasureSpec, getLayoutParams().width),
                getDefaultSize(getDesiredHeight(), heightMeasureSpec, getLayoutParams().height)
        );
    }

    @Override
    public void onSizeChanged(final int w, final int h, final int oldw, final int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (mKeyboard != null && !mHidden) {
            mKeyboard.resize(getContext(),
                    w - getPaddingLeft() - getPaddingRight(),
                    h - getPaddingTop() - getPaddingBottom());
        }
        mTouchedKeys.clear();
        mBuffer = null;
    }

    @Override
    public boolean isOpaque() {
        return true;
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);
        if (mHidden) return;
        if (!mDirtyRect.isEmpty() || mBuffer == null) {
            onBufferDraw();
        }
        canvas.drawBitmap(mBuffer, 0, 0, null);
    }

    protected void onBufferDraw() {
        if (mBuffer == null ||
                (mBuffer.getWidth() != getWidth() || mBuffer.getHeight() != getHeight())) {
            // Make sure our bitmap is at least 1x1
            final int width = Math.max(1, getWidth());
            final int height = Math.max(1, getHeight());
            mBuffer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            mCanvas = new Canvas(mBuffer);
            mDirtyRect.union(0, 0, getWidth(), getHeight());
        }
        final Canvas canvas = mCanvas;
        canvas.save();
        canvas.clipRect(mDirtyRect);
        getBackground().draw(canvas);

        if (mKeyboard == null) return;

        for (final ExtKeyboard.Key key : mKeyboard.getKeys()) {
            final int left = getPaddingLeft() + key.x;
            final int top = getPaddingTop() + key.y;
            if (canvas.quickReject(left, top, left + key.width, top + key.height, Canvas.EdgeType.AA))
                continue;
            onBufferDrawKey(key, mTouchedKeys.isPressed(key));
        }

        canvas.restore();
        mDirtyRect.setEmpty();
    }

    protected void onBufferDrawKey(final ExtKeyboard.Key key, final boolean pressed) {
        final Canvas canvas = mCanvas;
        if (canvas == null) return;
        final Paint paint = mPaint;
        final Drawable background = key.type == ExtKeyboard.Key.LED ?
                mLedBackground : mKeyBackground;
        final Rect padding = key.type == ExtKeyboard.Key.LED ?
                mLedPadding : mKeyPadding;
        final int left = getPaddingLeft() + key.x;
        final int top = getPaddingTop() + key.y;

        final ExtKeyboard.KeyFcn keyFcn = getKeyFcn(key, mAltKeysFcn);
        final int[] drawableState = ExtKeyboard.Key.getKeyState(pressed,
                keyFcn != null && leds.contains(keyFcn.code));
        background.setState(drawableState);

        final Rect bounds = background.getBounds();
        if (key.width != bounds.right ||
                key.height != bounds.bottom) {
            background.setBounds(0, 0, key.width, key.height);
        }

        canvas.save();

        canvas.translate(left, top);
        canvas.clipRect(0, 0, key.width, key.height);
        getBackground().draw(canvas);
        background.draw(canvas);

        if (keyFcn == null) {
            canvas.restore();
            return;
        }

        if (keyFcn.label != null) {
            if (keyFcn.label.length() < 2) {
                paint.setTextSize(mKeyTextSize);
                paint.setTypeface(typefaces[1]);
            } else {
                paint.setTextSize(mLabelTextSize);
                paint.setTypeface(typefaces[0]);
            }

            final float labelX = (float) (key.width - padding.left - padding.right) / 2
                    + padding.left;
            final float labelY = (float) (key.height - padding.top - padding.bottom) / 2
                    + (paint.getTextSize() - paint.descent()) / 2 + padding.top;

            paint.setShadowLayer(mShadowRadius, 0, 0, mShadowColor);
            if (key.showBothLabels) {
                final float loX = labelX / 2;
                final float loY = labelY / 2;
                canvas.drawText(keyFcn.label.toString(), labelX, labelY, paint);
                paint.setTextSize(paint.getTextSize() / 2);
                canvas.drawText(
                        key.functions.get(1 - mAltKeysFcn).label.toString(),
                        labelX + loX,
                        labelY + ((mAltKeysFcn == 0) ? -loY : (loY / 2)),
                        paint
                );
            } else canvas.drawText(keyFcn.label.toString(), labelX, labelY, paint);
            paint.clearShadowLayer();
        } else if (keyFcn.icon != null) {
            final int drawableX = (key.width - padding.left - padding.right
                    - keyFcn.icon.getIntrinsicWidth()) / 2 + padding.left;
            final int drawableY = (key.height - padding.top - padding.bottom
                    - keyFcn.icon.getIntrinsicHeight()) / 2 + padding.top;
            canvas.translate(drawableX, drawableY);
            keyFcn.icon.setBounds(0, 0,
                    keyFcn.icon.getIntrinsicWidth(), keyFcn.icon.getIntrinsicHeight());
            keyFcn.icon.draw(canvas);
            canvas.translate(-drawableX, -drawableY);
        }

        canvas.restore();
    }

    protected class Popup {
        protected final View view = new View(getContext());
        protected final PopupWindow window = new PopupWindow(view,
                mPopupKeySize * 3, mPopupKeySize * 3);
        protected final WeakHandler mHandler = new WeakHandler();
        protected final int[] mScreenCoords = new int[2];
        protected KeyTouchState keyState = null;
        protected float ptrA = Float.NaN;
        protected float ptrD = 0f;
        protected float ptrStep = 0f;
        protected int keyFcn = mAltKeysFcn;

        {
            window.setClippingEnabled(false);
        }

        public void setKeyState(@NonNull final KeyTouchState keyState) {
            this.keyState = keyState;
            this.ptrA = Float.NaN;
            this.ptrD = 0f;
            this.keyFcn = mAltKeysFcn;
            if (this.keyState.key != null)
                this.ptrStep = (float) Math.PI / (this.keyState.key.functions.size() - 1);
        }

        protected class View extends android.view.View {
            protected final Paint mPaint = new Paint();
            protected float mFontHeight;

            public View(final Context context) {
                super(context);
                setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
                mPaint.setAntiAlias(true);
                mPaint.setColor(mKeyTextColor);
                mPaint.setTextAlign(Align.CENTER);
                mPaint.setAlpha(255);
                mPaint.setStyle(Paint.Style.STROKE);
                mPaint.setTextSize(mPopupKeySize * 0.8f);
                mPaint.setTypeface(typefaces[1]);
                mFontHeight = mPaint.getFontSpacing();
            }

            @Override
            protected void onDraw(final Canvas canvas) {
                super.onDraw(canvas);
                if (keyState.key == null || keyState.key.functions.size() < 2) return;
                canvas.save();
                if (mPopupBackground != null) {
                    mPopupBackground.setBounds(0, 0, getWidth(), getHeight());
                    mPopupBackground.draw(canvas);
                }
                canvas.translate(getWidth() / 2f, getHeight() / 2f);
                mPaint.setShadowLayer(mPopupShadowRadius, 0, 0, mPopupShadowColor);
                for (int fcn = 0; fcn < keyState.key.functions.size(); ++fcn) {
                    final PointF coords = _getAltKeyFcnCoords(fcn);
                    final int[] state = ExtKeyboard.Key.getKeyState(fcn == getAltKeyFcn(),
                            false);
                    canvas.save();
                    canvas.translate(coords.x, coords.y);
                    canvas.save();
                    canvas.translate(-mFontHeight / 2, -mFontHeight / 2);
                    mPopupKeyBackground.mutate().setState(state);
                    mPopupKeyBackground.setBounds(0, 0, (int) mFontHeight, (int) mFontHeight);
                    canvas.clipRect(0, 0, mFontHeight, mFontHeight);
                    mPopupKeyBackground.draw(canvas);
                    canvas.restore();
                    final ExtKeyboard.KeyFcn keyFcn = keyState.key.functions.get(fcn);
                    if (keyFcn.label != null) {
                        if (mPopupKeyTextColor != null)
                            mPaint.setColor(mPopupKeyTextColor.getColorForState(state,
                                    mPopupKeyTextColor.getDefaultColor()));
                        canvas.drawText(keyFcn.label.toString(), 0, (mPaint.getTextSize() - mPaint.descent()) / 2, mPaint);
                    }
                    canvas.restore();
                }
                mPaint.clearShadowLayer();
                canvas.restore();
            }
        }

        protected final Runnable rShow = new Runnable() {
            @Override
            public void run() {
                if (keyState == null) return;
                ExtKeyboardView.this.getLocationOnScreen(mScreenCoords);
                window.showAtLocation(ExtKeyboardView.this, Gravity.NO_GRAVITY,
                        (int) (mScreenCoords[0] + keyState.coords.x - window.getWidth() / 2),
                        (int) (mScreenCoords[1] + keyState.coords.y - window.getHeight() / 2));
            }
        };

        public void show() {
            mHandler.postDelayed(rShow, mPopupDelay);
        }

        public void hide() {
            mHandler.removeCallbacks(rShow);
            window.dismiss();
        }

        public void addPointer(final float x, final float y) {
            final float dx = x - keyState.coords.x;
            final float dy = y - keyState.coords.y;
            ptrD = dx * dx + dy * dy;
            ptrA = (float) Math.atan2(-dy, dx);
            final int oFcn = keyFcn;
            keyFcn = _getAltKeyFcn();
            if (keyFcn != oFcn) view.invalidate();
        }

        public int getAltKeyFcn() {
            return keyFcn;
        }

        public void invalidate() {
            keyFcn = _getAltKeyFcn();
            view.invalidate();
        }

        protected int _getAltKeyFcn() {
            if (ptrD < mPopupKeySize * mPopupKeySize) return mAltKeysFcn;
            if (keyState.key == null || keyState.key.functions.size() < 2
                    || ptrA < 0)
                return -1;
            int i = (int) (ptrA / ptrStep) + 1;
            if (i == mAltKeysFcn) i = 0;
            return i;
        }

        protected final PointF _altKeyFcnCoords = new PointF();

        protected PointF _getAltKeyFcnCoords(int i) {
            if (i == 0) i = mAltKeysFcn;
            else if (i == mAltKeysFcn) i = 0;
            switch (i) {
                case 0:
                    _altKeyFcnCoords.x = 0;
                    _altKeyFcnCoords.y = 0;
                    break;
                case 1:
                    if (keyState.key == null || keyState.key.functions.size() == 2) {
                        _altKeyFcnCoords.x = 0;
                        _altKeyFcnCoords.y = -mPopupKeySize;
                        break;
                    }
                default:
                    final float a = ptrStep * ((i - 1) + 0.5f);
                    _altKeyFcnCoords.x = (float) Math.cos(a) * mPopupKeySize;
                    _altKeyFcnCoords.y = (float) -Math.sin(a) * mPopupKeySize;
            }
            return _altKeyFcnCoords;
        }
    }
}
