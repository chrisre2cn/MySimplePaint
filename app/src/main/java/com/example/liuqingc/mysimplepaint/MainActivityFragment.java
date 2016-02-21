package com.example.liuqingc.mysimplepaint;

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
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
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
    private boolean isDrawing=false, zoomed=false, firstTouch=true;
    private TextView brushText;
    private float[] iValues = new float[9];



//    drag&zoom
PointF start = new PointF();
PointF mid = new PointF();
float oldDist = 1f;
    static final int NONE = 0;
static final int DRAG = 1;
static final int ZOOM = 2;
int mode = NONE;

//   end drag&zoom

    private Stack<circle> circleStack;

    private class circle {

        public float x;
        public float y;
        public float r;
        public Paint p;

        public circle(float x, float y, float r, Paint p) {
            this.x = x;
            this.y = y;
            this.r = r;
            this.p = new Paint(p);
        }
    }

    private void drawSprites() {

        mCanvas.drawBitmap(basicbmp, 0, 0, null);

        for (circle c : circleStack) mCanvas.drawCircle(c.x, c.y, c.r, c.p);
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
        circleStack  = new Stack();
        brushText = (TextView) rootview.findViewById(R.id.textView1);
        mImageView = (ImageView) rootview.findViewById(R.id.imageView);
        Bitmap temp_bmp=null;
        final Uri imageUri = ((MainActivity) getActivity()).bmpUri;

        if ( imageUri!=null) {

            try {
                temp_bmp = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), imageUri);
                basename = imageUri.getLastPathSegment();
            } catch (IOException e) {
                Log.v(TAG,e.getMessage());
               // temp_bmp =  BitmapFactory.decodeResource(getResources(), R.drawable.img);
                //temp_bmp =Bitmap.createBitmap(mImageView.getWidth(),mImageView.getHeight(),Bitmap.Config.ARGB_8888);
                            temp_bmp =Bitmap.createBitmap(getResources().getConfiguration().screenWidthDp,
                    getResources().getConfiguration().screenHeightDp,Bitmap.Config.ARGB_8888);
            temp_bmp.eraseColor(Color.BLUE);

                basename="untitled";
            }
            ;
        }
        else {
//            temp_bmp = BitmapFactory.decodeResource(getResources(), R.drawable.img);
            temp_bmp =Bitmap.createBitmap(getResources().getConfiguration().screenWidthDp,
                    getResources().getConfiguration().screenHeightDp,Bitmap.Config.ARGB_8888);
            temp_bmp.eraseColor(Color.BLUE);
             basename="untitled";

        }

        isDrawing = false;

        brushBar=(SeekBar) rootview.findViewById(R.id.brushBar);
        brushBar.setProgress(0);
        bmp = temp_bmp.copy(Bitmap.Config.ARGB_8888,true);
        temp_bmp.recycle();
        basicbmp=bmp.copy(Bitmap.Config.ARGB_8888,true);
        mCanvas = new Canvas(bmp);
        dragging=false;


        //scale paint brush according to image size / screen size
//        scaleFactor = (float) getResources().getConfiguration().screenWidthDp/bmp.getWidth();



        mImageView.setImageBitmap(bmp);
//        mImageView.setScaleType(ImageView.ScaleType.MATRIX);
       mImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

//
        mMatrix= new Matrix();
        mSaveMatrix = new Matrix();
//
//        RectF bmpR, viewR;
//
//        bmpR = new RectF(0,0,bmp.getWidth(),bmp.getHeight());
//        viewR = new RectF(0,0,getResources().getConfiguration().screenWidthDp,
//                    getResources().getConfiguration().screenHeightDp);
//
//        mMatrix.setRectToRect(bmpR,viewR, Matrix.ScaleToFit.CENTER);
//
//       mMatrix.getValues(iValues);
//        scaleFactor = iValues[Matrix.MSCALE_X];

//        mMatrix.setScale(scaleFactor, scaleFactor);
//        mImageView.setScaleType(ImageView.ScaleType.MATRIX);
//        mImageView.setImageMatrix(mMatrix);

        mPaint.setColor(Color.RED);
        mPaint.setAntiAlias(true);

