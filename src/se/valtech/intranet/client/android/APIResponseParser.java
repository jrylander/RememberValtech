package se.valtech.intranet.client.android;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class APIResponseParser {
    private SimpleDateFormat format;

    public APIResponseParser() {
        format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }

    public List<Employee> parseEmployees(String data) {
        List<Employee> result = new ArrayList<Employee>();

        try {
            JSONArray array = new JSONArray(data);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                long employeeId = object.getLong("id");

                JSONObject status = getJSONObject(object, "status");

                String statusMessage = null;
                long statusTimeStamp = 0;
                if (status != null) {
                    statusMessage = status.getString("description");
                    statusTimeStamp = parseStatusTimeStamp(status);
                }

                result.add(new Employee(employeeId,
                                        object.getString("first_name"),
                                        object.getString("last_name"),
                                        object.getString("mobile"),
                                        object.getString("image_url"),
                                        object.getString("thumbnail_url"),
                                        object.getString("email"),
                                        statusMessage,
                                        statusTimeStamp));
            }

        } catch (JSONException e) {
            throw new RuntimeException("Could not parse data", e);
        }

        assertUserIdsUnique(result);

        return result;
    }

    private void assertUserIdsUnique(List<Employee> result) {
        HashSet<Long> userIds = new HashSet<Long>();
        for (Employee employee : result) {
            if (!userIds.add(employee.getUserId())) {
                throw new RuntimeException("Duplicate user id from the intranet API: " + employee.getUserId());
            }
        }
    }

    private JSONObject getJSONObject(JSONObject object, String key) throws JSONException {
        if (!object.has(key)) {
            return null;
        }
        Object o = object.get(key);
        if (o == null || JSONObject.NULL.equals(o) || "".equals(o)) {
            return null;
        }

        return object.getJSONObject(key);
    }

    private long parseStatusTimeStamp(JSONObject status) throws JSONException {
        try {
            return format.parse(status.getString("created_on")).getTime();
        } catch (ParseException e) {
            Log.w("ValtechIntra", "Could not parse status timestamp: " + status.getString("created_on"), e);
            return 0;
        }
    }
}
