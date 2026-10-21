package energy.octo.calc;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

final class HistoryStore {

    static final class Entry {
        final String expression;
        final String result;

        Entry(String expression, String result) {
            this.expression = expression;
            this.result = result;
        }
    }

    private static final String PREFS = "octo_calc";
    private static final String KEY_HISTORY = "history";
    private static final int MAX_ITEMS = 100;

    private final SharedPreferences prefs;
    final List<Entry> items = new ArrayList<>();

    HistoryStore(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        load();
    }

    private void load() {
        try {
            String raw = prefs.getString(KEY_HISTORY, "[]");
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                items.add(new Entry(o.optString("e"), o.optString("r")));
            }
        } catch (Exception ignored) {
        }
    }

    void add(String expression, String result) {
        items.add(0, new Entry(expression, result));
        while (items.size() > MAX_ITEMS) {
            items.remove(items.size() - 1);
        }
        save();
    }

    void clear() {
        items.clear();
        save();
    }

    private void save() {
        try {
            JSONArray array = new JSONArray();
            for (Entry e : items) {
                JSONObject o = new JSONObject();
                o.put("e", e.expression);
                o.put("r", e.result);
                array.put(o);
            }
            prefs.edit().putString(KEY_HISTORY, array.toString()).apply();
        } catch (Exception ignored) {
        }
    }
}
