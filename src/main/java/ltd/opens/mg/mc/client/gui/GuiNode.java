package ltd.opens.mg.mc.client.gui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import ltd.opens.mg.mc.core.blueprint.NodeDefinition;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GuiNode {
    public String id;
    public String typeId;
    public NodeDefinition definition;
    public String title;
    public float x, y;
    public int color;
    public float width = 120;
    public float height = 60;
    public float headerHeight = 15;
    public JsonObject inputValues = new JsonObject();

    public List<NodePort> inputs = new ArrayList<>();
    public List<NodePort> outputs = new ArrayList<>();

    private boolean sizeDirty = true;

    public GuiNode(NodeDefinition def, float x, float y) {
        this.id = UUID.randomUUID().toString();
        this.typeId = def.id();
        this.definition = def;
        this.title = def.name();
        this.x = x;
        this.y = y;
        this.color = def.color();
        
        for (NodeDefinition.PortDefinition p : def.inputs()) {
            addInput(p.id(), p.displayName(), p.type(), p.color(), p.hasInput(), p.defaultValue(), p.options());
        }
        for (NodeDefinition.PortDefinition p : def.outputs()) {
            addOutput(p.id(), p.displayName(), p.type(), p.color());
        }
    }

    public void addInput(String id, String displayName, NodeDefinition.PortType type, int color, boolean hasInput, Object defaultValue, String[] options) {
        inputs.add(new NodePort(id, displayName, type, color, true, hasInput, defaultValue, options));
        sizeDirty = true;
    }

    public void addOutput(String id, String displayName, NodeDefinition.PortType type, int color) {
        outputs.add(new NodePort(id, displayName, type, color, false, false, null, null));
        sizeDirty = true;
    }

    private void updateSize(net.minecraft.client.gui.Font font) {
        // Calculate height
        int maxPorts = Math.max(inputs.size(), outputs.size());
        this.height = Math.max(40, headerHeight + 10 + maxPorts * 15 + 5);

        // Calculate width
        float minWidth = 100;
        float titleW = font.width(title) + 20;

        float maxInputW = 0;
        for (NodePort p : inputs) {
            float w = 10 + font.width(p.displayName);
            if (p.hasInput) {
                w += 55; // Space for input field
            }
            maxInputW = Math.max(maxInputW, w);
        }

        float maxOutputW = 0;
        for (NodePort p : outputs) {
            float w = 10 + font.width(p.displayName);
            maxOutputW = Math.max(maxOutputW, w);
        }

        this.width = Math.max(minWidth, Math.max(titleW, maxInputW + maxOutputW + 20));
        sizeDirty = false;
    }

    public float[] getPortPositionByName(String id, boolean isInput) {
        List<NodePort> ports = isInput ? inputs : outputs;
        for (int i = 0; i < ports.size(); i++) {
            if (ports.get(i).id.equals(id)) {
                return getPortPosition(i, isInput);
            }
        }
        return new float[]{x, y};
    }

    public NodePort getPortByName(String id, boolean isInput) {
        List<NodePort> ports = isInput ? inputs : outputs;
        for (NodePort p : ports) {
            if (p.id.equals(id)) return p;
        }
        return null;
    }

    public float[] getPortPosition(int index, boolean isInput) {
        float py = y + headerHeight + 10 + index * 15; // Exact center of port
        float px = isInput ? x : x + width;
        return new float[]{px, py};
    }

    public void render(GuiGraphics guiGraphics, net.minecraft.client.gui.Font font, int mouseX, int mouseY, float panX, float panY, float zoom, List<GuiConnection> connections, GuiNode focusedNode, String focusedPort) {
        if (sizeDirty) {
            updateSize(font);
        }
        // Shadow
        guiGraphics.fill((int) x + 2, (int) y + 2, (int) (x + width + 2), (int) (y + height + 2), 0x88000000);
        
        // Background
        guiGraphics.fill((int) x, (int) y, (int) (x + width), (int) (y + height), 0xEE1A1A1A);
        
        // Border (highlighted if mouse is over)
        double worldMouseX = (mouseX - panX) / zoom;
        double worldMouseY = (mouseY - panY) / zoom;
        boolean isHovered = worldMouseX >= x && worldMouseX <= x + width && worldMouseY >= y && worldMouseY <= y + height;
        int borderColor = isHovered ? 0xFFFFFFFF : 0xFF333333;
        guiGraphics.renderOutline((int) x, (int) y, (int) width, (int) height, borderColor);
        
        // Header
        guiGraphics.fill((int) x + 1, (int) y + 1, (int) (x + width - 1), (int) (y + headerHeight), color);
        
        // Title
        guiGraphics.drawString(font, title, (int) x + 5, (int) y + 4, 0xFFFFFFFF, true);

        // Render Inputs
        for (int i = 0; i < inputs.size(); i++) {
            renderPort(guiGraphics, font, inputs.get(i), (int) x, (int) (y + headerHeight + 10 + i * 15), true, connections, focusedNode, focusedPort);
        }

        // Render Outputs
        for (int i = 0; i < outputs.size(); i++) {
            renderPort(guiGraphics, font, outputs.get(i), (int) (x + width), (int) (y + headerHeight + 10 + i * 15), false, connections, focusedNode, focusedPort);
        }
    }

    private int getPortColor(NodePort port) {
        switch (port.type) {
            case EXEC: return 0xFFFFFFFF;
            case STRING: return 0xFFFF5555;
            case FLOAT: return 0xFF55FF55;
            case BOOLEAN: return 0xFF5555FF;
            case LIST: return 0xFFFFFF55;
            case UUID: return 0xFFFF55FF;
            case ENUM: return 0xFFFFAA00;
            case ANY: return 0xFFAAAAAA;
            default: return port.color;
        }
    }

    private void renderPort(GuiGraphics guiGraphics, net.minecraft.client.gui.Font font, NodePort port, int px, int py, boolean isInput, List<GuiConnection> connections, GuiNode focusedNode, String focusedPort) {
        int color = getPortColor(port);
        boolean isConnected = false;
        for (GuiConnection conn : connections) {
            if (isInput) {
                if (conn.to == this && conn.toPort.equals(port.id)) {
                    isConnected = true;
                    break;
                }
            } else {
                if (conn.from == this && conn.fromPort.equals(port.id)) {
                    isConnected = true;
                    break;
                }
            }
        }

        // Port rendering (more "Unreal Engine" style)
        float size = 4.0f;
        if (port.type == NodeDefinition.PortType.EXEC) {
            // Execution port: Arrow shape
            if (isConnected) {
                // Filled arrow
                renderTriangle(guiGraphics, px - size, py - size, px + size, py, px - size, py + size, color);
            } else {
                // Outline arrow
                renderTriangleOutline(guiGraphics, px - size, py - size, px + size, py, px - size, py + size, color);
            }
        } else {
            // Data port: Circle
            if (isConnected) {
                // Filled circle
                drawCircle(guiGraphics, px, py, (int)size, color);
            } else {
                // Hollow circle
                drawCircleOutline(guiGraphics, px, py, (int)size, color);
            }
        }

        // Port label
        if (isInput) {
            guiGraphics.drawString(font, port.displayName, px + 8, py - 1, 0xFFAAAAAA, false);
            
            if (port.hasInput && !isConnected) {
                float inputX = px + 8 + font.width(port.displayName) + 2;
                float inputY = py - 4;
                float inputWidth = 50;
                float inputHeight = 10;
                
                // Background
                guiGraphics.fill((int)inputX, (int)inputY, (int)(inputX + inputWidth), (int)(inputY + inputHeight), 0x66000000);
                
                if (port.type == NodeDefinition.PortType.BOOLEAN) {
                    JsonElement val = inputValues.get(port.id);
                    boolean boolVal = val != null ? val.getAsBoolean() : (port.defaultValue instanceof Boolean ? (Boolean) port.defaultValue : false);
                    
                    // Checkbox style
                    int boxColor = boolVal ? 0xFF36CF36 : 0xFF333333;
                    guiGraphics.fill((int)inputX + 2, (int)inputY + 2, (int)inputX + 8, (int)inputY + 8, boxColor);
                    guiGraphics.renderOutline((int)inputX + 1, (int)inputY + 1, 8, 8, 0xFFFFFFFF);
                    
                    String text = boolVal ? "True" : "False";
                    guiGraphics.drawString(font, text, (int)inputX + 12, (int)inputY + 1, 0xFFCCCCCC, false);
                } else if (port.options != null && port.options.length > 0) {
                    // Selection box style
                    guiGraphics.renderOutline((int)inputX, (int)inputY, (int)inputWidth, (int)inputHeight, 0xFFFFFFFF);
                    
                    JsonElement val = inputValues.get(port.id);
                    String text = val != null ? val.getAsString() : (port.defaultValue != null ? port.defaultValue.toString() : port.options[0]);
                    
                    // Draw selection arrow
                    guiGraphics.drawString(font, "v", (int)(inputX + inputWidth - 8), (int)inputY + 1, 0xFFAAAAAA, false);
                    
                    String renderText = text;
                    if (font.width(renderText) > inputWidth - 12) {
                        renderText = font.plainSubstrByWidth(renderText, (int)inputWidth - 15, true) + "..";
                    }
                    guiGraphics.drawString(font, renderText, (int)inputX + 2, (int)inputY + 1, 0xFFCCCCCC, false);
                } else {
                    // Border if focused
                    boolean isFocused = focusedNode == this && focusedPort != null && focusedPort.equals(port.id);
                    guiGraphics.renderOutline((int)inputX, (int)inputY, (int)inputWidth, (int)inputHeight, isFocused ? 0xFFFFFFFF : 0x33FFFFFF);
                    
                    // Text
                    JsonElement val = inputValues.get(port.id);
                    String text = val != null ? val.getAsString() : (port.defaultValue != null ? port.defaultValue.toString() : "");
                    
                    String renderText = text;
                    // Truncate text if too long
                    if (font.width(renderText) > inputWidth - 4) {
                        renderText = "..." + font.plainSubstrByWidth(renderText, (int)inputWidth - 10, true);
                    }
                    
                    guiGraphics.drawString(font, renderText, (int)inputX + 2, (int)inputY + 1, 0xFFCCCCCC, false);
                }
            }
        } else {
            guiGraphics.drawString(font, port.displayName, px - 8 - font.width(port.displayName), py - 1, 0xFFAAAAAA, false);
        }
    }

    public boolean isMouseOverHeader(double worldMouseX, double worldMouseY) {
        return worldMouseX >= x && worldMouseX <= x + width && worldMouseY >= y && worldMouseY <= y + headerHeight;
    }

    private void renderTriangle(GuiGraphics guiGraphics, float x1, float y1, float x2, float y2, float x3, float y3, int color) {
        // Optimized triangle fill: Use scanline but with fewer fill calls
        float minY = Math.min(y1, Math.min(y2, y3));
        float maxY = Math.max(y1, Math.max(y2, y3));
        
        int iMinY = (int) Math.floor(minY);
        int iMaxY = (int) Math.ceil(maxY);
        
        for (int y = iMinY; y <= iMaxY; y++) {
            float minX = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE;
            
            float[][] edges = {{x1, y1, x2, y2}, {x2, y2, x3, y3}, {x3, y3, x1, y1}};
            boolean intersected = false;
            for (float[] edge : edges) {
                float ey1 = edge[1], ey2 = edge[3];
                if ((ey1 <= y && ey2 > y) || (ey2 <= y && ey1 > y)) {
                    float ex1 = edge[0], ex2 = edge[2];
                    float ix = ex1 + (y - ey1) * (ex2 - ex1) / (ey2 - ey1);
                    minX = Math.min(minX, ix);
                    maxX = Math.max(maxX, ix);
                    intersected = true;
                }
            }
            if (intersected) {
                guiGraphics.fill((int)Math.floor(minX), y, (int)Math.ceil(maxX), y + 1, color);
            }
        }
    }

    private void renderTriangleOutline(GuiGraphics guiGraphics, float x1, float y1, float x2, float y2, float x3, float y3, int color) {
        drawLine(guiGraphics, x1, y1, x2, y2, color);
        drawLine(guiGraphics, x2, y2, x3, y3, color);
        drawLine(guiGraphics, x3, y3, x1, y1, color);
    }

    private void drawLine(GuiGraphics guiGraphics, float x1, float y1, float x2, float y2, int color) {
        // Use a single fill if it's almost horizontal or vertical
        if (Math.abs(x1 - x2) < 0.5f) {
            guiGraphics.fill((int)x1, (int)Math.min(y1, y2), (int)x1 + 1, (int)Math.max(y1, y2) + 1, color);
            return;
        }
        if (Math.abs(y1 - y2) < 0.5f) {
            guiGraphics.fill((int)Math.min(x1, x2), (int)y1, (int)Math.max(x1, x2) + 1, (int)y1 + 1, color);
            return;
        }

        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.5f) return;

        // Optimized line drawing for ports and outlines
        float step = 1.5f;
        int numSteps = (int)(len / step);
        float xInc = dx * step / len;
        float yInc = dy * step / len;
        float curX = x1;
        float curY = y1;

        for (int i = 0; i <= numSteps; i++) {
            guiGraphics.fill((int)curX, (int)curY, (int)curX + 1, (int)curY + 1, color);
            curX += xInc;
            curY += yInc;
        }
    }

    private void drawCircle(GuiGraphics guiGraphics, float cx, float cy, int radius, int color) {
        for (int y = -radius; y <= radius; y++) {
            int xSpan = (int) Math.sqrt(radius * radius - y * y);
            guiGraphics.fill((int)(cx - xSpan), (int)(cy + y), (int)(cx + xSpan + 1), (int)(cy + y + 1), color);
        }
    }

    private void drawCircleOutline(GuiGraphics guiGraphics, float cx, float cy, int radius, int color) {
        for (int y = -radius; y <= radius; y++) {
            int xSpan = (int) Math.sqrt(radius * radius - y * y);
            guiGraphics.fill((int)(cx - xSpan), (int)(cy + y), (int)(cx - xSpan + 1), (int)(cy + y + 1), color);
            guiGraphics.fill((int)(cx + xSpan), (int)(cy + y), (int)(cx + xSpan + 1), (int)(cy + y + 1), color);
        }
        for (int x = -radius; x <= radius; x++) {
            int ySpan = (int) Math.sqrt(radius * radius - x * x);
            guiGraphics.fill((int)(cx + x), (int)(cy - ySpan), (int)(cx + x + 1), (int)(cy - ySpan + 1), color);
            guiGraphics.fill((int)(cx + x), (int)(cy + ySpan), (int)(cx + x + 1), (int)(cy + ySpan + 1), color);
        }
    }

    public static class NodePort {
        public String id;
        public String displayName;
        public NodeDefinition.PortType type;
        public int color;
        public boolean isInput;
        public boolean hasInput;
        public Object defaultValue;
        public String[] options;

        public NodePort(String id, String displayName, NodeDefinition.PortType type, int color, boolean isInput, boolean hasInput, Object defaultValue, String[] options) {
            this.id = id;
            this.displayName = displayName;
            this.type = type;
            this.color = color;
            this.isInput = isInput;
            this.hasInput = hasInput;
            this.defaultValue = defaultValue;
            this.options = options;
        }
    }
}