//        mPaint.setStrokeWidth((brushBar.getProgress()+1)/scaleFactor);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mUndo = (Button) rootview.findViewById(R.id.undo_button);
        mUndo.setEnabled(false);
        mUndo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                circleStack.pop();

                drawSprites();


                mImageView.invalidate();
                if (circleStack.empty())
                    mUndo.setEnabled(false);


            }
        });

        mDone = (Button) rootview.findViewById(R.id.done_button);

        mDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                try {
                    bmpUri = Uri.parse(MediaStore.Images.Media.insertImage(getActivity().getContentResolver(), bmp, basename, basename));
                } catch (Exception e) {Log.v(TAG,e.getMessage());};
                if (bmpUri!=null) {
                    Toast toast = Toast.makeText(getContext(), "Ready to export via share button.",Toast.LENGTH_SHORT);
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
        mMatrix= new Matrix( mImageView.getImageMatrix());
        mSaveMatrix = new Matrix(mMatrix);

       mMatrix.getValues(iValues);
        scaleFactor = Math.max(iValues[Matrix.MSCALE_X],iValues[Matrix.MSCALE_Y]);

        mImageView.setScaleType(ImageView.ScaleType.MATRIX);
        mImageView.setImageMatrix(mMatrix);

                    firstTouch=false;
                }

        if ( isDrawing) {

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

                case MotionEvent.ACTION_UP: //first finger lifted
                    circleStack.push(new circle(lastx, lasty, lastr, mPaint));
                    drawSprites();
                    dragging = false;
                    mImageView.invalidate();
                    mUndo.setEnabled(true);
                    break;

                case MotionEvent.ACTION_MOVE:

                    if (dragging) { //movement of first finger
                        drawSprites();
                        lastx = (coords[0] + startx) / 2;
                        lasty = (coords[1] + starty) / 2;
                        lastr = Math.max(Math.abs(coords[0] - startx), Math.abs(coords[1] - starty)) / 2;

                        mCanvas.drawCircle(lastx, lasty, lastr, mPaint);

                        mImageView.invalidate();
                    }

                    break;
            }

        }

                else // dragging or zooming
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
      if (mode == DRAG /*&& zoomed*/ )
      { //movement of first finger
         mMatrix.set(mSaveMatrix);

          float xtrans =   e.getX() - start.x;
          float ytrans =   e.getY() - start.y;

          final float[] coords1 = new float[]{0,0};
          final float[] coords2 = new float[]{0,bmp.getHeight()};
          final float[] coords3 = new float[]{bmp.getWidth(),0};
          final float[] coords4 = new float[]{bmp.getWidth(),bmp.getHeight()};
//            matrix.postTranslate(view.getScrollX(), view.getScrollY());
            mMatrix.mapPoints(coords1);
          mMatrix.mapPoints(coords4);
          mMatrix.mapPoints(coords2);

          if( coords1[0] >= 0 ) {
               if (coords4[0]<=view.getWidth())
                  xtrans=0;
              else if (xtrans>0)
                   xtrans=0;
          } else if (coords4[0]<=view.getWidth())
              if(xtrans<0)
                  xtrans =0;


           if( coords1[1] >= 0 ) {
               if (coords2[1] <= view.getHeight())
                        ytrans=0;
               else if (ytrans>0) {
                        ytrans=0;
               }
          }else  if (coords2[1] <= view.getHeight()) {
              if (ytrans < 0)
                        ytrans=0;
          }


          mMatrix.postTranslate(xtrans, ytrans);

                mSaveMatrix.set(mMatrix);
                start.set(e.getX(), e.getY());


      }
      else if (mode == ZOOM) { //pinch zooming

//          if (!zoomed) {
//              zoomed = true;
//
//
//          }

         float newDist = spacing(e);

         if (newDist > 5f) {
            mMatrix.set(mSaveMatrix);
            scale = newDist/oldDist; //thinking I need to play around with this value to limit it**
             if ( scale > 1)
                    mMatrix.postScale(scale, scale, mid.x, mid.y);
             else {

                 float xcent =  mid.x;
                float ycent =   mid.y;
                 final float[] coords1 = new float[]{0,0};
                final float[] coords2 = new float[]{0,bmp.getHeight()};
                final float[] coords3 = new float[]{bmp.getWidth(),0};
                final float[] coords4 = new float[]{bmp.getWidth(),bmp.getHeight()};
//            matrix.postTranslate(view.getScrollX(), view.getScrollY());
            mMatrix.mapPoints(coords1);
             mMatrix.mapPoints(coords4);
            int w=view.getWidth();
                 int h=view.getHeight();

                 if( coords1[0] >= 0 ) {
                     if( coords4[0] <= w )
                         xcent=(coords1[0]+coords4[0])/2;
                     else xcent=coords1[0];

          } else if ( coords4[0] <= w )
                     xcent=coords4[0];




         mMatrix.mapPoints(coords2);


           if( coords1[1] >= 0 ) {
               if (coords2[1] <= view.getHeight())
                        ycent=(coords2[1]+coords1[1])/2;
               else  {
                        ycent = coords1[1];
               }
          }else  if (coords2[1] <= view.getHeight()) {
             ycent = coords2[1];
          }

        mMatrix.postScale(scale, scale, xcent, ycent);
          ;

             }
             float[] values = new float[9];
             mMatrix.getValues(values);
             float scaleX = values[Matrix.MSCALE_X];


             if (scaleX > 1 )
                 mMatrix.set(mSaveMatrix);
             if (scaleX < scaleFactor)
                 mMatrix.set(mSaveMatrix);
             mSaveMatrix.set(mMatrix);
             oldDist=newDist;

         }
      }
      break;
   }

   // Perform the transformation
            ((ImageView) view).setImageMatrix(mMatrix);

        }


                return true;
            }
        });

        brushBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                        mPaint.setStrokeWidth(progress+5);
                if ( progress >0) {
                    mPaint.setStrokeWidth( progress);
                    isDrawing=true;
                    brushText.setText("Brush size "+progress);
                } else{
                    isDrawing=false;
                    brushText.setText("Drag/Zoom mode");
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
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

}
