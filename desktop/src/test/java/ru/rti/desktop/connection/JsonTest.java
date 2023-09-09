package ru.rti.desktop.connection;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import ru.rti.desktop.model.ProfileTaskQueryKey;
import ru.rti.desktop.model.report.QueryReportData;

@Log4j2
public class JsonTest {

    @Test
    public void jsonTest() {
        Map<ProfileTaskQueryKey, QueryReportData> mapReportData = new HashMap<>();
        mapReportData.put(new ProfileTaskQueryKey(1, 2, 3), new QueryReportData());

        Map<String, QueryReportData> mapString = new HashMap<>();
        mapReportData.entrySet()
                .forEach(e -> {
                    mapString
                            .put(e.getKey().getProfileId() + "_" + e.getKey().getTaskId() + "_" + e.getKey().getQueryId(), e.getValue());
                });


        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.setPrettyPrinting().create();
        String input = gson.toJson(mapString);

        Map<String, QueryReportData> mapOutput = gson.fromJson(input,new TypeToken<Map<String, QueryReportData>>() {}.getType() );

        assertTrue(mapString.entrySet().containsAll(mapOutput.entrySet()));
        assertTrue(mapOutput.entrySet().containsAll(mapString.entrySet()));
    }

}
