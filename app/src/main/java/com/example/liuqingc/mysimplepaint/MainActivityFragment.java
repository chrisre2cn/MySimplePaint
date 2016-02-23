package com.example.liuqingc.mysimplepaint;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.IOException;
import java.util.Stack;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    private static String TAG = "touch log:";
    private Uri bmpUri;
    private ShareActionProvider mShareActionProvider;
    private View rootview;
    private ImageView mImageView;
    private Paint mPaint = new Paint();
    private Bitmap bmp,basicbmp;
    private Canvas mCanvas;
    private boolean dragging;
    private float startx, starty,lastx,lasty,lastr;
    private Button mUndo,mDone;
    private MenuItem shareItem;
    private String basename;
    private SeekBar brushBar;
    private static float scaleFactor;
    private  Matrix mMatrix, mSaveMatrix;
    private boolean firstTouch=true, picked=false;
//    private TextView brushText;
    private float[] iValues = new float[9];
    private Spinner drawSpinner;
    private ArrayAdapter<CharSequence> drawAdapter;
    private int drawMode;
    static final int CIRCLE=0;
    static final int RECT=1;
    static final int TEXT=2;
    static final int PICK=3;
    static final int PANZOOM=4;

    String m_Text = null;

    private Sprite pickedSprite, moveSprite;

    // colors

    static final Integer colors [] = new Integer[] {Color.BLUE,Color.CYAN,Color.DKGRAY,Color.GRAY,
            Color.GREEN	,Color.LTGRAY,Color.MAGENTA,Color.RED,Color.WHITE,Color.YELLOW};


    //    drag&zoom
    PointF start = new PointF();
    PointF mid = new PointF();
    float oldDist = 1f;
    static final int NONE = 0;
    static final int DRAG = 1;
    static final int ZOOM = 2;
    int mode = NONE;

//   end drag&zoom

    private Stack<Sprite> spriteStack;

    private class Sprite {

        public int type;
        public float x;
        public float y;
        public float r;
        public Paint p;
        public String t;
        public float bx,by;
        public boolean visible=true;
        public Sprite savedSprite;

        public Sprite(Sprite s) {
            this.type = s.type;
            this.x = s.x;
            this.y = s.y;
            this.r = s.r;
            this.p = new Paint(s.p);
            if (s.t!=null)  this.t = s.t.toString();
            this.bx = s.bx;
            this.by = s.by;
        }

        public Sprite(float x, float y, float r, Paint p) {
            this.type = CIRCLE;
            this.x = x;
            this.y = y;
            this.r = r;
            this.p = new Paint(p);
        }

        public Sprite(float x, float y,  float bx, float by, Paint p) {
            this.type = RECT;
            this.x = x;
            this.y = y;
            this.p =  new Paint(p);
            this.bx = bx;
            this.by = by;
        }

        public Sprite(float x, float y, String t, Paint p) {
            this.type=TEXT;
            this.x = x;
            this.y = y;
            this.t = t.toString();
            this.p =  new Paint(p);
            float w=p.getStrokeWidth();
            if (w>5)
                this.p.setStrokeWidth(5);
            this.p.setTextSize(48+w*4);
        }

        public void draw( Canvas c) {

            if (visible) {

                switch (this.type) {
                    case CIRCLE:
                        c.drawCircle(x, y, r, p);
                        break;
                    case RECT:
                        c.drawRect(x, y, bx, by, p);
                        break;
                    case TEXT:
                        c.drawText(t, x, y, p);
                        break;
                }

            }
        }

        public void drawBalloon( Canvas c) {

            if (visible) {

                Paint ballonP = new Paint(p);
                ballonP.setAlpha(100);
                ballonP.setStyle(Paint.Style.FILL);
                c.drawCircle(x, y, 25, ballonP);


            }
        }

    }

    private void drawSprites() {

        mCanvas.drawBitmap(basicbmp, 0, 0, null);

        for (Sprite c : spriteStack) {
            c.draw(mCanvas);
            if (drawMode==PICK)
                c.drawBalloon(mCanvas);
        }
    }

    public MainActivityFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootview = inflater.inflate(R.layout.fragment_main, container, false);
        spriteStack  = new Stack();
