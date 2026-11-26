
package com.blockblast.aisolver;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(ScreenCapturePlugin.class);
        registerPlugin(OverlayPlugin.class);
        super.onCreate(savedInstanceState);
    }
}
