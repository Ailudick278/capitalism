package com.ailudick.capitalismmod.screen;

import com.ailudick.capitalismmod.Config;

/** Camera model for the world map: world-space center plus pixels-per-world-block zoom. */
public final class WorldMapViewport {
    private double centerX;
    private double centerZ;
    private float zoom = 4.0F;

    public WorldMapViewport(double centerX, double centerZ) {
        this.centerX = centerX;
        this.centerZ = centerZ;
    }

    public double centerX() { return centerX; }
    public double centerZ() { return centerZ; }
    public float zoom() { return zoom; }

    public void setZoom(float value) {
        zoom = clampZoom(value);
    }

    public void centerOn(double x, double z) {
        centerX = x;
        centerZ = z;
    }

    /** Moves the view in screen pixels while preserving the world point under the cursor. */
    public void panPixels(double deltaX, double deltaZ) {
        centerX -= deltaX / zoom;
        centerZ -= deltaZ / zoom;
    }

    public void zoomAt(double factor, double anchorWorldX, double anchorWorldZ) {
        float oldZoom = zoom;
        zoom = clampZoom((float) (zoom * factor));
        if (oldZoom != zoom) {
            double ratio = oldZoom / zoom;
            centerX = anchorWorldX + (centerX - anchorWorldX) * ratio;
            centerZ = anchorWorldZ + (centerZ - anchorWorldZ) * ratio;
        }
    }

    public double worldXAtScreen(double screenX, double viewportCenterX) {
        return centerX + (screenX - viewportCenterX) / zoom;
    }

    public double worldZAtScreen(double screenZ, double viewportCenterZ) {
        return centerZ + (screenZ - viewportCenterZ) / zoom;
    }

    public float screenX(double worldX, double viewportCenterX) {
        return (float) (viewportCenterX + (worldX - centerX) * zoom);
    }

    public float screenZ(double worldZ, double viewportCenterZ) {
        return (float) (viewportCenterZ + (worldZ - centerZ) * zoom);
    }

    private static float clampZoom(float value) {
        float min = Config.WORLD_MAP_MIN_ZOOM.get().floatValue();
        float max = Math.max(min, Config.WORLD_MAP_MAX_ZOOM.get().floatValue());
        return Math.max(min, Math.min(max, value));
    }
}