//        brushText = (TextView) rootview.findViewById(R.id.textView1);
        mImageView = (ImageView) rootview.findViewById(R.id.imageView);
        Bitmap temp_bmp=null;
        final Uri imageUri = ((MainActivity) getActivity()).bmpUri;

        if ( imageUri!=null) {

            try {
                temp_bmp = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), imageUri);
                basename = imageUri.getLastPathSegment();
            } catch (IOException e) {
                Log.v(TAG,e.getMessage());

                temp_bmp =Bitmap.createBitmap(getResources().getConfiguration().screenWidthDp,
                getResources().getConfiguration().screenHeightDp,Bitmap.Config.ARGB_8888);
                temp_bmp.eraseColor(Color.WHITE);

                basename="untitled";
            }

        }
        else {
            temp_bmp =Bitmap.createBitmap(getResources().getConfiguration().screenWidthDp,
                    getResources().getConfiguration().screenHeightDp,Bitmap.Config.ARGB_8888);
            temp_bmp.eraseColor(Color.WHITE);
             basename="untitled";

        }


        brushBar=(SeekBar) rootview.findViewById(R.id.brushBar);
        brushBar.setProgress(1);
        bmp = temp_bmp.copy(Bitmap.Config.ARGB_8888,true);
        temp_bmp.recycle();
        basicbmp=bmp.copy(Bitmap.Config.ARGB_8888,true);
        mCanvas = new Canvas(bmp);
        dragging=false;


        mImageView.setImageBitmap(bmp);
//        mImageView.setScaleType(ImageView.ScaleType.MATRIX);
       mImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

//
        mMatrix= new Matrix();
        mSaveMatrix = new Matrix();


        mPaint.setColor(Color.RED);
        mPaint.setAntiAlias(true);

        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(2);
        mPaint.setTextSize(48f);


        mUndo = (Button) rootview.findViewById(R.id.undo_button);
        mUndo.setEnabled(false);
        mUndo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Sprite s = spriteStack.pop();

                if (s.savedSprite!=null) // this is a moved object; undo is to make hidden previous sprite visible
                    s.savedSprite.visible=true;

                drawSprites();


                mImageView.invalidate();
                if (spriteStack.empty())
                    mUndo.setEnabled(false);


            }
        });

        mDone = (Button) rootview.findViewById(R.id.done_button);

        mDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                drawMode=PANZOOM;
                drawSpinner.setSelection(PANZOOM);

                drawSprites();

                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                try {
                    bmpUri = Uri.parse(MediaStore.Images.Media.insertImage(getActivity().getContentResolver(), bmp, basename, basename));
                } catch (Exception e) {
                    Log.v(TAG, e.getMessage());
                }

                if (bmpUri != null) {
                    Toast toast = Toast.makeText(getContext(), "Ready to export via share button.", Toast.LENGTH_SHORT);
                    toast.show();
//                    Log.v(TAG,bmpUri.toString());
                    shareIntent.putExtra(Intent.EXTRA_STREAM, bmpUri);
                    shareIntent.setType("image/*");
                    mShareActionProvider.setShareIntent(shareIntent);
                    shareItem.setEnabled(true);
                    shareItem.setVisible(true);

                }

            }
        });

        mImageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent e) {
                if (firstTouch) {

// imageView completely initialized now. Read matrix from view.
                    mMatrix = new Matrix(mImageView.getImageMatrix());
                    mSaveMatrix = new Matrix(mMatrix);

                    mMatrix.getValues(iValues);
                    scaleFactor = Math.max(iValues[Matrix.MSCALE_X], iValues[Matrix.MSCALE_Y]);

                    mImageView.setScaleType(ImageView.ScaleType.MATRIX);
                    mImageView.setImageMatrix(mMatrix);

                    firstTouch = false;
                }

                if (drawMode >= 0 && drawMode < 3) {  // drawing something

                    final int index = e.getActionIndex();
                    final float[] coords = new float[]{e.getX(index), e.getY(index)};
                    Matrix matrix = new Matrix();
                    ((ImageView) view).getImageMatrix().invert(matrix);
                    matrix.mapPoints(coords);

                    switch (MotionEventCompat.getActionMasked(e)) {

                        case MotionEvent.ACTION_DOWN: //first finger down only
                            if (!dragging) {
                                lastx = startx = coords[0];
                                lasty = starty = coords[1];
                                lastr = 0;
                                dragging = true;

                            }
                            break;

                        case MotionEvent.ACTION_UP://first finger lifted
                            switch (drawMode) {
                                case CIRCLE:
                                    if (lastr>0)
                                        spriteStack.push(new Sprite(lastx, lasty, lastr, mPaint));
                                    break;
                                case RECT:
                                    if (startx!=coords[0] || starty!=coords[1])
                                        spriteStack.push(new Sprite(Math.min(startx,coords[0]), Math.min(starty,coords[1]),
                                                Math.max(startx, coords[0]), Math.max(starty, coords[1]), mPaint));
                                    break;
                                case TEXT:
                                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                    builder.setTitle("Input text");
                                    // I'm using fragment here so I'm using getView() to provide ViewGroup
                                    // but you can provide here any other instance of ViewGroup from your Fragment / Activity
                                    View viewInflated = LayoutInflater.from(getContext()).inflate(R.layout.inputtext, (ViewGroup) getView(), false);
                                    // Set up the input
                                    final EditText input = (EditText) viewInflated.findViewById(R.id.input);
                                    // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                                    builder.setView(viewInflated);

                                    // Set up the buttons
                                    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                            m_Text = input.getText().toString();
                                            if (m_Text != null) {
                                                spriteStack.push(new Sprite(coords[0], coords[1], m_Text, mPaint));
                                                m_Text = null;
                                                if (!spriteStack.empty())
                                                    mUndo.setEnabled(true);
                                            }
                                            drawSprites();
//                                        mImageView.invalidate();


                                        }
                                    });
                                    builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.cancel();
                                        }
                                    });

                                    builder.show();
