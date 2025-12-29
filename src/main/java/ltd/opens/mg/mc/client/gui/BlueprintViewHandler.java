package ltd.opens.mg.mc.client.gui;

public class BlueprintViewHandler {
    private final BlueprintState state;

    public BlueprintViewHandler(BlueprintState state) {
        this.state = state;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 2) { // Middle click for panning
            state.isPanning = true;
            state.lastMouseX = mouseX;
            state.lastMouseY = mouseY;
            return true;
        }
        return false;
    }

    public boolean mouseReleased(int button) {
        if (button == 2) {
            state.isPanning = false;
            return true;
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY) {
        if (state.isPanning) {
            state.panX += (float) (mouseX - state.lastMouseX);
            state.panY += (float) (mouseY - state.lastMouseY);
            state.lastMouseX = mouseX;
            state.lastMouseY = mouseY;
            return true;
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        float zoomSensitivity = 0.1f;
        float oldZoom = state.zoom;
        if (scrollY > 0) {
            state.zoom *= (1 + zoomSensitivity);
        } else {
            state.zoom /= (1 + zoomSensitivity);
        }
        
        // Zoom limits
        state.zoom = Math.max(0.1f, Math.min(3.0f, state.zoom));
        
        if (state.zoom != oldZoom) {
            // Adjust pan to zoom towards mouse position
            double worldMouseX = (mouseX - state.panX) / oldZoom;
            double worldMouseY = (mouseY - state.panY) / oldZoom;
            state.panX = (float) (mouseX - worldMouseX * state.zoom);
            state.panY = (float) (mouseY - worldMouseY * state.zoom);
        }
        return true;
    }
}
