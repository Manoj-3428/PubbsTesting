package in.pubbs.pubbsadmin;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

public class QRScannerOverlayView extends View {
    
    private Paint paint;
    private Paint linePaint;
    private int cornerLength = 50;
    private int cornerWidth = 5;
    private int scanningLinePosition = 0;
    private boolean scanningDirection = true; // true = down, false = up
    
    public QRScannerOverlayView(Context context) {
        super(context);
        init();
    }
    
    public QRScannerOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public QRScannerOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        // Enable hardware acceleration and set layer type for proper transparency
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(cornerWidth);
        
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.parseColor("#2196F3")); // Light blue scanning line
        linePaint.setStyle(Paint.Style.FILL);
        linePaint.setStrokeWidth(2);
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        int width = getWidth();
        int height = getHeight();
        
        // Calculate scanning area size dynamically (75% of width for wider box)
        int boxSize = (int) (width * 0.75f); // 75% of screen width for wider scanning box
        
        int centerX = width / 2;
        int centerY = height / 2;
        
        // Calculate scanning area bounds
        int left = centerX - boxSize / 2;
        int top = centerY - boxSize / 2;
        int right = centerX + boxSize / 2;
        int bottom = centerY + boxSize / 2;
        
        // Draw semi-transparent overlay with hole in center using Path
        Path overlayPath = new Path();
        overlayPath.addRect(0, 0, width, height, Path.Direction.CW);
        overlayPath.addRect(left, top, right, bottom, Path.Direction.CCW); // Counter-clockwise creates hole
        
        Paint overlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        overlayPaint.setColor(Color.parseColor("#80000000")); // Semi-transparent black
        overlayPaint.setStyle(Paint.Style.FILL);
        canvas.drawPath(overlayPath, overlayPaint);
        
        // Draw corner brackets (L-shapes) - white
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(cornerWidth);
        paint.setStyle(Paint.Style.STROKE);
        
        // Top-left corner
        canvas.drawLine(left, top, left + cornerLength, top, paint); // Horizontal
        canvas.drawLine(left, top, left, top + cornerLength, paint); // Vertical
        
        // Top-right corner
        canvas.drawLine(right - cornerLength, top, right, top, paint); // Horizontal
        canvas.drawLine(right, top, right, top + cornerLength, paint); // Vertical
        
        // Bottom-left corner
        canvas.drawLine(left, bottom - cornerLength, left, bottom, paint); // Vertical
        canvas.drawLine(left, bottom, left + cornerLength, bottom, paint); // Horizontal
        
        // Bottom-right corner
        canvas.drawLine(right - cornerLength, bottom, right, bottom, paint); // Horizontal
        canvas.drawLine(right, bottom - cornerLength, right, bottom, paint); // Vertical
        
        // Draw scanning line (light blue, moving up and down)
        int maxLinePosition = boxSize;
        if (scanningLinePosition >= 0 && scanningLinePosition <= maxLinePosition) {
            int lineY = top + scanningLinePosition;
            linePaint.setColor(Color.parseColor("#2196F3")); // Light blue
            canvas.drawLine(left, lineY, right, lineY, linePaint);
        }
    }
    
    public void updateScanningLine(int position) {
        this.scanningLinePosition = position;
        invalidate();
    }
    
    public void resetScanningLine() {
        this.scanningLinePosition = 0;
        this.scanningDirection = true;
        invalidate();
    }
    
    public int getScanningAreaSize() {
        if (getWidth() > 0) {
            return (int) (getWidth() * 0.75f); // 75% of width
        }
        return 300; // Default fallback
    }
}

