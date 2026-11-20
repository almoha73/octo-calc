package energy.octo.calc;

import android.animation.ValueAnimator;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewParent;
import android.view.WindowInsets;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;
import android.view.animation.DecelerateInterpolator;
import android.widget.OverScroller;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

final class CalculatorView extends View {

    interface OnOverlayChangedListener {
        void onOverlayChanged(boolean open);
    }

    private static final int INK = 0xFF100030;
    private static final int SURFACE = 0xFF211052;
    private static final int SURFACE_PRESSED = 0xFF2C1968;
    private static final int ACTION = 0xFF2B1A5F;
    private static final int OP = 0xFF341B6B;
    private static final int PINK = 0xFFFF48D8;
    private static final int PINK_DEEP = 0xFFB945FF;
    private static final int PINK_SOFT = 0xFFFF9BDD;
    private static final int LAVENDER = 0xFFD9DDFF;
    private static final int MUTED = 0xFFA49FC8;
    private static final int MUTED_DARK = 0xFF7C74A8;
    private static final int WHITE = 0xFFFFFFFF;

    private static final int ID_AC = 1;
    private static final int ID_PAREN = 2;
    private static final int ID_PCT = 3;
    private static final int ID_DIV = 4;
    private static final int ID_MUL = 5;
    private static final int ID_SUB = 6;
    private static final int ID_ADD = 7;
    private static final int ID_DOT = 8;
    private static final int ID_BACK = 9;
    private static final int ID_EQ = 10;
    private static final int ID_NUM = 100;
    private static final int ID_INV = 200;
    private static final int ID_DEG = 201;
    private static final int ID_SIN = 202;
    private static final int ID_COS = 203;
    private static final int ID_TAN = 204;
    private static final int ID_LN = 205;
    private static final int ID_LOG = 206;
    private static final int ID_EXP = 207;
    private static final int ID_TENX = 208;
    private static final int ID_POW = 209;
    private static final int ID_SQRT = 210;
    private static final int ID_SQR = 211;
    private static final int ID_FACT = 212;
    private static final int ID_PI = 213;
    private static final int ID_E = 214;
    private static final int ID_MC = 215;
    private static final int ID_MR = 216;
    private static final int ID_MPLUS = 217;
    private static final int ID_MMINUS = 218;
    private static final int ID_ANS = 219;

    private static final int OVERLAY_NONE = 0;
    private static final int OVERLAY_HISTORY = 1;
    private static final int OVERLAY_MENU = 2;
    private static final int OVERLAY_CONFIRM = 3;
    private static final int OVERLAY_ABOUT = 4;

    private static final int TARGET_NONE = 0;
    private static final int TARGET_HIST_CLOSE = 1;
    private static final int TARGET_HIST_CLEAR = 2;
    private static final int TARGET_HIST_LIST = 3;
    private static final int TARGET_HIST_OUTSIDE = 4;
    private static final int TARGET_MENU_CLEAR = 10;
    private static final int TARGET_MENU_SCI = 11;
    private static final int TARGET_MENU_ABOUT = 12;
    private static final int TARGET_MENU_OUTSIDE = 13;
    private static final int TARGET_CONFIRM_OK = 20;
    private static final int TARGET_CONFIRM_CANCEL = 21;
    private static final int TARGET_CONFIRM_OUTSIDE = 22;
    private static final int TARGET_ABOUT_CLOSE = 30;
    private static final int TARGET_ABOUT_OUTSIDE = 31;

    private static final int V_DISPLAY = 1;
    private static final int V_HIST_ICON = 2;
    private static final int V_FX_ICON = 3;
    private static final int V_MENU_ICON = 4;
    private static final int V_BTN_BASE = 1000;
    private static final int V_HIST_CLEAR = 2001;
    private static final int V_HIST_CLOSE = 2002;
    private static final int V_MENU_CLEAR = 2010;
    private static final int V_MENU_SCI = 2011;
    private static final int V_MENU_ABOUT = 2012;
    private static final int V_CONFIRM_CANCEL = 2020;
    private static final int V_CONFIRM_OK = 2021;
    private static final int V_ABOUT_CLOSE = 2030;
    private static final int V_HIST_ITEM_BASE = 3000;

    private static final class Btn {
        final int id;
        final boolean circle;
        final RectF rect = new RectF();
        float cx, cy, r;

        Btn(int id, boolean circle) {
            this.id = id;
            this.circle = circle;
        }
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final HistoryStore history;
    private final List<Btn> buttons = new ArrayList<>();
    private final OverScroller historyScroller;
    private final AccessibilityManager accessibilityManager;

    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint buttonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint dimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private RadialGradient glowTopShader;
    private RadialGradient glowBottomShader;
    private LinearGradient eqShader;
    private final RectF tmpRect = new RectF();
    private final RectF tmpShadowRect = new RectF();
    private final Path tmpPath = new Path();
    private final int[] screenLoc = new int[2];

    private final Typeface regular;
    private final Typeface medium;
    private final Typeface semibold;
    private final Bitmap mascot;
    private final RectF mascotRect = new RectF();

    private final ArrayList<String> tokens = new ArrayList<>();
    private String lastExpr;
    private String lastResultRaw;
    private double lastResultValue;
    private double lastAnswer;
    private boolean hasAnswer;
    private String errorMsg;
    private String cachedPreview;

    private boolean degrees;
    private boolean inverse;
    private boolean scientific;
    private double memory;
    private boolean hasMemory;
    private boolean recalledFromHistory;

    private final DisplayMetrics displayMetrics;
    private final float density;
    private int W, H;
    private float padTop, padBottom;
    private float contentLeft, contentW;

    private float headerTop, headerH;
    private float displayTop, displayBottom;
    private float keypadTop, keypadCell, keypadCellW, keypadGap;
    private float sciTop, sciHeight;
    private float displayAvail;
    private float mainScroll;
    private boolean scrollToEnd = true;
    private float mainWidth;
    private final RectF displayRect = new RectF();

    private final RectF rHistory = new RectF();
    private final RectF rFx = new RectF();
    private final RectF rOverflow = new RectF();
    private final RectF rDisplay = new RectF();

    private int pressedId = -1;
    private boolean longFired;
    private int headerPressed;
    private boolean displayDragging;
    private boolean displayMoved;
    private float displayDragStartX;
    private float displayDragStartScroll;
    private final Runnable longPressRunnable = this::onLongPress;

    private int overlay = OVERLAY_NONE;
    private float overlayAnim = 1f;
    private ValueAnimator overlayAnimator;
    private int overlayDownTarget = TARGET_NONE;
    private boolean confirmResetAll;
    private OnOverlayChangedListener overlayChangedListener;

    private final RectF historySheet = new RectF();
    private final RectF historyList = new RectF();
    private final RectF rHistClear = new RectF();
    private final RectF rHistClose = new RectF();
    private float historyScroll;
    private float historyMaxScroll;
    private boolean historyDragging;
    private float historyDownY;
    private float historyStartScroll;
    private boolean historyMoved;
    private VelocityTracker velocityTracker;

    private final RectF menuCard = new RectF();
    private final RectF menuClear = new RectF();
    private final RectF menuSci = new RectF();
    private final RectF menuAbout = new RectF();

    private final RectF confirmCard = new RectF();
    private final RectF confirmCancel = new RectF();
    private final RectF confirmOk = new RectF();
    private final RectF aboutCard = new RectF();
    private final RectF aboutClose = new RectF();

    private CalcAccessibilityProvider a11yProvider;
    private int focusedVirtualId = View.NO_ID;
    private int hoveredVirtualId = View.NO_ID;

    CalculatorView(Context context) {
        super(context);
        displayMetrics = getResources().getDisplayMetrics();
        density = displayMetrics.density;
        regular = getResources().getFont(R.font.poppins_regular);
        medium = getResources().getFont(R.font.poppins_medium);
        semibold = getResources().getFont(R.font.poppins_semibold);
        mascot = BitmapFactory.decodeResource(getResources(), R.drawable.mascot);
        history = new HistoryStore(context);
        historyScroller = new OverScroller(context);
        accessibilityManager = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);

        degrees = history.isDegrees();
        scientific = history.isScientific();
        hasMemory = history.hasMemory();
        memory = hasMemory ? history.getMemory() : 0;

        setFocusable(true);
        setFocusableInTouchMode(true);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        setBackgroundColor(INK);
        setSystemUiVisibility(
                SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }

    void setOnOverlayChangedListener(OnOverlayChangedListener listener) {
        this.overlayChangedListener = listener;
        if (listener != null) {
            listener.onOverlayChanged(overlay != OVERLAY_NONE);
        }
    }

    private float dp(float value) {
        return value * density;
    }

    private float sp(float value) {
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, displayMetrics);
        return Math.min(px, dp(value * 1.35f));
    }

