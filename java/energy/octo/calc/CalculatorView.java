package energy.octo.calc;

import android.animation.ValueAnimator;
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
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.animation.DecelerateInterpolator;

import java.util.ArrayList;
import java.util.List;

final class CalculatorView extends View {

    private static final int INK = 0xFF100030;
    private static final int INK_DEEP = 0xFF0B0022;
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
    private static final int CYAN = 0xFF71E3FF;
    private static final int YELLOW = 0xFFFFD862;

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

    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint buttonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint dimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Typeface regular;
    private final Typeface medium;
    private final Typeface semibold;
    private final Typeface bold;
    private final Bitmap mascot;
    private final RectF mascotRect = new RectF();

    private final ArrayList<String> tokens = new ArrayList<>();
    private String lastExpr;
    private String lastResultRaw;
    private double lastResultValue;
    private double lastAnswer;
    private boolean hasAnswer;
    private String errorMsg;

    private boolean degrees = true;
    private boolean inverse;
    private boolean scientific;
    private double memory;
    private boolean hasMemory;

    private float density;
    private float scaledDensity;
    private int W, H;
    private float padTop, padBottom;

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
    private float downX, downY;
    private boolean longFired;
    private int headerPressed;
    private boolean displayDragging;
    private float displayDragStartX;
    private float displayDragStartScroll;
    private final Runnable longPressRunnable = this::onLongPress;

    private int overlay = OVERLAY_NONE;
    private float overlayAnim = 1f;
    private ValueAnimator overlayAnimator;

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

    private final RectF menuCard = new RectF();
    private final RectF menuClear = new RectF();
    private final RectF menuSci = new RectF();
    private final RectF menuAbout = new RectF();

    private final RectF confirmCard = new RectF();
    private final RectF confirmCancel = new RectF();
    private final RectF confirmOk = new RectF();
    private final RectF aboutCard = new RectF();
    private final RectF aboutClose = new RectF();