//                                    if (m_Text!=null) {
//                                        spriteStack.push(new Sprite(coords[0],coords[1],m_Text,mPaint));
//                                        m_Text=null;
//                                    }
                                    break;

                            }

                            drawSprites();
                            dragging = false;
                            mImageView.invalidate();
                            if (!spriteStack.empty())
                                mUndo.setEnabled(true);
                            break;

                        case MotionEvent.ACTION_MOVE:

                            if (dragging) { //movement of first finger
                                drawSprites();
                                switch (drawMode) {
                                    case CIRCLE:
                                        lastx = (coords[0] + startx) / 2;
                                        lasty = (coords[1] + starty) / 2;
                                        lastr = Math.max(Math.abs(coords[0] - startx), Math.abs(coords[1] - starty)) / 2;
                                        mCanvas.drawCircle(lastx, lasty, lastr, mPaint);
                                        break;
                                    case RECT:
                                        mCanvas.drawRect(startx, starty, coords[0], coords[1], mPaint);
                                        break;

                                }


                                mImageView.invalidate();
                            }

                            break;
                    }

                } else if (drawMode == PANZOOM)// dragging or zooming
                {
                    // make the image scalable as a matrix
                    float scale;

                    // Handle touch events here...
                    switch (MotionEventCompat.getActionMasked(e)) {

                        case MotionEvent.ACTION_DOWN: //first finger down only

                            mSaveMatrix.set(mMatrix);
                            start.set(e.getX(), e.getY());
                            mode = DRAG;
                            break;

                        case MotionEvent.ACTION_UP: //first finger lifted
                        case MotionEvent.ACTION_POINTER_UP: //second finger lifted
                            mode = NONE;
                            break;

                        case MotionEvent.ACTION_POINTER_DOWN: //second finger down
                            oldDist = spacing(e); // calculates the distance between two points where user touched.
                            // minimal distance between both the fingers
                            if (oldDist > 5f) {
                                mSaveMatrix.set(mMatrix);
                                midPoint(mid, e); // sets the mid-point of the straight line between two points where user touched.
                                mode = ZOOM;
                            }
                            break;

                        case MotionEvent.ACTION_MOVE:
                            if (mode == DRAG /*&& zoomed*/) { //movement of first finger
                                mMatrix.set(mSaveMatrix);

                                float xtrans = e.getX() - start.x;
                                float ytrans = e.getY() - start.y;

                                final float[] coords1 = new float[]{0, 0};
                                final float[] coords2 = new float[]{0, bmp.getHeight()};
//                                final float[] coords3 = new float[]{bmp.getWidth(), 0};
                                final float[] coords4 = new float[]{bmp.getWidth(), bmp.getHeight()};
//            matrix.postTranslate(view.getScrollX(), view.getScrollY());
                                mMatrix.mapPoints(coords1);
                                mMatrix.mapPoints(coords4);
                                mMatrix.mapPoints(coords2);

                                if (coords1[0] >= 0) {
                                    if (coords4[0] <= view.getWidth())
                                        xtrans = 0;
                                    else if (xtrans > 0)
                                        xtrans = 0;
                                } else if (coords4[0] <= view.getWidth())
                                    if (xtrans < 0)
                                        xtrans = 0;


                                if (coords1[1] >= 0) {
                                    if (coords2[1] <= view.getHeight())
                                        ytrans = 0;
                                    else if (ytrans > 0) {
                                        ytrans = 0;
                                    }
                                } else if (coords2[1] <= view.getHeight()) {
                                    if (ytrans < 0)
                                        ytrans = 0;
                                }


                                mMatrix.postTranslate(xtrans, ytrans);

                                mSaveMatrix.set(mMatrix);
                                start.set(e.getX(), e.getY());


                            } else if (mode == ZOOM) { //pinch zooming

//          if (!zoomed) {
//              zoomed = true;
//
//
//          }

                                float newDist = spacing(e);

                                if (newDist > 5f) {
                                    mMatrix.set(mSaveMatrix);
                                    scale = newDist / oldDist; //thinking I need to play around with this value to limit it**
                                    if (scale > 1)
                                        mMatrix.postScale(scale, scale, mid.x, mid.y);
                                    else {

                                        float xcent = mid.x;
                                        float ycent = mid.y;
                                        final float[] coords1 = new float[]{0, 0};
                                        final float[] coords2 = new float[]{0, bmp.getHeight()};
//                                        final float[] coords3 = new float[]{bmp.getWidth(), 0};
                                        final float[] coords4 = new float[]{bmp.getWidth(), bmp.getHeight()};
//            matrix.postTranslate(view.getScrollX(), view.getScrollY());
                                        mMatrix.mapPoints(coords1);
                                        mMatrix.mapPoints(coords4);
                                        int w = view.getWidth();
                                        int h = view.getHeight();

                                        if (coords1[0] >= 0) {
                                            if (coords4[0] <= w)
                                                xcent = (coords1[0] + coords4[0]) / 2;
                                            else xcent = coords1[0];

                                        } else if (coords4[0] <= w)
                                            xcent = coords4[0];


                                        mMatrix.mapPoints(coords2);


                                        if (coords1[1] >= 0) {
                                            if (coords2[1] <= view.getHeight())
                                                ycent = (coords2[1] + coords1[1]) / 2;
                                            else {
                                                ycent = coords1[1];
                                            }
                                        } else if (coords2[1] <= view.getHeight()) {
                                            ycent = coords2[1];
                                        }

                                        mMatrix.postScale(scale, scale, xcent, ycent);


                                    }
                                    float[] values = new float[9];
                                    mMatrix.getValues(values);
                                    float scaleX = values[Matrix.MSCALE_X];


                                    if (scaleX > 1)
                                        mMatrix.set(mSaveMatrix);
                                    if (scaleX < scaleFactor)
                                        mMatrix.set(mSaveMatrix);
                                    mSaveMatrix.set(mMatrix);
                                    oldDist = newDist;

                                }
                            }
                            break;
                    }

                    // Perform the transformation
                    ((ImageView) view).setImageMatrix(mMatrix);

                } else if (drawMode == PICK ) {

                    final int index = e.getActionIndex();
                    final float[] coords = new float[]{e.getX(index), e.getY(index)};
                    Matrix matrix = new Matrix();
                    ((ImageView) view).getImageMatrix().invert(matrix);
                    matrix.mapPoints(coords);

                    switch (MotionEventCompat.getActionMasked(e)) {

                        case MotionEvent.ACTION_DOWN: //first finger down only
                            if (!dragging) {
                                lastx = startx = coords[0];
                                lasty = starty = coords[1];
                                lastr = 0;
                                dragging = true;

                            for (Sprite c : spriteStack) {
                                if (c.visible && Math.abs(c.x - startx) < 25 && Math.abs(c.y - starty) < 25) {
                                    pickedSprite = c;
                                    picked = true;
                                    break;
                                }
                            }
                                if ( picked ) {
                                    moveSprite = new Sprite(pickedSprite);
                                    pickedSprite.visible = false;
                                    moveSprite.p.setStrokeWidth(moveSprite.p.getStrokeWidth() + 10);
                                    drawSprites();
                                    moveSprite.draw(mCanvas);
                                    mImageView.invalidate();
                                }

                            }

                            break;

                        case MotionEvent.ACTION_UP://first finger lifted

                            if (picked) {

                                if (startx != coords[0] && starty != coords[1]) {
                                    moveSprite.p.setStrokeWidth(moveSprite.p.getStrokeWidth() - 10);
                                    moveSprite.savedSprite = pickedSprite;
                                    spriteStack.push(moveSprite);
                                } else
                                    pickedSprite.visible = true;

                                drawSprites();
//                        for (Sprite c : spriteStack)
//                            c.drawBalloon(mCanvas);
                                dragging = false;
                                mImageView.invalidate();
                                picked = false;
                            }
                            dragging = false;
                        break;

                        case MotionEvent.ACTION_MOVE:

                            if (picked && dragging) { //movement of first finger
                                drawSprites();
                                moveSprite.x+=(coords[0]-lastx);
                                moveSprite.y+=(coords[1]-lasty);
                                if ( moveSprite.type == RECT) {
                                    moveSprite.bx+=(coords[0]-lastx);
                                    moveSprite.by+=(coords[1]-lasty);
                                }

                                moveSprite.draw(mCanvas);

                                lastx=coords[0];
                                lasty=coords[1];

                            mImageView.invalidate();

                            }


//                            mImageView.invalidate();
                            break;
                    }


                }







                return true;
            }
        });

        brushBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                        mPaint.setStrokeWidth(progress+5);
                mPaint.setStrokeWidth( progress+1);

