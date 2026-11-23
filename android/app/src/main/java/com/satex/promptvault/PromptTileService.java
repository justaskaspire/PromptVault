package com.satex.promptvault;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class PromptTileService extends TileService {
    private static final String TAG = "PromptVaultTiming";

    @Override
    public void onStartListening() {
        super.onStartListening();
        Tile tile = getQsTile();
        if (tile != null) {
            tile.setState(Tile.STATE_ACTIVE);
            tile.updateTile();
        }
    }

    @Override
    public void onClick() {
        long startedAt = SystemClock.elapsedRealtime();
        Log.i(TAG, "TILE_CLICK received");
        super.onClick();
        Intent intent = new Intent(this, PromptPickerActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            Log.i(TAG, "PENDING_INTENT_READY after "
                    + (SystemClock.elapsedRealtime() - startedAt) + " ms");
            startActivityAndCollapse(pendingIntent);
        } else {
            Log.i(TAG, "LEGACY_LAUNCH_REQUEST after "
                    + (SystemClock.elapsedRealtime() - startedAt) + " ms");
            startActivityAndCollapse(intent);
        }
        Log.i(TAG, "TILE_LAUNCH_REQUEST_RETURNED after "
                + (SystemClock.elapsedRealtime() - startedAt) + " ms");
    }
}
