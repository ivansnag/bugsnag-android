package com.bugsnag.android.mazerunner.scenarios;

import com.bugsnag.android.BeforeSend;
import com.bugsnag.android.Configuration;
import com.bugsnag.android.Report;

import android.content.Context;
import androidx.annotation.NonNull;

public class BeforeSendScenario extends Scenario {

    /**
     * Constructs a scenario which adds a beforeSend block
     */
    public BeforeSendScenario(@NonNull Configuration config, @NonNull Context context) {
        super(config, context);
        config.setAutoTrackSessions(false);
        config.addBeforeSend(new BeforeSend() {
            @Override
            public boolean run(Report report) {
                report.getEvent().setContext("UNSET");

                return true;
            }
        });
    }

    @Override
    public void run() {
        super.run();
        String metadata = getEventMetaData();
        if (metadata != null && metadata.equals("non-crashy")) {
            return;
        }
        throw new RuntimeException("Ruh-roh");
    }
}