//                if ( progress >0) {
//                    mPaint.setStrokeWidth( progress+1);
//                    isDrawing=true;
//                    brushText.setText("Brush size "+progress);
//                } else{
//                    isDrawing=false;
//                    brushText.setText("Drag/Zoom mode");
//                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


  // prepare draw_spinner

        drawSpinner  = (Spinner) rootview.findViewById(R.id.draw_spinner);
        drawAdapter = ArrayAdapter.createFromResource(getContext(),R.array.draw_array, android.R.layout.simple_spinner_item);
        drawAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        drawSpinner.setAdapter(drawAdapter);
        drawSpinner.setSelection(0);
        drawMode=CIRCLE;
        drawSpinner.setBackgroundColor(Color.RED);

        drawSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                drawMode = position;

                    drawSprites();

                Log.v(TAG, "drawMode " + drawMode);

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        ListView mListView = (ListView) rootview.findViewById(R.id.color_listView);


        colorAdapter MyAdapter = new colorAdapter(getContext(),0);
        MyAdapter.addAll(colors);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mPaint.setColor(colors[position]);
                drawSpinner.setBackgroundColor(colors[position]);
            }
        });

        mListView.setAdapter(MyAdapter);

        return rootview;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.share_menu, menu);


           shareItem = menu.findItem(R.id.menu_item_share);

            shareItem.setEnabled(false);
        shareItem.setVisible(false);


            mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(shareItem);
//            Intent shareIntent = new Intent();
//            shareIntent.setAction(Intent.ACTION_SEND);
//            shareIntent.putExtra(Intent.EXTRA_STREAM, bmpUri);
//            shareIntent.setType("image/*");
//            mShareActionProvider.setShareIntent(shareIntent);



    }


    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
}

private void midPoint(PointF point, MotionEvent event) {
   float x = event.getX(0) + event.getX(1);
   float y = event.getY(0) + event.getY(1);
   point.set(x / 2, y / 2);
}


        public class colorAdapter extends ArrayAdapter<Integer> {

        private Context context;

        public colorAdapter(Context context, int resource) {
            super(context, resource);
            this.context=context;
//            resource is layout, not used here
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
  //          return super.getView(position, convertView, parent);
   //               ImageView imageView;
        if (convertView == null) {
            LayoutInflater mInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            convertView = (ImageView) mInflater.inflate(R.layout.color_item, parent, false);

       }
            ((ImageView) convertView).setBackgroundColor(this.getItem(position));

            return convertView;

        }
    }


}
