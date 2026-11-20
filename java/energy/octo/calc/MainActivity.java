package energy.octo.calc;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

public class MainActivity extends Activity {

    private CalculatorView calculator;
    private Api33Back api33Back;

    private static final class Api33Back {
        private OnBackInvokedCallback callback;
        private boolean registered;

        void setEnabled(Activity activity, boolean enabled, Runnable onBack) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                return;
            }
            if (enabled == registered) {
                return;
            }
            OnBackInvokedDispatcher dispatcher = activity.getOnBackInvokedDispatcher();
            if (enabled) {
                if (callback == null && onBack != null) {
                    callback = onBack::run;
                }
                if (callback != null) {
                    dispatcher.registerOnBackInvokedCallback(
                            OnBackInvokedDispatcher.PRIORITY_DEFAULT, callback);
                    registered = true;
                }
            } else if (callback != null) {
                dispatcher.unregisterOnBackInvokedCallback(callback);
                registered = false;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setStatusBarContrastEnforced(false);
            window.setNavigationBarContrastEnforced(false);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
        } else {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        calculator = new CalculatorView(this);
        setContentView(calculator);

        if (savedInstanceState != null) {
            calculator.restoreState(savedInstanceState);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            api33Back = new Api33Back();
            calculator.setOnOverlayChangedListener(
                    hasOverlay -> api33Back.setEnabled(this, hasOverlay, () -> calculator.handleBack()));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        calculator.saveState(outState);
    }

    @Override
    protected void onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && api33Back != null) {
            api33Back.setEnabled(this, false, null);
        }
        super.onDestroy();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (!calculator.handleBack()) {
            super.onBackPressed();
        }
    }
}