    private String str(int resId) {
        return getContext().getString(resId);
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            padTop = bars.top;
            padBottom = bars.bottom;
        } else {
            padTop = insets.getSystemWindowInsetTop();
            padBottom = insets.getSystemWindowInsetBottom();
        }
        layoutUi();
        invalidate();
        return insets;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        W = w;
        H = h;
        bgPaint.setShader(new LinearGradient(0, 0, W * 0.55f, H,
                new int[]{0xFF170741, 0xFF100030, 0xFF0A0020}, null, Shader.TileMode.CLAMP));
        glowTopShader = new RadialGradient(W * 0.92f, H * 0.30f, Math.max(1f, W * 0.62f),
                new int[]{0x2AFF48D8, 0x0DFF48D8, 0x00FF48D8}, null, Shader.TileMode.CLAMP);
        glowBottomShader = new RadialGradient(W * 0.02f, H * 0.72f, Math.max(1f, W * 0.55f),
                new int[]{0x244B2AD8, 0x004B2AD8}, null, Shader.TileMode.CLAMP);
        layoutUi();
        requestFocus();
    }

    private void layoutUi() {
        if (W == 0 || H == 0) {
            return;
        }
        contentW = Math.min(W, dp(540));
        contentLeft = (W - contentW) / 2f;

        float margin = dp(16);
        keypadGap = dp(9);
        keypadCellW = (contentW - 2 * margin - 3 * keypadGap) / 4f;
        keypadCell = Math.min(keypadCellW * 1.1f, dp(96));
        headerTop = padTop + dp(6);
        headerH = dp(52);

        float keypadH = 5 * keypadCell + 4 * keypadGap;
        float sciBtnH = dp(40);
        float sciGap = dp(6);
        sciHeight = scientific ? 4 * sciBtnH + 3 * sciGap + dp(8) : 0;

        float contentBottom = H - padBottom - dp(10);
        float available = contentBottom - (headerTop + headerH) - sciHeight - keypadH - dp(14);
        while (available < dp(110) && keypadCell > dp(52)) {
            keypadCell -= dp(1);
            keypadH = 5 * keypadCell + 4 * keypadGap;
            available = contentBottom - (headerTop + headerH) - sciHeight - keypadH - dp(14);
        }

        // Progressive compression for compact split-screen or landscape viewports.
        while (available < dp(88)) {
            boolean shrunk = false;
            if (keypadCell > dp(38)) {
                keypadCell = Math.max(dp(38), keypadCell - dp(1));
                shrunk = true;
            }
            if (scientific && sciBtnH > dp(28)) {
                sciBtnH = Math.max(dp(28), sciBtnH - dp(0.75f));
                shrunk = true;
            }
            if (keypadGap > dp(5)) {
                keypadGap = Math.max(dp(5), keypadGap - dp(0.5f));
                shrunk = true;
            }
            if (sciGap > dp(4)) {
                sciGap = Math.max(dp(4), sciGap - dp(0.25f));
                shrunk = true;
            }
            if (headerH > dp(40)) {
                headerH = Math.max(dp(40), headerH - dp(0.5f));
                shrunk = true;
            }
            if (!shrunk) {
                break;
            }
            keypadCellW = (contentW - 2 * margin - 3 * keypadGap) / 4f;
            keypadCell = Math.min(keypadCell, keypadCellW * 1.1f);
            keypadH = 5 * keypadCell + 4 * keypadGap;
            sciHeight = scientific ? 4 * sciBtnH + 3 * sciGap + dp(8) : 0;
            available = contentBottom - (headerTop + headerH) - sciHeight - keypadH - dp(14);
        }

        keypadTop = contentBottom - keypadH;
        sciTop = keypadTop - sciHeight;
        displayTop = headerTop + headerH + dp(4);
        displayBottom = scientific ? sciTop - dp(4) : keypadTop - dp(6);
        if (displayBottom < displayTop + dp(56)) {
            displayBottom = displayTop + dp(56);
        }
        displayRect.set(contentLeft, displayTop, contentLeft + contentW, displayBottom);
        rDisplay.set(contentLeft + dp(8), displayTop, contentLeft + contentW - dp(8), displayBottom);

        float keypadLeft = contentLeft + margin;
        float halfW = keypadCellW / 2f;
        float halfH = keypadCell / 2f;
        float r = Math.min(halfW, halfH);
        buttons.clear();

        String[] row0 = {"AC", "()", "%", "\u00f7"};
        String[] row1 = {"7", "8", "9", "\u00d7"};
        String[] row2 = {"4", "5", "6", "\u2212"};
        String[] row3 = {"1", "2", "3", "+"};
        String[] row4 = {"0", ",", "back", "="};
        String[][] rows = {row0, row1, row2, row3, row4};
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 4; col++) {
                float cx = keypadLeft + col * (keypadCellW + keypadGap) + halfW;
                float cy = keypadTop + row * (keypadCell + keypadGap) + halfH;
                Btn b = new Btn(idForLabel(rows[row][col]), true);
                b.cx = cx;
                b.cy = cy;
                b.r = r;
                b.rect.set(cx - halfW, cy - halfH, cx + halfW, cy + halfH);
                buttons.add(b);
            }
        }

        Btn eqBtn = findButton(ID_EQ);
        if (eqBtn != null) {
            eqShader = new LinearGradient(eqBtn.rect.left, eqBtn.rect.top, eqBtn.rect.right, eqBtn.rect.bottom,
                    new int[]{PINK, PINK_DEEP}, null, Shader.TileMode.CLAMP);
        }

        if (scientific) {
            float sciGapX = dp(6);
            float sw = (contentW - 2 * margin - 4 * sciGapX) / 5f;
            float sh = sciBtnH;
            int[][] ids = {
                    {ID_INV, ID_DEG, ID_SIN, ID_COS, ID_TAN},
                    {ID_LN, ID_LOG, ID_EXP, ID_TENX, ID_POW},
                    {ID_SQRT, ID_SQR, ID_FACT, ID_PI, ID_E},
                    {ID_MC, ID_MR, ID_MPLUS, ID_MMINUS, ID_ANS}
            };
            for (int row = 0; row < 4; row++) {
                for (int col = 0; col < ids[row].length; col++) {
                    float x = keypadLeft + col * (sw + sciGapX);
                    float y = sciTop + dp(4) + row * (sh + sciGap);
                    Btn b = new Btn(ids[row][col], false);
                    b.rect.set(x, y, x + sw, y + sh);
                    b.cx = x + sw / 2f;
                    b.cy = y + sh / 2f;
                    b.r = sh / 2f;
                    buttons.add(b);
                }
            }
        }

        float iconY = headerTop + headerH / 2f;
        float iconSize = Math.min(dp(44), headerH);
        float right = contentLeft + contentW - dp(20);
        rOverflow.set(right - iconSize / 2f, iconY - iconSize / 2f, right + iconSize / 2f, iconY + iconSize / 2f);
        float fxX = right - dp(46);
        rFx.set(fxX - iconSize / 2f, iconY - iconSize / 2f, fxX + iconSize / 2f, iconY + iconSize / 2f);
        float histX = fxX - dp(46);
        rHistory.set(histX - iconSize / 2f, iconY - iconSize / 2f, histX + iconSize / 2f, iconY + iconSize / 2f);

        layoutOverlays();
        scrollToEnd = true;
        invalidate();
    }

    private void layoutOverlays() {
        refreshHistoryBounds();

        float margin = dp(12);
        float menuRight = contentLeft + contentW - margin;
        float menuWidth = Math.min(dp(302), contentW - dp(24));
        float menuTop = headerTop + headerH + dp(4);
        menuCard.set(menuRight - menuWidth, menuTop, menuRight, menuTop + 3 * dp(54) + dp(14));
        float itemTop = menuCard.top + dp(7);
        menuClear.set(menuCard.left, itemTop, menuCard.right, itemTop + dp(54));
        menuSci.set(menuCard.left, itemTop + dp(54), menuCard.right, itemTop + dp(108));
        menuAbout.set(menuCard.left, itemTop + dp(108), menuCard.right, itemTop + dp(162));

        float cw = Math.min(dp(330), W - dp(40));
        float availH = Math.max(dp(180), H - padTop - padBottom - dp(24));
        float ch = Math.min(dp(214), availH);
        confirmCard.set((W - cw) / 2f, (H - ch) / 2f, (W + cw) / 2f, (H + ch) / 2f);
        float by = confirmCard.bottom - dp(66);
        confirmCancel.set(confirmCard.left + dp(20), by, confirmCard.centerX() - dp(6), by + dp(46));
        confirmOk.set(confirmCard.centerX() + dp(6), by, confirmCard.right - dp(20), by + dp(46));

        float ah = Math.min(dp(352), availH);
        aboutCard.set((W - cw) / 2f, (H - ah) / 2f, (W + cw) / 2f, (H + ah) / 2f);
        aboutClose.set(aboutCard.left + dp(24), aboutCard.bottom - dp(66),
                aboutCard.right - dp(24), aboutCard.bottom - dp(20));
    }

    private int idForLabel(String label) {
        switch (label) {
            case "AC":
                return ID_AC;
            case "()":
                return ID_PAREN;
            case "%":
                return ID_PCT;
            case "\u00f7":
                return ID_DIV;
            case "\u00d7":
                return ID_MUL;
            case "\u2212":
                return ID_SUB;
            case "+":
                return ID_ADD;
            case ",":
                return ID_DOT;
            case "back":
                return ID_BACK;
            case "=":
                return ID_EQ;
            default:
                return ID_NUM + Integer.parseInt(label);
        }
    }

    private String labelFor(Btn b) {
        if (b.id >= ID_NUM && b.id < ID_NUM + 10) {
            return String.valueOf(b.id - ID_NUM);
        }
        switch (b.id) {
            case ID_AC:
                return isAllClear() ? "AC" : "C";
            case ID_PAREN:
                return "()";
            case ID_PCT:
                return "%";
            case ID_DIV:
                return "\u00f7";
            case ID_MUL:
                return "\u00d7";
            case ID_SUB:
                return "\u2212";
            case ID_ADD:
                return "+";
            case ID_DOT:
                return ",";
            case ID_BACK:
                return "";
            case ID_EQ:
                return "=";
            case ID_INV:
                return "INV";
            case ID_DEG:
                return degrees ? "DEG" : "RAD";
            case ID_SIN:
                return inverse ? "sin\u207b\u00b9" : "sin";
            case ID_COS:
                return inverse ? "cos\u207b\u00b9" : "cos";
            case ID_TAN:
                return inverse ? "tan\u207b\u00b9" : "tan";
            case ID_LN:
                return "ln";
            case ID_LOG:
                return "log";
            case ID_EXP:
                return "e^x";
            case ID_TENX:
                return "10^x";
            case ID_POW:
                return "x^y";
            case ID_SQRT:
                return "\u221a";
            case ID_SQR:
                return "x\u00b2";
            case ID_FACT:
                return "n!";
            case ID_PI:
                return "\u03c0";
            case ID_E:
                return "e";
            case ID_MC:
                return "MC";
            case ID_MR:
                return "MR";
            case ID_MPLUS:
                return "M+";
            case ID_MMINUS:
                return "M\u2212";
            case ID_ANS:
                return "Ans";
        }
        return "";
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(0, 0, W, H, bgPaint);
        drawGlow(canvas);
        drawHeader(canvas);
        drawDisplay(canvas);
        for (Btn b : buttons) {
            drawButton(canvas, b);
        }
        if (overlay != OVERLAY_NONE) {
            drawOverlay(canvas);
        }
    }

    private void drawGlow(Canvas canvas) {
        if (glowTopShader != null) {
            glowPaint.setShader(glowTopShader);
            canvas.drawCircle(W * 0.92f, H * 0.30f, W * 0.62f, glowPaint);
        }
        if (glowBottomShader != null) {
            glowPaint.setShader(glowBottomShader);
            canvas.drawCircle(W * 0.02f, H * 0.72f, W * 0.55f, glowPaint);
        }
        glowPaint.setShader(null);
    }

    private void drawHeader(Canvas canvas) {
        float cy = headerTop + headerH / 2f;
        drawMascot(canvas, contentLeft + dp(28), cy + dp(1.5f), dp(31), 1f);
        textPaint.setTypeface(semibold);
        textPaint.setTextSize(Math.min(sp(21), headerH * 0.45f));
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(WHITE);
        float titleX = contentLeft + dp(49);
        float titleY = cy + dp(7.5f);
        canvas.drawText("Octo", titleX, titleY, textPaint);
        float w = textPaint.measureText("Octo");
        textPaint.setColor(PINK);
        canvas.drawText("Calc", titleX + w + dp(1), titleY, textPaint);

        drawHistoryIcon(canvas, rHistory.centerX(), cy);
        drawFxIcon(canvas, rFx.centerX(), cy);
        drawOverflowIcon(canvas, rOverflow.centerX(), cy);
    }

    private void drawMascot(Canvas canvas, float cx, float cy, float width, float alpha) {
        if (mascot == null || width <= 0) {
            return;
        }
        float height = width * mascot.getHeight() / (float) mascot.getWidth();
        mascotRect.set(cx - width / 2f, cy - height / 2f, cx + width / 2f, cy + height / 2f);
        buttonPaint.setShader(null);
        buttonPaint.setStyle(Paint.Style.FILL);
        buttonPaint.setColor(WHITE);
        buttonPaint.setAlpha((int) (255 * alpha));
        buttonPaint.setFilterBitmap(true);
        canvas.drawBitmap(mascot, null, mascotRect, buttonPaint);
        buttonPaint.setFilterBitmap(false);
        buttonPaint.setAlpha(255);
    }

    private void drawHistoryIcon(Canvas canvas, float cx, float cy) {
        linePaint.setShader(null);
        linePaint.setColor(LAVENDER);
        linePaint.setAlpha(overlay == OVERLAY_HISTORY ? 255 : 215);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(dp(1.9f));
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        tmpRect.set(cx - dp(9), cy - dp(9), cx + dp(9), cy + dp(9));
        canvas.drawArc(tmpRect, 205, 285, false, linePaint);
        tmpPath.reset();
        tmpPath.moveTo(cx - dp(9.5f), cy - dp(3.5f));
        tmpPath.lineTo(cx - dp(9.5f), cy - dp(9f));
        tmpPath.lineTo(cx - dp(3.8f), cy - dp(9f));
        canvas.drawPath(tmpPath, linePaint);
        canvas.drawLine(cx, cy - dp(4.5f), cx, cy + dp(0.5f), linePaint);
        canvas.drawLine(cx, cy + dp(0.5f), cx + dp(4f), cy + dp(2.5f), linePaint);
        linePaint.setAlpha(255);
    }

    private void drawFxIcon(Canvas canvas, float cx, float cy) {
        tmpRect.set(cx - dp(14), cy - dp(11.5f), cx + dp(14), cy + dp(11.5f));
        buttonPaint.setShader(null);
        buttonPaint.setStyle(Paint.Style.FILL);
        buttonPaint.setColor(scientific ? PINK : 0x00000000);
        canvas.drawRoundRect(tmpRect, dp(11), dp(11), buttonPaint);
        if (!scientific) {
            strokePaint.setShader(null);
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeWidth(dp(1.3f));
            strokePaint.setColor(0xFF493579);
            canvas.drawRoundRect(tmpRect, dp(11), dp(11), strokePaint);
        }
        textPaint.setTypeface(medium);
        textPaint.setTextSize(sp(13));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(scientific ? INK : LAVENDER);
        float base = cy - (textPaint.ascent() + textPaint.descent()) / 2f;
        canvas.drawText("fx", cx, base, textPaint);
    }

    private void drawOverflowIcon(Canvas canvas, float cx, float cy) {
        buttonPaint.setShader(null);
        buttonPaint.setStyle(Paint.Style.FILL);
        buttonPaint.setColor(LAVENDER);
        buttonPaint.setAlpha(220);
        canvas.drawCircle(cx - dp(7), cy, dp(1.9f), buttonPaint);
        canvas.drawCircle(cx, cy, dp(1.9f), buttonPaint);
        canvas.drawCircle(cx + dp(7), cy, dp(1.9f), buttonPaint);
        buttonPaint.setAlpha(255);
    }

    private String currentMainDisplayText() {
        if (errorMsg != null) {
            return errorMsg;
        }
        if (tokens.isEmpty() && lastResultRaw != null) {
            return pretty(lastResultRaw);
        }
        String expr = joinExpression();
        return expr.isEmpty() ? "0" : pretty(expr);
    }

    private String spokenDisplayText() {
        String main = currentMainDisplayText();
        if (errorMsg == null && cachedPreview != null) {
            return main + ", = " + cachedPreview;
        }
        return main;
    }

    private void drawDisplay(Canvas canvas) {
        String small = null;
        if (errorMsg != null) {
            String expr = joinExpression();
            small = expr.isEmpty() ? null : pretty(expr);
        } else if (tokens.isEmpty() && lastResultRaw != null) {
            small = lastExpr == null ? null : lastExpr + " =";
        }

        String main = currentMainDisplayText();
        boolean previewVisible = errorMsg == null && cachedPreview != null;

        canvas.save();
        canvas.clipRect(displayRect);
        float right = contentLeft + contentW - dp(24);
        float avail = contentW - dp(48);
        displayAvail = avail;

        float previewSize = Math.min(sp(22), Math.max(dp(14), displayRect.height() * 0.22f));
        float previewY = displayBottom - dp(8);
        float mainBase = previewVisible ? previewY - dp(28) : displayBottom - dp(12);

        float size = (tokens.isEmpty() && lastResultRaw != null && errorMsg == null) ? dp(64) : dp(46);
        if (errorMsg != null) {
            size = dp(30);
        }
        float maxMainH = Math.max(dp(20), displayRect.height() - (small != null ? dp(28) : dp(8)) - (previewVisible ? dp(28) : dp(10)));
        if (size > maxMainH) {
            size = maxMainH;
        }

        if (small != null) {
            textPaint.setTypeface(medium);
            textPaint.setColor(MUTED);
            float smallMaxW = hasMemory ? Math.max(dp(40), avail - dp(32)) : avail;
            float smallBase = Math.min(displayTop + dp(24), mainBase - size * 0.9f);
            smallBase = Math.max(displayTop + dp(16), smallBase);
            drawRightAlignedText(canvas, small, right, smallBase, smallMaxW, sp(18), sp(11));
        }

        textPaint.setTypeface(semibold);
        textPaint.setTextSize(size);
        float measured = textPaint.measureText(main);
        while (measured > avail && size > dp(20)) {
            size -= dp(1);
            textPaint.setTextSize(size);
            measured = textPaint.measureText(main);
        }
        mainWidth = measured;

        float limit = Math.max(0, measured - avail);
        if (scrollToEnd) {
            mainScroll = 0;
            scrollToEnd = false;
        }
        if (mainScroll > limit) {
            mainScroll = limit;
        }
        if (mainScroll < 0) {
            mainScroll = 0;
        }

        textPaint.setTextAlign(Paint.Align.RIGHT);
        textPaint.setColor(errorMsg != null ? PINK_SOFT : WHITE);
        if (errorMsg == null && tokens.isEmpty() && lastResultRaw != null) {
            textPaint.setShadowLayer(dp(18), 0, 0, 0x33FF48D8);
        } else {
            textPaint.clearShadowLayer();
        }
        canvas.drawText(main, right + mainScroll, mainBase, textPaint);
        textPaint.clearShadowLayer();

        if (previewVisible) {
            textPaint.setTypeface(medium);
            textPaint.setColor(0xFFB9B2E6);
            drawRightAlignedText(canvas, cachedPreview, right, previewY, avail, previewSize, sp(12));
        }

        if (hasMemory) {
            float badgeLeft = contentLeft + dp(24);
            tmpRect.set(badgeLeft, displayTop + dp(2), badgeLeft + dp(24), displayTop + dp(20));
            buttonPaint.setShader(null);
            buttonPaint.setStyle(Paint.Style.FILL);
            buttonPaint.setColor(0xFF2B1A5F);
            canvas.drawRoundRect(tmpRect, dp(9), dp(9), buttonPaint);
            textPaint.setTypeface(semibold);
            textPaint.setTextSize(sp(10));
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setColor(PINK);
            canvas.drawText("M", tmpRect.centerX(), tmpRect.centerY() + dp(3.5f), textPaint);
        }
        canvas.restore();
    }

    private void drawRightAlignedText(Canvas canvas, String text, float right, float baseline,
            float maxWidth, float maxSize, float minSize) {
        if (text == null || text.isEmpty() || maxWidth <= 0) {
            return;
        }
        textPaint.setTextAlign(Paint.Align.RIGHT);
        float size = maxSize;
        textPaint.setTextSize(size);
        while (textPaint.measureText(text) > maxWidth && size > minSize) {
            size = Math.max(minSize, size - dp(1));
            textPaint.setTextSize(size);
        }
        String shown = text;
        if (textPaint.measureText(shown) > maxWidth) {
            int lo = 0;
            int hi = text.length();
            while (lo < hi) {
                int mid = (lo + hi) / 2;
                String candidate = "\u2026" + text.substring(mid);
                if (textPaint.measureText(candidate) <= maxWidth) {
                    hi = mid;
                } else {
                    lo = mid + 1;
                }
            }
            shown = lo < text.length() ? ("\u2026" + text.substring(lo)) : "\u2026";
        }
        canvas.drawText(shown, right, baseline, textPaint);
    }

    private void drawButton(Canvas canvas, Btn b) {
        boolean pressed = pressedId == b.id;
        float scale = pressed ? 0.945f : 1f;
        boolean sci = !b.circle;
        int bg;
        int fg;

        if (sci) {
            boolean active = (b.id == ID_INV && inverse) || (b.id == ID_DEG);
            if (pressed) {
                bg = active ? 0xFFE23BC2 : 0xFF3A2475;
                fg = active ? INK : WHITE;
            } else if (active) {
                bg = PINK;
                fg = INK;
            } else {
                bg = 0xFF1B0C48;
                fg = 0xFFB9B2E6;
            }
        } else if (b.id == ID_EQ) {
            bg = PINK;
            fg = INK;
        } else if (b.id == ID_AC) {
            bg = ACTION;
            fg = PINK_SOFT;
        } else if (b.id == ID_DIV || b.id == ID_MUL || b.id == ID_SUB || b.id == ID_ADD) {
            bg = OP;
            fg = PINK_SOFT;
        } else if (b.id == ID_PAREN || b.id == ID_PCT || b.id == ID_BACK) {
            bg = ACTION;
            fg = LAVENDER;
        } else {
            bg = SURFACE;
            fg = WHITE;
        }
        if (pressed && b.id != ID_INV && b.id != ID_DEG) {
            if (b.id == ID_EQ) {
                bg = PINK;
            } else if (b.id == ID_AC) {
                bg = 0xFF3C2474;
            } else {
                bg = SURFACE_PRESSED;
            }
        }

        canvas.save();
        canvas.scale(scale, scale, b.cx, b.cy);
        if (b.circle) {
            float r = b.r;
            if (b.id == ID_EQ && eqShader != null) {
                buttonPaint.setShader(eqShader);
                buttonPaint.setColor(PINK);
            } else {
                buttonPaint.setShader(null);
                buttonPaint.setColor(bg);
            }
            buttonPaint.setStyle(Paint.Style.FILL);
            canvas.drawRoundRect(b.rect, r, r, buttonPaint);
            buttonPaint.setShader(null);
        } else {
            buttonPaint.setShader(null);
            buttonPaint.setStyle(Paint.Style.FILL);
            buttonPaint.setColor(bg);
            canvas.drawRoundRect(b.rect, dp(13), dp(13), buttonPaint);
            if (!pressed && bg == 0xFF1B0C48) {
                strokePaint.setShader(null);
                strokePaint.setStyle(Paint.Style.STROKE);
                strokePaint.setStrokeWidth(dp(1));
                strokePaint.setColor(0x14FFFFFF);
                canvas.drawRoundRect(b.rect, dp(13), dp(13), strokePaint);
            }
        }

        String label = labelFor(b);
        if (sci) {
            float size = Math.min(label.length() >= 5 ? sp(10.5f) : sp(12.5f), b.rect.height() * 0.44f);
            textPaint.setTypeface(medium);
            textPaint.setTextSize(size);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setColor(fg);
            float base = b.cy - (textPaint.ascent() + textPaint.descent()) / 2f;
            canvas.drawText(label, b.cx, base, textPaint);
        } else if (b.id == ID_BACK) {
            drawBackspaceIcon(canvas, b.cx, b.cy, b.r * 0.72f, fg);
        } else if (b.id == ID_EQ) {
            drawEqualsIcon(canvas, b.cx, b.cy, b.r * 0.86f);
        } else {
            float size;
            if (b.id == ID_AC) {
                size = b.r * 0.62f;
            } else if (b.id == ID_PAREN || b.id == ID_PCT) {
                size = b.r * 0.72f;
            } else {
                size = b.r * 0.90f;
            }
            textPaint.setTypeface(b.id == ID_DOT ? semibold : medium);
            textPaint.setTextSize(size);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setColor(fg);
            float base = b.cy - (textPaint.ascent() + textPaint.descent()) / 2f;
            canvas.drawText(label, b.cx, base, textPaint);
        }
        canvas.restore();
    }

    private void drawBackspaceIcon(Canvas canvas, float cx, float cy, float size, int color) {
        linePaint.setShader(null);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(dp(2f));
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setColor(color);
        float w = size * 0.98f;
        float h = size * 0.72f;
        tmpPath.reset();
        tmpPath.moveTo(cx - w / 2f, cy);
        tmpPath.lineTo(cx - w / 2f + h * 0.55f, cy - h / 2f);
        tmpPath.lineTo(cx + w / 2f, cy - h / 2f);
        tmpPath.lineTo(cx + w / 2f, cy + h / 2f);
        tmpPath.lineTo(cx - w / 2f + h * 0.55f, cy + h / 2f);
        tmpPath.close();
        canvas.drawPath(tmpPath, linePaint);
        float xs = h * 0.22f;
        canvas.drawLine(cx - xs, cy - xs, cx + xs, cy + xs, linePaint);
        canvas.drawLine(cx - xs, cy + xs, cx + xs, cy - xs, linePaint);
    }

    private void drawEqualsIcon(Canvas canvas, float cx, float cy, float size) {
        linePaint.setShader(null);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(dp(3.2f));
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setColor(INK);
        float offset = Math.min(dp(6), size * 0.26f);
        canvas.drawLine(cx - size / 2f, cy - offset, cx + size / 2f, cy - offset, linePaint);
        canvas.drawLine(cx - size / 2f, cy + offset, cx + size / 2f, cy + offset, linePaint);
    }

    private String pretty(String raw) {
        return raw.replace('-', '\u2212');
    }

    private boolean isAllClear() {
        return tokens.isEmpty() && lastResultRaw == null && errorMsg == null;
    }

    private String joinExpression() {
        StringBuilder sb = new StringBuilder();
        for (String token : tokens) {
            sb.append(token);
        }
        return sb.toString();
    }

    private void clearLastState() {
        lastExpr = null;
        lastResultRaw = null;
        lastResultValue = 0;
        errorMsg = null;
        recalledFromHistory = false;
    }

    private void afterEdit() {
        errorMsg = null;
        recalledFromHistory = false;
        scrollToEnd = true;
        updatePreview();
        invalidate();
        notifyDisplayAccessibilityChanged();
    }

    private void addFormattedNumberTokens(String formatted, boolean wrapNegativeInParens) {
        if (formatted == null || formatted.isEmpty()) {
            return;
        }
        boolean neg = formatted.charAt(0) == '-' || formatted.charAt(0) == '\u2212';
        if (neg) {
            String mag = formatted.substring(1).replace('-', '\u2212');
            if (wrapNegativeInParens) {
                tokens.add("(");
                tokens.add("\u2212");
                tokens.add(mag);
                tokens.add(")");
            } else {
                tokens.add("\u2212");
                tokens.add(mag);
            }
        } else {
            tokens.add(formatted.replace('-', '\u2212'));
        }
    }

    private static boolean isPlainNumberExpression(String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }
        int i = 0;
        char first = s.charAt(0);
        if (first == '-' || first == '\u2212') {
            i++;
            if (i >= s.length()) {
                return false;
            }
        }
        boolean hasDigit = false;
        boolean hasDot = false;
        for (; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isDigit(c)) {
                hasDigit = true;
            } else if ((c == ',' || c == '.') && !hasDot) {
                hasDot = true;
            } else {
                return false;
            }
        }
        return hasDigit;
    }

    private void updatePreview() {
        if (errorMsg != null || tokens.isEmpty()) {
            cachedPreview = null;
            return;
        }
        String cleaned = CalcEngine.trimTrailingOperators(joinExpression());
        if (cleaned.isEmpty() || isPlainNumberExpression(cleaned)) {
            cachedPreview = null;
            return;
        }
        try {
            double v = CalcEngine.evaluate(cleaned, degrees);
            String formatted = pretty(CalcEngine.format(v));
            if (formatted.equals(pretty(cleaned))) {
                cachedPreview = null;
            } else {
                cachedPreview = formatted;
            }
        } catch (CalcEngine.Error e) {
            cachedPreview = null;
        }
    }

    private void appendFunction(String name) {
        if (tokens.isEmpty() && lastResultRaw != null) {
            clearLastState();
        }
        if (needsMultiplyBeforeValue()) {
            tokens.add("\u00d7");
        }
        tokens.add(name);
        afterEdit();
    }

    private boolean needsMultiplyBeforeValue() {
        if (tokens.isEmpty()) {
            return false;
        }
        String last = tokens.get(tokens.size() - 1);
        if (last.isEmpty()) {
            return false;
        }
        char c = last.charAt(last.length() - 1);
        return Character.isDigit(c) || c == ')' || c == '\u03c0' || c == 'e'
                || c == '!' || c == '\u00b2' || c == '%';
    }

    private void appendDigit(String digit) {
        if (tokens.isEmpty() && lastResultRaw != null) {
            clearLastState();
        }
        if (tokens.isEmpty()) {
            tokens.add(digit);
            afterEdit();
            return;
        }
        String last = tokens.get(tokens.size() - 1);
        if (isNumber(last)) {
            if (last.equals("0")) {
                if (!digit.equals("0")) {
                    tokens.set(tokens.size() - 1, digit);
                }
            } else {
                tokens.set(tokens.size() - 1, last + digit);
            }
        } else {
            if (needsMultiplyBeforeValue()) {
                tokens.add("\u00d7");
            }
            tokens.add(digit);
        }
        afterEdit();
    }

    private boolean isNumber(String s) {
        if (s.isEmpty()) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!Character.isDigit(c) && c != ',' && c != '.') {
                return false;
            }
        }
        return true;
    }

    private void appendDot() {
        if (tokens.isEmpty() && lastResultRaw != null) {
            clearLastState();
        }
        if (tokens.isEmpty()) {
            tokens.add("0,");
            afterEdit();
            return;
        }
        String last = tokens.get(tokens.size() - 1);
        if (isNumber(last)) {
            if (!last.contains(",") && !last.contains(".")) {
                tokens.set(tokens.size() - 1, last + ",");
            }
        } else {
            if (needsMultiplyBeforeValue()) {
                tokens.add("\u00d7");
            }
            tokens.add("0,");
        }
        afterEdit();
    }

    private void appendOperator(String op) {
        if (tokens.isEmpty() && lastResultRaw != null) {
            addFormattedNumberTokens(lastResultRaw, false);
            clearLastState();
        } else if (errorMsg != null) {
            errorMsg = null;
        }
        if (tokens.isEmpty()) {
            if (op.equals("\u2212")) {
                tokens.add(op);
            }
            afterEdit();
            return;
        }
        String last = tokens.get(tokens.size() - 1);
        if (op.equals("\u2212") && last.equals("\u2212")) {
            afterEdit();
            return;
        }
        if (op.equals("\u2212") && (last.equals("\u00d7") || last.equals("\u00f7"))) {
            tokens.add(op);
            afterEdit();
            return;
        }
        while (!tokens.isEmpty() && isOperatorToken(tokens.get(tokens.size() - 1))) {
            tokens.remove(tokens.size() - 1);
        }
        if (tokens.isEmpty()) {
            if (op.equals("\u2212")) {
                tokens.add(op);
            }
            afterEdit();
            return;
        }
        last = tokens.get(tokens.size() - 1);
        if (last.endsWith("(") || last.endsWith("^")) {
            if (op.equals("\u2212")) {
                tokens.add(op);
            }
            afterEdit();
            return;
        }
        tokens.add(op);
        afterEdit();
    }

    private boolean isOperatorToken(String s) {
        return s.equals("+") || s.equals("\u2212") || s.equals("-")
                || s.equals("\u00d7") || s.equals("\u00f7") || s.equals("*") || s.equals("/");
    }

    private void appendParen() {
        if (tokens.isEmpty() && lastResultRaw != null) {
            clearLastState();
        }
        errorMsg = null;
        int open = 0;
        int close = 0;
        for (String t : tokens) {
            for (int i = 0; i < t.length(); i++) {
                if (t.charAt(i) == '(') {
                    open++;
                } else if (t.charAt(i) == ')') {
                    close++;
                }
            }
        }
        boolean lastIsValue = false;
        if (!tokens.isEmpty()) {
            String last = tokens.get(tokens.size() - 1);
            if (!last.isEmpty()) {
                char c = last.charAt(last.length() - 1);
                lastIsValue = Character.isDigit(c) || c == ')' || c == '\u03c0' || c == 'e'
                        || c == '!' || c == '\u00b2' || c == '%';
            }
        }
        if (open > close && lastIsValue) {
            tokens.add(")");
        } else {
            tokens.add("(");
        }
        afterEdit();
    }

    private void appendPostfix(String token) {
        if (tokens.isEmpty()) {
            if (lastResultRaw != null) {
                addFormattedNumberTokens(lastResultRaw, true);
                clearLastState();
            } else {
                return;
            }
        }
        if (recalledFromHistory && tokens.size() == 2 && tokens.get(0).equals("\u2212")) {
            tokens.add(0, "(");
            tokens.add(")");
        }
        String last = tokens.get(tokens.size() - 1);
        if (isOperatorToken(last) || last.endsWith("(") || last.endsWith("^")) {
            return;
        }
        tokens.add(token);
        afterEdit();
    }

    private void backspace() {
        if (tokens.isEmpty()) {
            if (lastResultRaw != null || lastExpr != null || errorMsg != null) {
                clearLastState();
                cachedPreview = null;
                scrollToEnd = true;
                invalidate();
                notifyDisplayAccessibilityChanged();
            }
            return;
        }
        String last = tokens.get(tokens.size() - 1);
        if (isNumber(last) && last.length() > 1) {
            tokens.set(tokens.size() - 1, last.substring(0, last.length() - 1));
        } else {
            tokens.remove(tokens.size() - 1);
        }
        afterEdit();
    }

    private String errorText(CalcEngine.Error e) {
        switch (e.kind) {
            case DIV_ZERO:
                return str(R.string.err_div_zero);
            case DOMAIN:
                return str(R.string.err_domain);
            case UNDEFINED:
                return str(R.string.err_undefined);
            case FORMAT:
            default:
                return str(R.string.err_format);
        }
    }

    private void evaluateAndShow() {
        if (tokens.isEmpty()) {
            if (lastResultRaw != null) {
                errorMsg = null;
                invalidate();
            }
            return;
        }
        String cleaned = CalcEngine.trimTrailingOperators(joinExpression());
        if (cleaned.isEmpty()) {
            return;
        }
        try {
            double value = CalcEngine.evaluate(cleaned, degrees);
            String result = CalcEngine.format(value);
            String shownExpr = pretty(cleaned);
            history.add(shownExpr, pretty(result));
            lastExpr = shownExpr;
            lastResultRaw = result;
            lastResultValue = value;
            lastAnswer = value;
            hasAnswer = true;
            errorMsg = null;
            recalledFromHistory = false;
            tokens.clear();
            cachedPreview = null;
            scrollToEnd = true;
            performHapticFeedback(HapticFeedbackConstants.CONFIRM);
        } catch (CalcEngine.Error e) {
            errorMsg = errorText(e);
            cachedPreview = null;
        }
        invalidate();
        notifyDisplayAccessibilityChanged();
    }

    private double currentValue() {
        if (!tokens.isEmpty()) {
            try {
                return CalcEngine.evaluate(joinExpression(), degrees);
            } catch (CalcEngine.Error ignored) {
            }
        }
        if (lastResultRaw != null) {
            return lastResultValue;
        }
        return 0;
    }

    private void insertMemory() {
        if (!hasMemory) {
            return;
        }
        if (tokens.isEmpty() && lastResultRaw != null) {
            clearLastState();
        }
        if (needsMultiplyBeforeValue()) {
            tokens.add("\u00d7");
        }
        addFormattedNumberTokens(CalcEngine.format(memory), true);
        afterEdit();
    }

    private void copyDisplayedValue() {
        if (errorMsg != null) {
            return;
        }
        String value;
        if (tokens.isEmpty() && lastResultRaw != null) {
            value = lastResultRaw;
        } else if (cachedPreview != null) {
            value = cachedPreview;
        } else {
            String expr = joinExpression();
            value = expr.isEmpty() ? "0" : pretty(expr);
        }
        ClipboardManager cm = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText(str(R.string.app_name), value));
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            Toast.makeText(getContext(), R.string.toast_copied, Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleScientific(boolean closeOverlayFirst) {
        scientific = !scientific;
        history.setScientific(scientific);
        if (closeOverlayFirst) {
            closeOverlay();
        }
        layoutUi();
        notifyWindowContentChanged();
    }

    private void onButton(int id) {
        if (id >= ID_NUM && id < ID_NUM + 10) {
            appendDigit(String.valueOf(id - ID_NUM));
            return;
        }
        switch (id) {
            case ID_AC:
                if (isAllClear()) {
                    return;
                }
                tokens.clear();
                clearLastState();
                cachedPreview = null;
                scrollToEnd = true;
                invalidate();
                notifyDisplayAccessibilityChanged();
                break;
            case ID_PAREN:
                appendParen();
                break;
            case ID_PCT:
                appendPostfix("%");
                break;
            case ID_DIV:
                appendOperator("\u00f7");
                break;
            case ID_MUL:
                appendOperator("\u00d7");
                break;
            case ID_SUB:
                appendOperator("\u2212");
                break;
            case ID_ADD:
                appendOperator("+");
                break;
            case ID_DOT:
                appendDot();
                break;
            case ID_BACK:
                backspace();
                break;
            case ID_EQ:
                evaluateAndShow();
                break;
            case ID_INV:
                inverse = !inverse;
                invalidate();
                notifyWindowContentChanged();
                break;
            case ID_DEG:
                degrees = !degrees;
                history.setDegrees(degrees);
                afterEdit();
                notifyWindowContentChanged();
                break;
            case ID_SIN:
                appendFunction(inverse ? "sin\u207b\u00b9(" : "sin(");
                break;
            case ID_COS:
                appendFunction(inverse ? "cos\u207b\u00b9(" : "cos(");
                break;
            case ID_TAN:
                appendFunction(inverse ? "tan\u207b\u00b9(" : "tan(");
                break;
            case ID_LN:
                appendFunction("ln(");
                break;
            case ID_LOG:
                appendFunction("log(");
                break;
            case ID_EXP:
                appendFunction("e^");
                break;
            case ID_TENX:
                appendFunction("10^");
                break;
            case ID_SQRT:
                appendFunction("\u221a(");
                break;
            case ID_POW:
                appendPostfix("^");
                break;
            case ID_SQR:
                appendPostfix("\u00b2");
                break;
            case ID_FACT:
                appendPostfix("!");
                break;
            case ID_PI:
                if (tokens.isEmpty() && lastResultRaw != null) {
                    clearLastState();
                }
                if (needsMultiplyBeforeValue()) {
                    tokens.add("\u00d7");
                }
                tokens.add("\u03c0");
                afterEdit();
                break;
            case ID_E:
                if (tokens.isEmpty() && lastResultRaw != null) {
                    clearLastState();
                }
                if (needsMultiplyBeforeValue()) {
                    tokens.add("\u00d7");
                }
                tokens.add("e");
                afterEdit();
                break;
            case ID_MC:
                memory = 0;
                hasMemory = false;
                history.clearMemory();
                invalidate();
                break;
            case ID_MR:
                insertMemory();
                break;
            case ID_MPLUS:
                memory += currentValue();
                // Normalise -0.0 en +0.0
                if (memory == 0.0) {
                    memory = 0.0;
                }
                hasMemory = true;
                history.setMemory(memory);
                invalidate();
                break;
            case ID_MMINUS:
                memory -= currentValue();
                // Normalise -0.0 en +0.0
                if (memory == 0.0) {
                    memory = 0.0;
                }
                hasMemory = true;
                history.setMemory(memory);
                invalidate();
                break;
            case ID_ANS:
                if (!hasAnswer) {
                    return;
                }
                if (tokens.isEmpty() && lastResultRaw != null) {
                    clearLastState();
                }
                if (needsMultiplyBeforeValue()) {
                    tokens.add("\u00d7");
                }
                addFormattedNumberTokens(CalcEngine.format(lastAnswer), true);
                afterEdit();
                break;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (overlay != OVERLAY_NONE) {
            if (keyCode == KeyEvent.KEYCODE_ESCAPE) {
                closeOverlay();
                return true;
            }
            return super.onKeyDown(keyCode, event);
        }
        switch (keyCode) {
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
                onButton(ID_EQ);
                return true;
            case KeyEvent.KEYCODE_DEL:
                onButton(ID_BACK);
                return true;
            case KeyEvent.KEYCODE_ESCAPE:
            case KeyEvent.KEYCODE_FORWARD_DEL:
                onButton(ID_AC);
                return true;
        }
        int ch = event.getUnicodeChar();
        if (ch >= '0' && ch <= '9') {
            onButton(ID_NUM + (ch - '0'));
            return true;
        }
        switch (ch) {
            case ',':
            case '.':
                onButton(ID_DOT);
                return true;
            case '+':
                onButton(ID_ADD);
                return true;
            case '-':
            case '\u2212':
                onButton(ID_SUB);
                return true;
            case '*':
            case 'x':
            case 'X':
            case '\u00d7':
                onButton(ID_MUL);
                return true;
            case '/':
            case '\u00f7':
                onButton(ID_DIV);
                return true;
            case '%':
                onButton(ID_PCT);
                return true;
            case '(':
            case ')':
                onButton(ID_PAREN);
                return true;
            case '^':
                onButton(ID_POW);
                return true;
            case '=':
                onButton(ID_EQ);
                return true;
            case '!':
                onButton(ID_FACT);
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                longFired = false;
                if (overlay != OVERLAY_NONE) {
                    handleOverlayDown(event);
                    return true;
                }
                if (rDisplay.contains(x, y)) {
                    displayDragging = true;
                    displayMoved = false;
                    displayDragStartX = x;
                    displayDragStartScroll = mainScroll;
                    handler.postDelayed(longPressRunnable, 480);
                    return true;
                }
                if (rHistory.contains(x, y)) {
                    headerPressed = 1;
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    return true;
                }
                if (rFx.contains(x, y)) {
                    headerPressed = 2;
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    return true;
                }
                if (rOverflow.contains(x, y)) {
                    headerPressed = 3;
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    return true;
                }
                Btn hit = hitButton(x, y);
                if (hit != null) {
                    pressedId = hit.id;
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    if (hit.id == ID_AC || hit.id == ID_BACK) {
                        handler.postDelayed(longPressRunnable, 480);
                    }
                    invalidate();
                    return true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (overlay != OVERLAY_NONE) {
                    handleOverlayMove(event);
                    return true;
                }
                if (headerPressed != 0) {
                    if ((headerPressed == 1 && !rHistory.contains(x, y))
                            || (headerPressed == 2 && !rFx.contains(x, y))
                            || (headerPressed == 3 && !rOverflow.contains(x, y))) {
                        headerPressed = 0;
                    }
                    return true;
                }
                if (displayDragging) {
                    if (!displayMoved && Math.abs(x - displayDragStartX) > dp(6)) {
                        displayMoved = true;
                        handler.removeCallbacks(longPressRunnable);
                    }
                    float limit = Math.max(0, mainWidth - displayAvail);
                    mainScroll = Math.max(0, Math.min(limit, displayDragStartScroll + (x - displayDragStartX)));
                    invalidate();
                    return true;
                }
                if (pressedId != -1) {
                    Btn pressed = findButton(pressedId);
                    if (pressed != null) {
                        float slop = pressed.r * 0.35f;
                        if (x < pressed.rect.left - slop || x > pressed.rect.right + slop
                                || y < pressed.rect.top - slop || y > pressed.rect.bottom + slop) {
                            pressedId = -1;
                            handler.removeCallbacks(longPressRunnable);
                            invalidate();
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                handler.removeCallbacks(longPressRunnable);
                if (overlay != OVERLAY_NONE) {
                    handleOverlayUp(event);
                    return true;
                }
                if (headerPressed != 0) {
                    int which = headerPressed;
                    boolean inside = (which == 1 && rHistory.contains(x, y))
                            || (which == 2 && rFx.contains(x, y))
                            || (which == 3 && rOverflow.contains(x, y));
                    headerPressed = 0;
                    if (inside) {
                        if (which == 1) {
                            openOverlay(OVERLAY_HISTORY);
                        } else if (which == 2) {
                            toggleScientific(false);
                        } else {
                            openOverlay(OVERLAY_MENU);
                        }
                    }
                    return true;
                }
                if (displayDragging) {
                    displayDragging = false;
                    displayMoved = false;
                    return true;
                }
                if (pressedId != -1) {
                    Btn pressed = findButton(pressedId);
                    Btn up = hitButton(x, y);
                    if (pressed != null && up != null && up.id == pressed.id && !longFired) {
                        onButton(pressed.id);
                    }
                    pressedId = -1;
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                handler.removeCallbacks(longPressRunnable);
                if (overlay != OVERLAY_NONE) {
                    finishOverlayTouch();
                    return true;
                }
                pressedId = -1;
                headerPressed = 0;
                displayDragging = false;
                displayMoved = false;
                historyDragging = false;
                recycleVelocityTracker();
                invalidate();
                break;
        }
        return true;
    }

    private Btn hitButton(float x, float y) {
        for (Btn b : buttons) {
            if (b.circle) {
                float slop = keypadGap / 2f;
                if (x >= b.rect.left - slop && x <= b.rect.right + slop
                        && y >= b.rect.top - slop && y <= b.rect.bottom + slop) {
                    return b;
                }
            } else if (b.rect.contains(x, y)) {
                return b;
            }
        }
        return null;
    }

    private Btn findButton(int id) {
        for (Btn b : buttons) {
            if (b.id == id) {
                return b;
            }
        }
        return null;
    }

    private void onLongPress() {
        if (displayDragging && !displayMoved) {
            longFired = true;
            displayDragging = false;
            copyDisplayedValue();
            return;
        }
        if (pressedId == ID_AC) {
            pressedId = -1;
            if (!isAllClear() || !history.items.isEmpty() || hasMemory || hasAnswer) {
                longFired = true;
                confirmResetAll = true;
                openOverlay(OVERLAY_CONFIRM);
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            } else {
                invalidate();
            }
        } else if (pressedId == ID_BACK) {
            longFired = true;
            pressedId = -1;
            if (!tokens.isEmpty() || !isAllClear()) {
                tokens.clear();
                clearLastState();
                cachedPreview = null;
                scrollToEnd = true;
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                invalidate();
                notifyDisplayAccessibilityChanged();
            }
        }
    }

    private void openOverlay(int which) {
        overlay = which;
        overlayAnim = 0f;
        overlayDownTarget = TARGET_NONE;
        historyDragging = false;
        recycleVelocityTracker();
        if (!historyScroller.isFinished()) {
            historyScroller.abortAnimation();
        }
        if (overlayAnimator != null) {
            overlayAnimator.cancel();
        }
        overlayAnimator = ValueAnimator.ofFloat(0f, 1f);
        overlayAnimator.setDuration(which == OVERLAY_HISTORY ? 220 : 170);
        overlayAnimator.setInterpolator(new DecelerateInterpolator());
        overlayAnimator.addUpdateListener(a -> {
            overlayAnim = (float) a.getAnimatedValue();
            invalidate();
        });
        overlayAnimator.start();
        if (which == OVERLAY_HISTORY) {
            historyScroll = 0;
            refreshHistoryBounds();
            historyMoved = false;
        }
        if (overlayChangedListener != null) {
            overlayChangedListener.onOverlayChanged(true);
        }
        invalidate();
        notifyWindowContentChanged();
    }

    private void closeOverlay() {
        overlay = OVERLAY_NONE;
        pressedId = -1;
        overlayDownTarget = TARGET_NONE;
        historyDragging = false;
        recycleVelocityTracker();
        if (!historyScroller.isFinished()) {
            historyScroller.abortAnimation();
        }
        if (overlayAnimator != null) {
            overlayAnimator.cancel();
            overlayAnimator = null;
        }
        if (overlayChangedListener != null) {
            overlayChangedListener.onOverlayChanged(false);
        }
        invalidate();
        notifyWindowContentChanged();
    }

    private void refreshHistoryBounds() {
        float margin = dp(10);
        historySheet.set(contentLeft + margin, padTop + dp(8),
                contentLeft + contentW - margin, H - padBottom - dp(10));
        float header = dp(66);
        historyList.set(historySheet.left + dp(8), historySheet.top + header,
                historySheet.right - dp(8), historySheet.bottom - dp(10));
        float closeR = dp(40);
        rHistClose.set(historySheet.right - dp(16) - closeR, historySheet.top + dp(13),
                historySheet.right - dp(16), historySheet.top + dp(13) + closeR);
        float clearW = dp(72);
        rHistClear.set(rHistClose.left - dp(10) - clearW, historySheet.top + dp(13),
                rHistClose.left - dp(10), historySheet.top + dp(13) + closeR);
        historyMaxScroll = Math.max(0, history.items.size() * dp(76) - historyList.height());
        if (historyScroll > historyMaxScroll) {
            historyScroll = historyMaxScroll;
        }
        if (historyScroll < 0) {
            historyScroll = 0;
        }
    }

    private int hitOverlayTarget(float x, float y) {
        if (overlay == OVERLAY_HISTORY) {
            if (rHistClose.contains(x, y)) {
                return TARGET_HIST_CLOSE;
            }
            if (!history.items.isEmpty() && rHistClear.contains(x, y)) {
                return TARGET_HIST_CLEAR;
            }
            if (historyList.contains(x, y)) {
                return TARGET_HIST_LIST;
            }
            if (!historySheet.contains(x, y)) {
                return TARGET_HIST_OUTSIDE;
            }
            return TARGET_NONE;
        }
        if (overlay == OVERLAY_MENU) {
            if (menuClear.contains(x, y)) {
                return TARGET_MENU_CLEAR;
            }
            if (menuSci.contains(x, y)) {
                return TARGET_MENU_SCI;
            }
            if (menuAbout.contains(x, y)) {
                return TARGET_MENU_ABOUT;
            }
            if (!menuCard.contains(x, y)) {
                return TARGET_MENU_OUTSIDE;
            }
            return TARGET_NONE;
        }
        if (overlay == OVERLAY_CONFIRM) {
            if (confirmOk.contains(x, y)) {
                return TARGET_CONFIRM_OK;
            }
            if (confirmCancel.contains(x, y)) {
                return TARGET_CONFIRM_CANCEL;
            }
            if (!confirmCard.contains(x, y)) {
                return TARGET_CONFIRM_OUTSIDE;
            }
            return TARGET_NONE;
        }
        if (overlay == OVERLAY_ABOUT) {
            if (aboutClose.contains(x, y)) {
                return TARGET_ABOUT_CLOSE;
            }
            if (!aboutCard.contains(x, y)) {
                return TARGET_ABOUT_OUTSIDE;
            }
            return TARGET_NONE;
        }
        return TARGET_NONE;
    }

    private void handleOverlayDown(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        overlayDownTarget = hitOverlayTarget(x, y);
        if (overlay == OVERLAY_HISTORY && overlayDownTarget == TARGET_HIST_LIST) {
            if (!historyScroller.isFinished()) {
                historyScroller.abortAnimation();
            }
            historyDragging = true;
            historyDownY = y;
            historyStartScroll = historyScroll;
            historyMoved = false;
            if (velocityTracker == null) {
                velocityTracker = VelocityTracker.obtain();
            } else {
                velocityTracker.clear();
            }
            velocityTracker.addMovement(event);
        }
    }

    private void handleOverlayMove(MotionEvent event) {
        if (overlay == OVERLAY_HISTORY && historyDragging) {
            if (velocityTracker != null) {
                velocityTracker.addMovement(event);
            }
            float delta = historyDownY - event.getY();
            if (Math.abs(delta) > dp(5)) {
                historyMoved = true;
            }
            historyScroll = Math.max(0, Math.min(historyMaxScroll, historyStartScroll + delta));
            invalidate();
        }
    }

    private void handleOverlayUp(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        int upTarget = hitOverlayTarget(x, y);
        int downTarget = overlayDownTarget;
        overlayDownTarget = TARGET_NONE;

        if (overlay == OVERLAY_HISTORY) {
            if (historyDragging) {
                if (historyMoved) {
                    if (velocityTracker != null && historyMaxScroll > 0) {
                        velocityTracker.addMovement(event);
                        velocityTracker.computeCurrentVelocity(1000);
                        float vy = velocityTracker.getYVelocity();
                        if (Math.abs(vy) > dp(120)) {
                            historyScroller.fling(0, Math.round(historyScroll), 0, Math.round(-vy),
                                    0, 0, 0, (int) Math.ceil(historyMaxScroll));
                            postInvalidateOnAnimation();
                        }
                    }
                } else if (upTarget == TARGET_HIST_LIST && !history.items.isEmpty()) {
                    int index = (int) ((y - historyList.top + historyScroll) / dp(76));
                    if (index >= 0 && index < history.items.size()) {
                        recallHistoryEntry(index);
                        return;
                    }
                }
                historyDragging = false;
                recycleVelocityTracker();
                return;
            }
            if (downTarget == upTarget) {
                if (upTarget == TARGET_HIST_CLOSE || upTarget == TARGET_HIST_OUTSIDE) {
                    closeOverlay();
                } else if (upTarget == TARGET_HIST_CLEAR && !history.items.isEmpty()) {
                    confirmResetAll = false;
                    openOverlay(OVERLAY_CONFIRM);
                }
            }
            return;
        }
        if (downTarget != upTarget) {
            return;
        }
        if (overlay == OVERLAY_MENU) {
            if (upTarget == TARGET_MENU_CLEAR && !history.items.isEmpty()) {
                confirmResetAll = false;
                openOverlay(OVERLAY_CONFIRM);
            } else if (upTarget == TARGET_MENU_SCI) {
                toggleScientific(true);
            } else if (upTarget == TARGET_MENU_ABOUT) {
                openOverlay(OVERLAY_ABOUT);
            } else if (upTarget == TARGET_MENU_OUTSIDE) {
                closeOverlay();
            }
            return;
        }
        if (overlay == OVERLAY_CONFIRM) {
            if (upTarget == TARGET_CONFIRM_OK) {
                performConfirmedClear();
            } else if (upTarget == TARGET_CONFIRM_CANCEL || upTarget == TARGET_CONFIRM_OUTSIDE) {
                closeOverlay();
            }
            return;
        }
        if (overlay == OVERLAY_ABOUT) {
            if (upTarget == TARGET_ABOUT_CLOSE || upTarget == TARGET_ABOUT_OUTSIDE) {
                closeOverlay();
            }
        }
    }

    private void recallHistoryEntry(int index) {
        HistoryStore.Entry entry = history.items.get(index);
        tokens.clear();
        clearLastState();
        addFormattedNumberTokens(entry.result, false);
        scrollToEnd = true;
        historyDragging = false;
        recycleVelocityTracker();
        closeOverlay();
        afterEdit();
        recalledFromHistory = true;
    }

    private void performConfirmedClear() {
        history.clear();
        refreshHistoryBounds();
        if (confirmResetAll) {
            memory = 0;
            hasMemory = false;
            history.clearMemory();
            lastAnswer = 0;
            hasAnswer = false;
            tokens.clear();
            clearLastState();
            cachedPreview = null;
            scrollToEnd = true;
        }
        closeOverlay();
        notifyDisplayAccessibilityChanged();
    }

    private void finishOverlayTouch() {
        historyDragging = false;
        overlayDownTarget = TARGET_NONE;
        recycleVelocityTracker();
    }

    private void recycleVelocityTracker() {
        if (velocityTracker != null) {
            velocityTracker.recycle();
            velocityTracker = null;
        }
    }

    @Override
    public void computeScroll() {
        if (historyScroller.computeScrollOffset()) {
            historyScroll = Math.max(0, Math.min(historyMaxScroll, historyScroller.getCurrY()));
            postInvalidateOnAnimation();
        }
    }

    private void drawOverlay(Canvas canvas) {
        dimPaint.setShader(null);
        dimPaint.setColor(Color.argb((int) (150 * overlayAnim), 4, 0, 14));
        canvas.drawRect(0, 0, W, H, dimPaint);

        if (overlay == OVERLAY_HISTORY) {
            drawHistory(canvas);
        } else if (overlay == OVERLAY_MENU) {
            drawMenu(canvas);
        } else if (overlay == OVERLAY_CONFIRM) {
            drawConfirm(canvas);
        } else if (overlay == OVERLAY_ABOUT) {
            drawAbout(canvas);
        }
    }

    private void drawHistory(Canvas canvas) {
        float offset = (1f - overlayAnim) * dp(46);
        canvas.save();
        canvas.translate(0, offset);
        drawCard(canvas, historySheet, dp(28), 0xFF16073E, 0x88000000);

        textPaint.setTypeface(semibold);
        textPaint.setTextSize(sp(20));
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(WHITE);
        canvas.drawText(str(R.string.history_title), historySheet.left + dp(24), historySheet.top + dp(40), textPaint);

        if (!history.items.isEmpty()) {
            textPaint.setTypeface(medium);
            textPaint.setTextSize(sp(13));
            textPaint.setTextAlign(Paint.Align.RIGHT);
            textPaint.setColor(PINK);
            canvas.drawText(str(R.string.action_clear), rHistClear.right, historySheet.top + dp(39), textPaint);
        }

        drawCloseIcon(canvas, rHistClose.centerX(), rHistClose.centerY());

        canvas.save();
        canvas.clipRect(historyList);
        if (history.items.isEmpty()) {
            float cy = historyList.centerY() - dp(40);
            drawMascot(canvas, historyList.centerX(), cy + dp(6), dp(68), 0.38f);
            textPaint.setTypeface(medium);
            textPaint.setTextSize(sp(17));
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setColor(LAVENDER);
            canvas.drawText(str(R.string.history_empty_title), historyList.centerX(), cy + dp(60), textPaint);
            textPaint.setTypeface(regular);
            textPaint.setTextSize(sp(13.5f));
            textPaint.setColor(MUTED_DARK);
            canvas.drawText(str(R.string.history_empty_subtitle), historyList.centerX(), cy + dp(82), textPaint);
        } else {
            int count = history.items.size();
            float rowH = dp(76);
            float right = historyList.right - dp(16);
            float maxRowW = Math.max(dp(40), historyList.width() - dp(32));
            for (int i = 0; i < count; i++) {
                HistoryStore.Entry entry = history.items.get(i);
                float top = historyList.top + i * rowH - historyScroll;
                if (top > historyList.bottom || top + rowH < historyList.top) {
                    continue;
                }
                textPaint.setTypeface(regular);
                textPaint.setColor(MUTED);
                drawRightAlignedText(canvas, entry.expression, right, top + dp(28), maxRowW, sp(15), sp(10));
                textPaint.setTypeface(semibold);
                textPaint.setColor(WHITE);
                drawRightAlignedText(canvas, entry.result, right, top + dp(58), maxRowW, sp(25), sp(13));
                if (i < count - 1) {
                    linePaint.setShader(null);
                    linePaint.setStyle(Paint.Style.STROKE);
                    linePaint.setStrokeWidth(1);
                    linePaint.setColor(0x12FFFFFF);
                    canvas.drawLine(historyList.left + dp(12), top + rowH - dp(1),
                            historyList.right - dp(12), top + rowH - dp(1), linePaint);
                }
            }
            if (historyMaxScroll > 0) {
                float contentH = count * rowH;
                float viewH = historyList.height();
                float thumbH = Math.max(dp(24), viewH * (viewH / contentH));
                float thumbTravel = Math.max(0, viewH - thumbH);
                float thumbTop = historyList.top + (historyScroll / historyMaxScroll) * thumbTravel;
                float trackRight = historyList.right - dp(4);
                tmpRect.set(trackRight - dp(3), thumbTop, trackRight, thumbTop + thumbH);
                buttonPaint.setShader(null);
                buttonPaint.setStyle(Paint.Style.FILL);
                buttonPaint.setColor(0x44FFFFFF);
                canvas.drawRoundRect(tmpRect, dp(1.5f), dp(1.5f), buttonPaint);
            }
        }
        canvas.restore();
        canvas.restore();
    }

    private void drawMenu(Canvas canvas) {
        drawCard(canvas, menuCard, dp(22), 0xFF1B0C48, 0x99000000);
        canvas.save();
        canvas.clipRect(menuCard);
        drawMenuItem(canvas, menuClear, str(R.string.menu_clear_history), 0, !history.items.isEmpty());
        drawMenuItem(canvas, menuSci, str(R.string.menu_scientific), 1, true);
        drawMenuItem(canvas, menuAbout, str(R.string.menu_about), 2, true);
        linePaint.setShader(null);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(1);
        linePaint.setColor(0x14FFFFFF);
        canvas.drawLine(menuCard.left + dp(20), menuClear.bottom, menuCard.right - dp(20), menuClear.bottom, linePaint);
        canvas.drawLine(menuCard.left + dp(20), menuSci.bottom, menuCard.right - dp(20), menuSci.bottom, linePaint);
        canvas.restore();
    }

    private void drawMenuItem(Canvas canvas, RectF rect, String label, int icon, boolean enabled) {
        float cy = rect.centerY();
        int color = enabled ? LAVENDER : MUTED_DARK;
        drawMenuIcon(canvas, rect.left + dp(30), cy, icon, color);
        textPaint.setTypeface(medium);
        textPaint.setTextSize(sp(15));
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(color);
        canvas.drawText(label, rect.left + dp(52), cy + dp(5), textPaint);
        if (icon == 1 && scientific) {
            drawCheck(canvas, rect.right - dp(28), cy);
        }
    }

    private void drawMenuIcon(Canvas canvas, float cx, float cy, int icon, int color) {
        linePaint.setShader(null);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(dp(1.7f));
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setColor(color);
        if (icon == 0) {
            tmpRect.set(cx - dp(6), cy - dp(5), cx + dp(6), cy + dp(8));
            canvas.drawRoundRect(tmpRect, dp(3), dp(3), linePaint);
            canvas.drawLine(cx - dp(8), cy - dp(5), cx + dp(8), cy - dp(5), linePaint);
            canvas.drawLine(cx - dp(3), cy - dp(8), cx + dp(3), cy - dp(8), linePaint);
            canvas.drawLine(cx - dp(2), cy - dp(1), cx - dp(2), cy + dp(4), linePaint);
            canvas.drawLine(cx + dp(2), cy - dp(1), cx + dp(2), cy + dp(4), linePaint);
        } else if (icon == 1) {
            textPaint.setTypeface(medium);
            textPaint.setTextSize(sp(14));
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setColor(color);
            canvas.drawText("fx", cx, cy + dp(5), textPaint);
        } else {
            tmpRect.set(cx - dp(8), cy - dp(8), cx + dp(8), cy + dp(8));
            canvas.drawOval(tmpRect, linePaint);
            textPaint.setTypeface(medium);
            textPaint.setTextSize(sp(12));
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setColor(color);
            canvas.drawText("i", cx, cy + dp(4.2f), textPaint);
        }
    }

    private void drawCheck(Canvas canvas, float cx, float cy) {
        linePaint.setShader(null);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(dp(2.2f));
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setColor(PINK);
        tmpPath.reset();
        tmpPath.moveTo(cx - dp(6), cy);
        tmpPath.lineTo(cx - dp(1), cy + dp(5));
        tmpPath.lineTo(cx + dp(7), cy - dp(5));
        canvas.drawPath(tmpPath, linePaint);
    }

    private void drawConfirm(Canvas canvas) {
        float scale = 0.94f + 0.06f * overlayAnim;
        canvas.save();
        canvas.scale(scale, scale, W / 2f, H / 2f);
        drawCard(canvas, confirmCard, dp(24), 0xFF1B0C48, 0x99000000);
        textPaint.setTypeface(semibold);
        textPaint.setTextSize(sp(19));
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(WHITE);
        canvas.drawText(str(confirmResetAll ? R.string.confirm_reset_title : R.string.confirm_history_title),
                confirmCard.left + dp(24), confirmCard.top + dp(48), textPaint);
        textPaint.setTypeface(regular);
        textPaint.setTextSize(sp(14));
        textPaint.setColor(MUTED);
        canvas.drawText(str(confirmResetAll ? R.string.confirm_reset_line1 : R.string.confirm_history_line1),
                confirmCard.left + dp(24), confirmCard.top + dp(78), textPaint);
        canvas.drawText(str(confirmResetAll ? R.string.confirm_reset_line2 : R.string.confirm_history_line2),
                confirmCard.left + dp(24), confirmCard.top + dp(98), textPaint);

        textPaint.setTypeface(medium);
        textPaint.setTextSize(sp(14.5f));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(LAVENDER);
        canvas.drawText(str(R.string.action_cancel), confirmCancel.centerX(), confirmCancel.centerY() + dp(5), textPaint);

        buttonPaint.setShader(null);
        buttonPaint.setStyle(Paint.Style.FILL);
        buttonPaint.setColor(PINK);
        canvas.drawRoundRect(confirmOk, dp(23), dp(23), buttonPaint);
        textPaint.setTypeface(semibold);
        textPaint.setColor(INK);
        canvas.drawText(str(confirmResetAll ? R.string.action_reset : R.string.action_clear),
                confirmOk.centerX(), confirmOk.centerY() + dp(5), textPaint);
        canvas.restore();
    }

    private void drawAbout(Canvas canvas) {
        float scale = 0.94f + 0.06f * overlayAnim;
        canvas.save();
        canvas.scale(scale, scale, W / 2f, H / 2f);
        drawCard(canvas, aboutCard, dp(26), 0xFF1B0C48, 0x99000000);
        float cx = aboutCard.centerX();
        float hRatio = Math.min(1f, aboutCard.height() / dp(352));
        drawMascot(canvas, cx, aboutCard.top + dp(71) * hRatio, dp(75) * hRatio, 1f);
        textPaint.setTypeface(semibold);
        textPaint.setTextSize(sp(24));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(WHITE);
        canvas.drawText(str(R.string.app_name), cx, aboutCard.top + dp(130) * hRatio, textPaint);
        textPaint.setTypeface(regular);
        textPaint.setTextSize(sp(12.5f));
        textPaint.setColor(MUTED);
        canvas.drawText(str(R.string.about_version), cx, aboutCard.top + dp(152) * hRatio, textPaint);

        textPaint.setTextSize(sp(14));
        textPaint.setColor(LAVENDER);
        canvas.drawText(str(R.string.about_desc_line1), cx, aboutCard.top + dp(194) * hRatio, textPaint);
        canvas.drawText(str(R.string.about_desc_line2), cx, aboutCard.top + dp(214) * hRatio, textPaint);
        textPaint.setTextSize(sp(12.5f));
        textPaint.setColor(MUTED);
        canvas.drawText(str(R.string.about_features), cx, aboutCard.top + dp(250) * hRatio, textPaint);

        buttonPaint.setShader(null);
        buttonPaint.setStyle(Paint.Style.FILL);
        buttonPaint.setColor(PINK);
        canvas.drawRoundRect(aboutClose, dp(23), dp(23), buttonPaint);
        textPaint.setTypeface(semibold);
        textPaint.setTextSize(sp(14.5f));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(INK);
        canvas.drawText(str(R.string.action_close), aboutClose.centerX(), aboutClose.centerY() + dp(5), textPaint);
        canvas.restore();
    }

    private void drawCard(Canvas canvas, RectF rect, float radius, int fill, int shadow) {
        cardPaint.setShader(null);
        cardPaint.setStyle(Paint.Style.FILL);
        cardPaint.setColor(shadow);
        tmpShadowRect.set(rect);
        tmpShadowRect.offset(0, dp(8));
        canvas.drawRoundRect(tmpShadowRect, radius, radius, cardPaint);
        cardPaint.setColor(fill);
        canvas.drawRoundRect(rect, radius, radius, cardPaint);
        strokePaint.setShader(null);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(1);
        strokePaint.setColor(0x1AFFFFFF);
        canvas.drawRoundRect(rect, radius, radius, strokePaint);
    }

    private void drawCloseIcon(Canvas canvas, float cx, float cy) {
        linePaint.setShader(null);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(dp(2));
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setColor(MUTED);
        canvas.drawLine(cx - dp(8), cy - dp(8), cx + dp(8), cy + dp(8), linePaint);
        canvas.drawLine(cx - dp(8), cy + dp(8), cx + dp(8), cy - dp(8), linePaint);
    }

    boolean handleBack() {
        if (overlay != OVERLAY_NONE) {
            closeOverlay();
            return true;
        }
        if (displayDragging) {
            displayDragging = false;
            displayMoved = false;
            return true;
        }
        return false;
    }

    void saveState(Bundle out) {
        out.putStringArrayList("tokens", new ArrayList<>(tokens));
        out.putString("lastExpr", lastExpr);
        out.putString("lastResultRaw", lastResultRaw);
        out.putDouble("lastResultValue", lastResultValue);
        out.putString("errorMsg", errorMsg);
        out.putBoolean("degrees", degrees);
        out.putBoolean("inverse", inverse);
        out.putBoolean("scientific", scientific);
        out.putDouble("memory", memory);
        out.putBoolean("hasMemory", hasMemory);
        out.putDouble("lastAnswer", lastAnswer);
        out.putBoolean("hasAnswer", hasAnswer);
    }

    void restoreState(Bundle in) {
        ArrayList<String> saved = in.getStringArrayList("tokens");
        if (saved != null) {
            tokens.clear();
            tokens.addAll(saved);
        }
        lastExpr = in.getString("lastExpr");
        lastResultRaw = in.getString("lastResultRaw");
        lastResultValue = in.getDouble("lastResultValue");
        errorMsg = in.getString("errorMsg");
        degrees = in.getBoolean("degrees");
        inverse = in.getBoolean("inverse");
        scientific = in.getBoolean("scientific");
        memory = in.getDouble("memory");
        hasMemory = in.getBoolean("hasMemory");
        lastAnswer = in.getDouble("lastAnswer");
        hasAnswer = in.getBoolean("hasAnswer");
        recalledFromHistory = false;
        scrollToEnd = true;
        updatePreview();
        layoutUi();
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        handler.removeCallbacks(longPressRunnable);
        recycleVelocityTracker();
        if (overlayAnimator != null) {
            overlayAnimator.cancel();
        }
    }

    private String contentDescriptionForButton(Btn b) {
        if (b.id >= ID_NUM && b.id < ID_NUM + 10) {
            return String.valueOf(b.id - ID_NUM);
        }
        switch (b.id) {
            case ID_AC:
                return str(isAllClear() ? R.string.cd_all_clear : R.string.cd_clear);
            case ID_PAREN:
                return str(R.string.cd_paren);
            case ID_PCT:
                return str(R.string.cd_percent);
            case ID_DIV:
                return str(R.string.cd_div);
            case ID_MUL:
                return str(R.string.cd_mul);
            case ID_SUB:
                return str(R.string.cd_sub);
            case ID_ADD:
                return str(R.string.cd_add);
            case ID_DOT:
                return str(R.string.cd_dot);
            case ID_BACK:
                return str(R.string.cd_back);
            case ID_EQ:
                return str(R.string.cd_eq);
            case ID_INV:
                return str(R.string.cd_inv);
            case ID_DEG:
                return str(degrees ? R.string.cd_deg : R.string.cd_rad);
            case ID_SIN:
                return str(inverse ? R.string.cd_asin : R.string.cd_sin);
            case ID_COS:
                return str(inverse ? R.string.cd_acos : R.string.cd_cos);
            case ID_TAN:
                return str(inverse ? R.string.cd_atan : R.string.cd_tan);
            case ID_LN:
                return str(R.string.cd_ln);
            case ID_LOG:
                return str(R.string.cd_log);
            case ID_EXP:
                return str(R.string.cd_exp);
            case ID_TENX:
                return str(R.string.cd_tenx);
            case ID_POW:
                return str(R.string.cd_pow);
            case ID_SQRT:
                return str(R.string.cd_sqrt);
            case ID_SQR:
                return str(R.string.cd_sqr);
            case ID_FACT:
                return str(R.string.cd_fact);
            case ID_PI:
                return str(R.string.cd_pi);
            case ID_E:
                return str(R.string.cd_e);
            case ID_MC:
                return str(R.string.cd_mc);
            case ID_MR:
                return str(R.string.cd_mr);
            case ID_MPLUS:
                return str(R.string.cd_mplus);
            case ID_MMINUS:
                return str(R.string.cd_mminus);
            case ID_ANS:
                return str(R.string.cd_ans);
        }
        return "";
    }

    @SuppressWarnings("deprecation")
    private static AccessibilityNodeInfo obtainNodeInfo() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return new AccessibilityNodeInfo();
        }
        return AccessibilityNodeInfo.obtain();
    }

    @SuppressWarnings("deprecation")
    private static AccessibilityNodeInfo obtainNodeInfo(View source) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return new AccessibilityNodeInfo(source);
        }
        return AccessibilityNodeInfo.obtain(source);
    }

    @SuppressWarnings("deprecation")
    private static AccessibilityEvent obtainAccessibilityEvent(int eventType) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return new AccessibilityEvent(eventType);
        }
        return AccessibilityEvent.obtain(eventType);
    }

    @SuppressWarnings("deprecation")
    private static void setBoundsInParentCompat(AccessibilityNodeInfo node, Rect bounds) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            node.setBoundsInParent(bounds);
        }
    }

    private void notifyWindowContentChanged() {
        if (accessibilityManager == null || !accessibilityManager.isEnabled()) {
            return;
        }
        AccessibilityEvent ev = obtainAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);
        ev.setPackageName(getContext().getPackageName());
        ev.setClassName(CalculatorView.class.getName());
        ev.setSource(this);
        ViewParent parent = getParent();
        if (parent != null) {
            parent.requestSendAccessibilityEvent(this, ev);
        }
    }

    private void notifyDisplayAccessibilityChanged() {
        if (accessibilityManager == null || !accessibilityManager.isEnabled() || overlay != OVERLAY_NONE) {
            return;
        }
        sendAccessibilityEventForVirtualView(V_DISPLAY, AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);
    }

    private void sendAccessibilityEventForVirtualView(int virtualViewId, int eventType) {
        if (accessibilityManager == null || !accessibilityManager.isEnabled()) {
            return;
        }
        AccessibilityEvent event = obtainAccessibilityEvent(eventType);
        event.setPackageName(getContext().getPackageName());
        event.setClassName(CalculatorView.class.getName());
        event.setSource(this, virtualViewId);
        CharSequence text = descriptionForVirtualView(virtualViewId);
        if (text != null) {
            event.getText().add(text);
        }
        ViewParent parent = getParent();
        if (parent != null) {
            parent.requestSendAccessibilityEvent(this, event);
        }
    }

    private CharSequence descriptionForVirtualView(int virtualViewId) {
        switch (virtualViewId) {
            case V_DISPLAY:
                return getContext().getString(R.string.cd_display, spokenDisplayText());
            case V_HIST_ICON:
                return str(R.string.cd_history);
            case V_FX_ICON:
                return str(R.string.cd_scientific);
            case V_MENU_ICON:
                return str(R.string.cd_menu);
            case V_HIST_CLEAR:
                return str(R.string.action_clear);
            case V_HIST_CLOSE:
            case V_ABOUT_CLOSE:
                return str(R.string.cd_close);
            case V_MENU_CLEAR:
                return str(R.string.menu_clear_history);
            case V_MENU_SCI:
                return str(R.string.menu_scientific);
            case V_MENU_ABOUT:
                return str(R.string.menu_about);
            case V_CONFIRM_CANCEL:
                return str(R.string.action_cancel);
            case V_CONFIRM_OK:
                return str(confirmResetAll ? R.string.action_reset : R.string.action_clear);
        }
        if (virtualViewId >= V_BTN_BASE && virtualViewId < V_HIST_CLEAR) {
            Btn b = findButton(virtualViewId - V_BTN_BASE);
            return b != null ? contentDescriptionForButton(b) : null;
        }
        if (virtualViewId >= V_HIST_ITEM_BASE) {
            int idx = virtualViewId - V_HIST_ITEM_BASE;
            if (idx >= 0 && idx < history.items.size()) {
                HistoryStore.Entry entry = history.items.get(idx);
                return entry.expression + " = " + entry.result;
            }
        }
        return null;
    }

    private int hitVirtualView(float x, float y) {
        if (overlay == OVERLAY_NONE) {
            if (rDisplay.contains(x, y)) {
                return V_DISPLAY;
            }
            if (rHistory.contains(x, y)) {
                return V_HIST_ICON;
            }
            if (rFx.contains(x, y)) {
                return V_FX_ICON;
            }
            if (rOverflow.contains(x, y)) {
                return V_MENU_ICON;
            }
            Btn b = hitButton(x, y);
            return b != null ? V_BTN_BASE + b.id : View.NO_ID;
        }
        if (overlay == OVERLAY_HISTORY) {
            if (rHistClose.contains(x, y)) {
                return V_HIST_CLOSE;
            }
            if (!history.items.isEmpty() && rHistClear.contains(x, y)) {
                return V_HIST_CLEAR;
            }
            if (historyList.contains(x, y) && !history.items.isEmpty()) {
                int idx = (int) ((y - historyList.top + historyScroll) / dp(76));
                if (idx >= 0 && idx < history.items.size()) {
                    return V_HIST_ITEM_BASE + idx;
                }
            }
            return View.NO_ID;
        }
        if (overlay == OVERLAY_MENU) {
            if (menuClear.contains(x, y)) {
                return V_MENU_CLEAR;
            }
            if (menuSci.contains(x, y)) {
                return V_MENU_SCI;
            }
            if (menuAbout.contains(x, y)) {
                return V_MENU_ABOUT;
            }
            return View.NO_ID;
        }
        if (overlay == OVERLAY_CONFIRM) {
            if (confirmCancel.contains(x, y)) {
                return V_CONFIRM_CANCEL;
            }
            if (confirmOk.contains(x, y)) {
                return V_CONFIRM_OK;
            }
            return View.NO_ID;
        }
        if (overlay == OVERLAY_ABOUT) {
            if (aboutClose.contains(x, y)) {
                return V_ABOUT_CLOSE;
            }
        }
        return View.NO_ID;
    }

    private void updateHoveredVirtualId(int virtualId) {
        if (hoveredVirtualId == virtualId) {
            return;
        }
        int prev = hoveredVirtualId;
        hoveredVirtualId = virtualId;
        if (prev != View.NO_ID) {
            sendAccessibilityEventForVirtualView(prev, AccessibilityEvent.TYPE_VIEW_HOVER_EXIT);
        }
        if (virtualId != View.NO_ID) {
            sendAccessibilityEventForVirtualView(virtualId, AccessibilityEvent.TYPE_VIEW_HOVER_ENTER);
        }
    }

    @Override
    protected boolean dispatchHoverEvent(MotionEvent event) {
        if (accessibilityManager != null && accessibilityManager.isTouchExplorationEnabled()) {
            int virtualId = hitVirtualView(event.getX(), event.getY());
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_HOVER_ENTER:
                case MotionEvent.ACTION_HOVER_MOVE:
                    updateHoveredVirtualId(virtualId);
                    return true;
                case MotionEvent.ACTION_HOVER_EXIT:
                    updateHoveredVirtualId(View.NO_ID);
                    return true;
            }
        }
        return super.dispatchHoverEvent(event);
    }

    @Override
    public AccessibilityNodeProvider getAccessibilityNodeProvider() {
        if (a11yProvider == null) {
            a11yProvider = new CalcAccessibilityProvider();
        }
        return a11yProvider;
    }

    private final class CalcAccessibilityProvider extends AccessibilityNodeProvider {

        @Override
        public AccessibilityNodeInfo createAccessibilityNodeInfo(int virtualViewId) {
            if (virtualViewId == HOST_VIEW_ID) {
                AccessibilityNodeInfo root = obtainNodeInfo(CalculatorView.this);
                onInitializeAccessibilityNodeInfo(root);
                if (overlay == OVERLAY_NONE) {
                    root.addChild(CalculatorView.this, V_DISPLAY);
                    root.addChild(CalculatorView.this, V_HIST_ICON);
                    root.addChild(CalculatorView.this, V_FX_ICON);
                    root.addChild(CalculatorView.this, V_MENU_ICON);
                    for (Btn b : buttons) {
                        root.addChild(CalculatorView.this, V_BTN_BASE + b.id);
                    }
                } else if (overlay == OVERLAY_HISTORY) {
                    if (!history.items.isEmpty()) {
                        root.addChild(CalculatorView.this, V_HIST_CLEAR);
                    }
                    root.addChild(CalculatorView.this, V_HIST_CLOSE);
                    float rowH = dp(76);
                    for (int i = 0; i < history.items.size(); i++) {
                        float top = historyList.top + i * rowH - historyScroll;
                        if (top + rowH > historyList.top && top < historyList.bottom) {
                            root.addChild(CalculatorView.this, V_HIST_ITEM_BASE + i);
                        }
                    }
                } else if (overlay == OVERLAY_MENU) {
                    root.addChild(CalculatorView.this, V_MENU_CLEAR);
                    root.addChild(CalculatorView.this, V_MENU_SCI);
                    root.addChild(CalculatorView.this, V_MENU_ABOUT);
                } else if (overlay == OVERLAY_CONFIRM) {
                    root.addChild(CalculatorView.this, V_CONFIRM_CANCEL);
                    root.addChild(CalculatorView.this, V_CONFIRM_OK);
                } else if (overlay == OVERLAY_ABOUT) {
                    root.addChild(CalculatorView.this, V_ABOUT_CLOSE);
                }
                return root;
            }

            Rect bounds = boundsForVirtualView(virtualViewId);
            CharSequence desc = descriptionForVirtualView(virtualViewId);
            if (bounds == null || desc == null) {
                return null;
            }

            AccessibilityNodeInfo node = obtainNodeInfo();
            node.setPackageName(getContext().getPackageName());
            node.setSource(CalculatorView.this, virtualViewId);
            node.setParent(CalculatorView.this);
            node.setContentDescription(desc);
            node.setVisibleToUser(true);
            setBoundsInParentCompat(node, bounds);
            getLocationOnScreen(screenLoc);
            Rect screenBounds = new Rect(bounds);
            screenBounds.offset(screenLoc[0], screenLoc[1]);
            node.setBoundsInScreen(screenBounds);

            if (virtualViewId == V_DISPLAY) {
                node.setClassName("android.widget.TextView");
                node.setEnabled(true);
                node.setLongClickable(errorMsg == null);
                if (errorMsg == null) {
                    node.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_LONG_CLICK);
                }
            } else {
                node.setClassName("android.widget.Button");
                boolean enabled = virtualViewId != V_MENU_CLEAR || !history.items.isEmpty();
                node.setEnabled(enabled);
                node.setClickable(enabled);
                if (enabled) {
                    node.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK);
                }
            }

            if (focusedVirtualId == virtualViewId) {
                node.setAccessibilityFocused(true);
                node.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLEAR_ACCESSIBILITY_FOCUS);
            } else {
                node.setAccessibilityFocused(false);
                node.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_ACCESSIBILITY_FOCUS);
            }
            return node;
        }

        private Rect boundsForVirtualView(int virtualViewId) {
            RectF f = null;
            if (overlay == OVERLAY_NONE) {
                if (virtualViewId == V_DISPLAY) {
                    f = rDisplay;
                } else if (virtualViewId == V_HIST_ICON) {
                    f = rHistory;
                } else if (virtualViewId == V_FX_ICON) {
                    f = rFx;
                } else if (virtualViewId == V_MENU_ICON) {
                    f = rOverflow;
                } else if (virtualViewId >= V_BTN_BASE && virtualViewId < V_HIST_CLEAR) {
                    Btn b = findButton(virtualViewId - V_BTN_BASE);
                    if (b != null) {
                        f = b.rect;
                    }
                }
            } else if (overlay == OVERLAY_HISTORY) {
                if (virtualViewId == V_HIST_CLEAR && !history.items.isEmpty()) {
                    f = rHistClear;
                } else if (virtualViewId == V_HIST_CLOSE) {
                    f = rHistClose;
                } else if (virtualViewId >= V_HIST_ITEM_BASE) {
                    int idx = virtualViewId - V_HIST_ITEM_BASE;
                    if (idx >= 0 && idx < history.items.size()) {
                        float rowH = dp(76);
                        float top = Math.max(historyList.top, historyList.top + idx * rowH - historyScroll);
                        float bottom = Math.min(historyList.bottom, historyList.top + (idx + 1) * rowH - historyScroll);
                        if (bottom > top) {
                            return new Rect(Math.round(historyList.left), Math.round(top),
                                    Math.round(historyList.right), Math.round(bottom));
                        }
                    }
                }
            } else if (overlay == OVERLAY_MENU) {
                if (virtualViewId == V_MENU_CLEAR) {
                    f = menuClear;
                } else if (virtualViewId == V_MENU_SCI) {
                    f = menuSci;
                } else if (virtualViewId == V_MENU_ABOUT) {
                    f = menuAbout;
                }
            } else if (overlay == OVERLAY_CONFIRM) {
                if (virtualViewId == V_CONFIRM_CANCEL) {
                    f = confirmCancel;
                } else if (virtualViewId == V_CONFIRM_OK) {
                    f = confirmOk;
                }
            } else if (overlay == OVERLAY_ABOUT) {
                if (virtualViewId == V_ABOUT_CLOSE) {
                    f = aboutClose;
                }
            }
            if (f == null) {
                return null;
            }
            return new Rect(Math.round(f.left), Math.round(f.top), Math.round(f.right), Math.round(f.bottom));
        }

        @Override
        public boolean performAction(int virtualViewId, int action, Bundle arguments) {
            if (virtualViewId == HOST_VIEW_ID) {
                return performAccessibilityAction(action, arguments);
            }
            if (action == AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS) {
                if (focusedVirtualId != virtualViewId) {
                    focusedVirtualId = virtualViewId;
                    invalidate();
                    sendAccessibilityEventForVirtualView(virtualViewId,
                            AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED);
                    return true;
                }
                return false;
            }
            if (action == AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS) {
                if (focusedVirtualId == virtualViewId) {
                    focusedVirtualId = View.NO_ID;
                    invalidate();
                    sendAccessibilityEventForVirtualView(virtualViewId,
                            AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED);
                    return true;
                }
                return false;
            }
            if (action == AccessibilityNodeInfo.ACTION_LONG_CLICK && virtualViewId == V_DISPLAY) {
                copyDisplayedValue();
                return true;
            }
            if (action != AccessibilityNodeInfo.ACTION_CLICK) {
                return false;
            }
            if (overlay == OVERLAY_NONE) {
                if (virtualViewId == V_HIST_ICON) {
                    openOverlay(OVERLAY_HISTORY);
                } else if (virtualViewId == V_FX_ICON) {
                    toggleScientific(false);
                } else if (virtualViewId == V_MENU_ICON) {
                    openOverlay(OVERLAY_MENU);
                } else if (virtualViewId >= V_BTN_BASE && virtualViewId < V_HIST_CLEAR) {
                    onButton(virtualViewId - V_BTN_BASE);
                } else {
                    return false;
                }
                sendAccessibilityEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_CLICKED);
                return true;
            }
            if (overlay == OVERLAY_HISTORY) {
                if (virtualViewId == V_HIST_CLOSE) {
                    closeOverlay();
                } else if (virtualViewId == V_HIST_CLEAR && !history.items.isEmpty()) {
                    confirmResetAll = false;
                    openOverlay(OVERLAY_CONFIRM);
                } else if (virtualViewId >= V_HIST_ITEM_BASE) {
                    int idx = virtualViewId - V_HIST_ITEM_BASE;
                    if (idx >= 0 && idx < history.items.size()) {
                        recallHistoryEntry(idx);
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
                sendAccessibilityEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_CLICKED);
                return true;
            }
            if (overlay == OVERLAY_MENU) {
                if (virtualViewId == V_MENU_CLEAR && !history.items.isEmpty()) {
                    confirmResetAll = false;
                    openOverlay(OVERLAY_CONFIRM);
                } else if (virtualViewId == V_MENU_SCI) {
                    toggleScientific(true);
                } else if (virtualViewId == V_MENU_ABOUT) {
                    openOverlay(OVERLAY_ABOUT);
                } else {
                    return false;
                }
                sendAccessibilityEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_CLICKED);
                return true;
            }
            if (overlay == OVERLAY_CONFIRM) {
                if (virtualViewId == V_CONFIRM_CANCEL) {
                    closeOverlay();
                } else if (virtualViewId == V_CONFIRM_OK) {
                    performConfirmedClear();
                } else {
                    return false;
                }
                sendAccessibilityEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_CLICKED);
                return true;
            }
            if (overlay == OVERLAY_ABOUT && virtualViewId == V_ABOUT_CLOSE) {
                closeOverlay();
                sendAccessibilityEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_CLICKED);
                return true;
            }
            return false;
        }
    }
}