    CalculatorView(Context context) {
        super(context);
        density = getResources().getDisplayMetrics().density;
        scaledDensity = getResources().getDisplayMetrics().scaledDensity;
        regular = getResources().getFont(R.font.poppins_regular);
        medium = getResources().getFont(R.font.poppins_medium);
        semibold = getResources().getFont(R.font.poppins_semibold);
        bold = getResources().getFont(R.font.poppins_bold);
        mascot = BitmapFactory.decodeResource(getResources(), R.drawable.mascot);
        history = new HistoryStore(context);
        setFocusable(true);
        setFocusableInTouchMode(true);
        setBackgroundColor(INK);
        setSystemUiVisibility(
                SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }

    private float dp(float value) {
        return value * density;
    }

    private float sp(float value) {
        return value * scaledDensity;
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
        layoutUi();
        requestFocus();
    }

    private void layoutUi() {
        if (W == 0 || H == 0) {
            return;
        }
        float margin = dp(16);
        keypadGap = dp(9);
        keypadCellW = (W - 2 * margin - 3 * keypadGap) / 4f;
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

        keypadTop = contentBottom - keypadH;
        sciTop = keypadTop - sciHeight;
        displayTop = headerTop + headerH + dp(4);
        displayBottom = scientific ? sciTop - dp(4) : keypadTop - dp(6);
        if (displayBottom < displayTop + dp(80)) {
            displayBottom = displayTop + dp(80);
        }
        displayRect.set(0, displayTop, W, displayBottom);
        rDisplay.set(dp(8), displayTop, W - dp(8), displayBottom);

        float keypadLeft = margin;
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

        if (scientific) {
            float sciGapX = dp(6);
            float sw = (W - 2 * margin - 4 * sciGapX) / 5f;
            float sh = sciBtnH;
            int[][] ids = {
                    {ID_INV, ID_DEG, ID_SIN, ID_COS, ID_TAN},
                    {ID_LN, ID_LOG, ID_EXP, ID_TENX, ID_POW},
                    {ID_SQRT, ID_SQR, ID_FACT, ID_PI, ID_E},
                    {ID_MC, ID_MR, ID_MPLUS, ID_MMINUS, ID_ANS}
            };
            for (int row = 0; row < 4; row++) {
                for (int col = 0; col < ids[row].length; col++) {
                    float x = margin + col * (sw + sciGapX);
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
        float iconSize = dp(44);
        float right = W - dp(20);
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
        float margin = dp(12);
        historySheet.set(margin, padTop + dp(8), W - margin, H - padBottom - dp(10));

        menuCard.set(W - Math.min(dp(302), W - dp(24)) - margin,
                headerTop + headerH + dp(4),
                W - margin,
                headerTop + headerH + dp(4) + 3 * dp(54) + dp(14));
        float itemTop = menuCard.top + dp(7);
        menuClear.set(menuCard.left, itemTop, menuCard.right, itemTop + dp(54));
        menuSci.set(menuCard.left, itemTop + dp(54), menuCard.right, itemTop + dp(108));
        menuAbout.set(menuCard.left, itemTop + dp(108), menuCard.right, itemTop + dp(162));

        float cw = Math.min(dp(330), W - dp(40));
        float ch = dp(214);
        confirmCard.set((W - cw) / 2f, (H - ch) / 2f, (W + cw) / 2f, (H + ch) / 2f);
        float by = confirmCard.bottom - dp(66);
        confirmCancel.set(confirmCard.left + dp(20), by, confirmCard.centerX() - dp(6), by + dp(46));
        confirmOk.set(confirmCard.centerX() + dp(6), by, confirmCard.right - dp(20), by + dp(46));

        float ah = dp(352);
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
        glowPaint.setShader(new RadialGradient(W * 0.92f, H * 0.30f, W * 0.62f,
                new int[]{0x2AFF48D8, 0x0DFF48D8, 0x00FF48D8}, null, Shader.TileMode.CLAMP));
        canvas.drawCircle(W * 0.92f, H * 0.30f, W * 0.62f, glowPaint);
        glowPaint.setShader(new RadialGradient(W * 0.02f, H * 0.72f, W * 0.55f,
                new int[]{0x244B2AD8, 0x004B2AD8}, null, Shader.TileMode.CLAMP));
        canvas.drawCircle(W * 0.02f, H * 0.72f, W * 0.55f, glowPaint);
        glowPaint.setShader(null);
    }

    private void drawHeader(Canvas canvas) {
        float cy = headerTop + headerH / 2f;
        drawMascot(canvas, dp(28), cy + dp(1.5f), dp(31), 1f);
        textPaint.setTypeface(semibold);
        textPaint.setTextSize(dp(21));
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(WHITE);
        canvas.drawText("Octo", dp(49), cy + dp(7.5f), textPaint);
        float w = textPaint.measureText("Octo");
        textPaint.setColor(PINK);
        canvas.drawText("Calc", dp(49) + w + dp(1), cy + dp(7.5f), textPaint);

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
        linePaint.setColor(LAVENDER);
        linePaint.setAlpha(overlay == OVERLAY_HISTORY ? 255 : 215);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(dp(1.9f));
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        RectF arc = new RectF(cx - dp(9), cy - dp(9), cx + dp(9), cy + dp(9));
        canvas.drawArc(arc, 205, 285, false, linePaint);
        Path arrow = new Path();
        arrow.moveTo(cx - dp(9.5f), cy - dp(3.5f));
        arrow.lineTo(cx - dp(9.5f), cy - dp(9f));
        arrow.lineTo(cx - dp(3.8f), cy - dp(9f));
        canvas.drawPath(arrow, linePaint);
        canvas.drawLine(cx, cy - dp(4.5f), cx, cy + dp(0.5f), linePaint);
        canvas.drawLine(cx, cy + dp(0.5f), cx + dp(4f), cy + dp(2.5f), linePaint);
        linePaint.setAlpha(255);
    }

    private void drawFxIcon(Canvas canvas, float cx, float cy) {
        RectF pill = new RectF(cx - dp(14), cy - dp(11.5f), cx + dp(14), cy + dp(11.5f));
        buttonPaint.setShader(null);
        buttonPaint.setStyle(Paint.Style.FILL);
        buttonPaint.setColor(scientific ? PINK : 0x00000000);
        canvas.drawRoundRect(pill, dp(11), dp(11), buttonPaint);
        if (!scientific) {
            strokePaint.setShader(null);
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeWidth(dp(1.3f));
            strokePaint.setColor(0xFF493579);
            canvas.drawRoundRect(pill, dp(11), dp(11), strokePaint);
        }
        textPaint.setTypeface(medium);
        textPaint.setTextSize(dp(13));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(scientific ? INK : LAVENDER);
        canvas.drawText("fx", cx, cy + dp(4.5f), textPaint);
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

    private void drawDisplay(Canvas canvas) {
        String small = null;
        String main;
        String preview = null;

        String expr = joinExpression();
        if (errorMsg != null) {
            main = errorMsg;
            small = expr.isEmpty() ? null : pretty(expr);
        } else if (tokens.isEmpty() && lastResultRaw != null) {
            main = pretty(lastResultRaw);
            small = lastExpr == null ? null : lastExpr + " =";
        } else {
            main = expr.isEmpty() ? "0" : pretty(expr);
        }

        if (tokens.isEmpty() && lastResultRaw == null && errorMsg == null) {
            main = "0";
        }

        boolean previewVisible = false;
        if (errorMsg == null && !tokens.isEmpty() && tokens.size() > 1) {
            preview = computePreview();
            if (preview != null && !preview.equals(main)) {
                previewVisible = true;
            }
        }

        canvas.save();
        canvas.clipRect(displayRect);
        float right = W - dp(24);
        float avail = W - dp(48);
        displayAvail = avail;

        if (small != null) {
            textPaint.setTypeface(medium);
            textPaint.setTextSize(dp(18));
            textPaint.setTextAlign(Paint.Align.RIGHT);
            textPaint.setColor(MUTED);
            canvas.drawText(small, right, displayTop + dp(24), textPaint);
        }

        float previewSize = dp(22);
        float previewY = displayBottom - dp(8);
        float mainBase = previewVisible ? previewY - dp(28) : displayBottom - dp(12);

        float size = (tokens.isEmpty() && lastResultRaw != null && errorMsg == null) ? dp(64) : dp(46);
        if (errorMsg != null) {
            size = dp(30);
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
            textPaint.setTextSize(previewSize);
            textPaint.setTextAlign(Paint.Align.RIGHT);
            textPaint.setColor(0xFFB9B2E6);
            String p = pretty(preview);
            canvas.drawText(p, right, previewY, textPaint);
        }

        if (hasMemory) {
            RectF badge = new RectF(dp(24), displayTop + dp(2), dp(24) + dp(24), displayTop + dp(20));
            buttonPaint.setShader(null);
            buttonPaint.setStyle(Paint.Style.FILL);
            buttonPaint.setColor(0xFF2B1A5F);
            canvas.drawRoundRect(badge, dp(9), dp(9), buttonPaint);
            textPaint.setTypeface(semibold);
            textPaint.setTextSize(dp(10));
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setColor(PINK);
            canvas.drawText("M", badge.centerX(), badge.centerY() + dp(3.5f), textPaint);
        }
        canvas.restore();
    }

    private String computePreview() {
        try {
            double v = CalcEngine.evaluate(joinExpression(), degrees);
            return CalcEngine.format(v);
        } catch (CalcEngine.Error e) {
            return null;
        }
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
            if (b.id == ID_EQ) {
                buttonPaint.setShader(new LinearGradient(b.rect.left, b.rect.top, b.rect.right, b.rect.bottom,
                        new int[]{PINK, PINK_DEEP}, null, Shader.TileMode.CLAMP));
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
            float size = label.length() >= 5 ? dp(10.5f) : dp(12.5f);
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
        Path p = new Path();
        p.moveTo(cx - w / 2f, cy);
        p.lineTo(cx - w / 2f + h * 0.55f, cy - h / 2f);
        p.lineTo(cx + w / 2f, cy - h / 2f);
        p.lineTo(cx + w / 2f, cy + h / 2f);
        p.lineTo(cx - w / 2f + h * 0.55f, cy + h / 2f);
        p.close();
        canvas.drawPath(p, linePaint);
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
        canvas.drawLine(cx - size / 2f, cy - dp(6), cx + size / 2f, cy - dp(6), linePaint);
        canvas.drawLine(cx - size / 2f, cy + dp(6), cx + size / 2f, cy + dp(6), linePaint);
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
    }

    private void afterEdit() {
        errorMsg = null;
        scrollToEnd = true;
        invalidate();
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
            tokens.set(tokens.size() - 1, last + digit);
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
            tokens.add(lastResultRaw);
            clearLastState();
        } else if (errorMsg != null) {
            errorMsg = null;
        }
        if (!tokens.isEmpty() && isOperatorToken(tokens.get(tokens.size() - 1))) {
            tokens.remove(tokens.size() - 1);
        }
        if (tokens.isEmpty()) {
            if (op.equals("\u2212")) {
                tokens.add(op);
            }
            afterEdit();
            return;
        }
        String last = tokens.get(tokens.size() - 1);
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

    private void appendPercent() {
        if (tokens.isEmpty()) {
            if (lastResultRaw != null) {
                tokens.add(lastResultRaw);
                clearLastState();
            } else {
                return;
            }
        }
        String last = tokens.get(tokens.size() - 1);
        if (isOperatorToken(last) || last.endsWith("(")) {
            return;
        }
        tokens.add("%");
        afterEdit();
    }

    private void appendPostfix(String token) {
        if (tokens.isEmpty()) {
            if (lastResultRaw != null) {
                tokens.add(lastResultRaw);
                clearLastState();
            } else {
                return;
            }
        }
        String last = tokens.get(tokens.size() - 1);
        if (isOperatorToken(last) || last.endsWith("(")) {
            return;
        }
        tokens.add(token);
        afterEdit();
    }

    private void backspace() {
        if (tokens.isEmpty()) {
            if (lastResultRaw != null || lastExpr != null) {
                clearLastState();
                scrollToEnd = true;
                invalidate();
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

    private void evaluateAndShow() {
        if (tokens.isEmpty()) {
            if (lastResultRaw != null) {
                errorMsg = null;
                        invalidate();
            }
            return;
        }
        String expr = joinExpression();
        try {
            double value = CalcEngine.evaluate(expr, degrees);
            String result = CalcEngine.format(value);
            history.add(pretty(expr), pretty(result));
            lastExpr = pretty(expr);
            lastResultRaw = result;
            lastResultValue = value;
            lastAnswer = value;
            hasAnswer = true;
                errorMsg = null;
            tokens.clear();
            scrollToEnd = true;
            performHapticFeedback(HapticFeedbackConstants.CONFIRM);
        } catch (CalcEngine.Error e) {
            errorMsg = e.getMessage();
            }
        invalidate();
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
        tokens.add(CalcEngine.format(memory).replace('-', '\u2212'));
        afterEdit();
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
                scrollToEnd = true;
                invalidate();
                break;
            case ID_PAREN:
                appendParen();
                break;
            case ID_PCT:
                appendPercent();
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
                break;
            case ID_DEG:
                degrees = !degrees;
                afterEdit();
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
                invalidate();
                break;
            case ID_MR:
                insertMemory();
                break;
            case ID_MPLUS:
                memory += currentValue();
                hasMemory = true;
                invalidate();
                break;
            case ID_MMINUS:
                memory -= currentValue();
                hasMemory = true;
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
                tokens.add(CalcEngine.format(lastAnswer).replace('-', '\u2212'));
                afterEdit();
                break;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = x;
                downY = y;
                longFired = false;
                if (overlay != OVERLAY_NONE) {
                    handleOverlayDown(x, y);
                    return true;
                }
                if (rDisplay.contains(x, y)) {
                    displayDragging = true;
                    displayDragStartX = x;
                    displayDragStartScroll = mainScroll;
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
                    handleOverlayMove(x, y);
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
                if (overlay != OVERLAY_NONE) {
                    handleOverlayUp(x, y);
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
                            scientific = !scientific;
                            layoutUi();
                        } else {
                            openOverlay(OVERLAY_MENU);
                        }
                    }
                    return true;
                }
                if (displayDragging) {
                    displayDragging = false;
                    return true;
                }
                handler.removeCallbacks(longPressRunnable);
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
                if (overlay != OVERLAY_NONE) {
                    finishOverlayTouch();
                    return true;
                }
                handler.removeCallbacks(longPressRunnable);
                pressedId = -1;
                headerPressed = 0;
                displayDragging = false;
                historyDragging = false;
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
        if (pressedId == ID_AC) {
            longFired = true;
            pressedId = -1;
            openOverlay(OVERLAY_CONFIRM);
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        } else if (pressedId == ID_BACK) {
            longFired = true;
            pressedId = -1;
            if (!tokens.isEmpty()) {
                tokens.clear();
                clearLastState();
                scrollToEnd = true;
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                invalidate();
            }
        }
    }

    private void openOverlay(int which) {
        overlay = which;
        overlayAnim = 0f;
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
            refreshHistoryBounds();
            historyScroll = 0;
            historyDragging = false;
            historyMoved = false;
        }
        invalidate();
    }

    private void closeOverlay() {
        overlay = OVERLAY_NONE;
        pressedId = -1;
        historyDragging = false;
        if (overlayAnimator != null) {
            overlayAnimator.cancel();
            overlayAnimator = null;
        }
        invalidate();
    }

    private void refreshHistoryBounds() {
        historySheet.set(dp(10), padTop + dp(8), W - dp(10), H - padBottom - dp(10));
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
    }

    private void handleOverlayDown(float x, float y) {
        if (overlay == OVERLAY_HISTORY) {
            if (rHistClose.contains(x, y)) {
                return;
            }
            if (!history.items.isEmpty() && rHistClear.contains(x, y)) {
                return;
            }
            if (historyList.contains(x, y)) {
                historyDragging = true;
                historyDownY = y;
                historyStartScroll = historyScroll;
                historyMoved = false;
            }
        } else if (overlay == OVERLAY_MENU) {
            // handled on up
        } else if (overlay == OVERLAY_CONFIRM) {
            // handled on up
        } else if (overlay == OVERLAY_ABOUT) {
            // handled on up
        }
    }

    private void handleOverlayMove(float x, float y) {
        if (overlay == OVERLAY_HISTORY && historyDragging) {
            float delta = historyDownY - y;
            if (Math.abs(delta) > dp(5)) {
                historyMoved = true;
            }
            historyScroll = Math.max(0, Math.min(historyMaxScroll, historyStartScroll + delta));
            invalidate();
        }
    }

    private void handleOverlayUp(float x, float y) {
        if (overlay == OVERLAY_HISTORY) {
            if (rHistClose.contains(x, y)) {
                closeOverlay();
                return;
            }
            if (!history.items.isEmpty() && rHistClear.contains(x, y)) {
                openOverlay(OVERLAY_CONFIRM);
                return;
            }
            if (historyDragging && !historyMoved && history.items.size() > 0) {
                int index = (int) ((y - historyList.top + historyScroll) / dp(76));
                if (index >= 0 && index < history.items.size()) {
                    HistoryStore.Entry entry = history.items.get(index);
                    tokens.clear();
                    clearLastState();
                    tokens.add(entry.result.replace('-', '\u2212'));
                    scrollToEnd = true;
                    closeOverlay();
                    afterEdit();
                    return;
                }
            }
            historyDragging = false;
            return;
        }
        if (overlay == OVERLAY_MENU) {
            if (menuClear.contains(x, y) && !history.items.isEmpty()) {
                openOverlay(OVERLAY_CONFIRM);
                return;
            }
            if (menuSci.contains(x, y)) {
                scientific = !scientific;
                closeOverlay();
                layoutUi();
                return;
            }
            if (menuAbout.contains(x, y)) {
                openOverlay(OVERLAY_ABOUT);
                return;
            }
            closeOverlay();
            return;
        }
        if (overlay == OVERLAY_CONFIRM) {
            if (confirmOk.contains(x, y)) {
                history.clear();
                memory = 0;
                hasMemory = false;
                tokens.clear();
                clearLastState();
                scrollToEnd = true;
                closeOverlay();
                return;
            }
            if (confirmCancel.contains(x, y) || !confirmCard.contains(x, y)) {
                closeOverlay();
            }
            return;
        }
        if (overlay == OVERLAY_ABOUT) {
            if (aboutClose.contains(x, y) || !aboutCard.contains(x, y)) {
                closeOverlay();
            }
        }
    }

    private void finishOverlayTouch() {
        historyDragging = false;
    }

    private void drawOverlay(Canvas canvas) {
        dimPaint.setShader(null);
        dimPaint.setColor(0x00000000);
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
        refreshHistoryBounds();
        float offset = (1f - overlayAnim) * dp(46);
        canvas.save();
        canvas.translate(0, offset);
        drawCard(canvas, historySheet, dp(28), 0xFF16073E, 0x88000000);

        textPaint.setTypeface(semibold);
        textPaint.setTextSize(dp(20));
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(WHITE);
        canvas.drawText("Historique", historySheet.left + dp(24), historySheet.top + dp(40), textPaint);

        if (!history.items.isEmpty()) {
            textPaint.setTypeface(medium);
            textPaint.setTextSize(dp(13));
            textPaint.setTextAlign(Paint.Align.RIGHT);
            textPaint.setColor(PINK);
            canvas.drawText("Effacer", rHistClear.right, historySheet.top + dp(39), textPaint);
        }

        drawCloseIcon(canvas, rHistClose.centerX(), rHistClose.centerY());

        canvas.save();
        canvas.clipRect(historyList);
        if (history.items.isEmpty()) {
            float cy = historyList.centerY() - dp(40);
            drawMascot(canvas, historyList.centerX(), cy + dp(6), dp(68), 0.38f);
            textPaint.setTypeface(medium);
            textPaint.setTextSize(dp(17));
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setColor(LAVENDER);
            canvas.drawText("Aucun historique", historyList.centerX(), cy + dp(60), textPaint);
            textPaint.setTypeface(regular);
            textPaint.setTextSize(dp(13.5f));
            textPaint.setColor(MUTED_DARK);
            canvas.drawText("Vos calculs apparaîtront ici", historyList.centerX(), cy + dp(82), textPaint);
        } else {
            int count = history.items.size();
            float rowH = dp(76);
            for (int i = 0; i < count; i++) {
                HistoryStore.Entry entry = history.items.get(i);
                float top = historyList.top + i * rowH - historyScroll;
                if (top > historyList.bottom || top + rowH < historyList.top) {
                    continue;
                }
                float right = historyList.right - dp(16);
                textPaint.setTypeface(regular);
                textPaint.setTextSize(dp(15));
                textPaint.setTextAlign(Paint.Align.RIGHT);
                textPaint.setColor(MUTED);
                canvas.drawText(entry.expression, right, top + dp(28), textPaint);
                textPaint.setTypeface(semibold);
                textPaint.setTextSize(dp(25));
                textPaint.setColor(WHITE);
                canvas.drawText(entry.result, right, top + dp(58), textPaint);
                if (i < count - 1) {
                    linePaint.setShader(null);
                    linePaint.setStyle(Paint.Style.STROKE);
                    linePaint.setStrokeWidth(1);
                    linePaint.setColor(0x12FFFFFF);
                    canvas.drawLine(historyList.left + dp(12), top + rowH - dp(1),
                            historyList.right - dp(12), top + rowH - dp(1), linePaint);
                }
            }
        }
        canvas.restore();
        canvas.restore();
    }

    private void drawMenu(Canvas canvas) {
        drawCard(canvas, menuCard, dp(22), 0xFF1B0C48, 0x99000000);
        canvas.save();
        canvas.clipRect(menuCard);
        drawMenuItem(canvas, menuClear, "Effacer l'historique", 0, !history.items.isEmpty());
        drawMenuItem(canvas, menuSci, "Boutons scientifiques", 1, true);
        drawMenuItem(canvas, menuAbout, "\u00c0 propos", 2, true);
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
        textPaint.setTextSize(dp(15));
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
            RectF can = new RectF(cx - dp(6), cy - dp(5), cx + dp(6), cy + dp(8));
            canvas.drawRoundRect(can, dp(3), dp(3), linePaint);
            canvas.drawLine(cx - dp(8), cy - dp(5), cx + dp(8), cy - dp(5), linePaint);
            canvas.drawLine(cx - dp(3), cy - dp(8), cx + dp(3), cy - dp(8), linePaint);
            canvas.drawLine(cx - dp(2), cy - dp(1), cx - dp(2), cy + dp(4), linePaint);
            canvas.drawLine(cx + dp(2), cy - dp(1), cx + dp(2), cy + dp(4), linePaint);
        } else if (icon == 1) {
            textPaint.setTypeface(medium);
            textPaint.setTextSize(dp(14));
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setColor(color);
            canvas.drawText("fx", cx, cy + dp(5), textPaint);
        } else {
            RectF circle = new RectF(cx - dp(8), cy - dp(8), cx + dp(8), cy + dp(8));
            canvas.drawOval(circle, linePaint);
            textPaint.setTypeface(medium);
            textPaint.setTextSize(dp(12));
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
        Path p = new Path();
        p.moveTo(cx - dp(6), cy);
        p.lineTo(cx - dp(1), cy + dp(5));
        p.lineTo(cx + dp(7), cy - dp(5));
        canvas.drawPath(p, linePaint);
    }

    private void drawConfirm(Canvas canvas) {
        float scale = 0.94f + 0.06f * overlayAnim;
        canvas.save();
        canvas.scale(scale, scale, W / 2f, H / 2f);
        drawCard(canvas, confirmCard, dp(24), 0xFF1B0C48, 0x99000000);
        textPaint.setTypeface(semibold);
        textPaint.setTextSize(dp(19));
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(WHITE);
        canvas.drawText("Effacer l'historique ?", confirmCard.left + dp(24), confirmCard.top + dp(48), textPaint);
        textPaint.setTypeface(regular);
        textPaint.setTextSize(dp(14));
        textPaint.setColor(MUTED);
        canvas.drawText("L'historique et la m\u00e9moire seront", confirmCard.left + dp(24), confirmCard.top + dp(78), textPaint);
        canvas.drawText("effac\u00e9s d\u00e9finitivement.", confirmCard.left + dp(24), confirmCard.top + dp(98), textPaint);

        textPaint.setTypeface(medium);
        textPaint.setTextSize(dp(14.5f));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(LAVENDER);
        canvas.drawText("Annuler", confirmCancel.centerX(), confirmCancel.centerY() + dp(5), textPaint);

        buttonPaint.setShader(null);
        buttonPaint.setStyle(Paint.Style.FILL);
        buttonPaint.setColor(PINK);
        canvas.drawRoundRect(confirmOk, dp(23), dp(23), buttonPaint);
        textPaint.setTypeface(semibold);
        textPaint.setColor(INK);
        canvas.drawText("Effacer", confirmOk.centerX(), confirmOk.centerY() + dp(5), textPaint);
        canvas.restore();
    }

    private void drawAbout(Canvas canvas) {
        float scale = 0.94f + 0.06f * overlayAnim;
        canvas.save();
        canvas.scale(scale, scale, W / 2f, H / 2f);
        drawCard(canvas, aboutCard, dp(26), 0xFF1B0C48, 0x99000000);
        float cx = aboutCard.centerX();
        drawMascot(canvas, cx, aboutCard.top + dp(71), dp(75), 1f);
        textPaint.setTypeface(semibold);
        textPaint.setTextSize(dp(24));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(WHITE);
        canvas.drawText("Octo Calc", cx, aboutCard.top + dp(130), textPaint);
        textPaint.setTypeface(regular);
        textPaint.setTextSize(dp(12.5f));
        textPaint.setColor(MUTED);
        canvas.drawText("Version 1.0", cx, aboutCard.top + dp(152), textPaint);

        textPaint.setTextSize(dp(14));
        textPaint.setColor(LAVENDER);
        canvas.drawText("Une calculatrice au design inspir\u00e9", cx, aboutCard.top + dp(194), textPaint);
        canvas.drawText("de la charte Octopus Energy.", cx, aboutCard.top + dp(214), textPaint);
        textPaint.setTextSize(dp(12.5f));
        textPaint.setColor(MUTED);
        canvas.drawText("Scientifique  \u00b7  Historique  \u00b7  M\u00e9moire", cx, aboutCard.top + dp(250), textPaint);

        buttonPaint.setShader(null);
        buttonPaint.setStyle(Paint.Style.FILL);
        buttonPaint.setColor(PINK);
        canvas.drawRoundRect(aboutClose, dp(23), dp(23), buttonPaint);
        textPaint.setTypeface(semibold);
        textPaint.setTextSize(dp(14.5f));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(INK);
        canvas.drawText("Fermer", aboutClose.centerX(), aboutClose.centerY() + dp(5), textPaint);
        canvas.restore();
    }

    private void drawCard(Canvas canvas, RectF rect, float radius, int fill, int shadow) {
        cardPaint.setShader(null);
        cardPaint.setStyle(Paint.Style.FILL);
        cardPaint.setColor(shadow);
        RectF shadowRect = new RectF(rect);
        shadowRect.offset(0, dp(8));
        canvas.drawRoundRect(shadowRect, radius, radius, cardPaint);
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
            return true;
        }
        return false;
    }

    void saveState(Bundle out) {
        out.putStringArrayList("tokens", new ArrayList<>(tokens));
        out.putString("lastExpr", lastExpr);
        out.putString("lastResultRaw", lastResultRaw);
        out.putDouble("lastResultValue", lastResultValue);
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
        degrees = in.getBoolean("degrees");
        inverse = in.getBoolean("inverse");
        scientific = in.getBoolean("scientific");
        memory = in.getDouble("memory");
        hasMemory = in.getBoolean("hasMemory");
        lastAnswer = in.getDouble("lastAnswer");
        hasAnswer = in.getBoolean("hasAnswer");
        scrollToEnd = true;
        layoutUi();
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        handler.removeCallbacks(longPressRunnable);
        if (overlayAnimator != null) {
            overlayAnimator.cancel();
        }
    }
}
