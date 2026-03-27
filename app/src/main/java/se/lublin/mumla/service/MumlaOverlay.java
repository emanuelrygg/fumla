package se.lublin.mumla.service;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Button;

import se.lublin.humla.model.IChannel;
import se.lublin.humla.model.IUser;
import se.lublin.humla.util.HumlaObserver;
import se.lublin.mumla.R;
import se.lublin.mumla.Settings;
import se.lublin.mumla.channel.ChannelAdapter;

/**
 * An onscreen interactive overlay displaying the users in the current channel.
 * Created by andrew on 26/09/13.
 */
public class MumlaOverlay {
    private static final String TAG = MumlaOverlay.class.getName();

    private static final int MIN_VISIBLE_ROWS_WHEN_MULTIPLE = 2;
    private static final int MAX_VISIBLE_ROWS = 3;

    private static final int MIN_OVERLAY_WIDTH_DP = 160;

    private final HumlaObserver mObserver = new HumlaObserver() {
        @Override
        public void onUserTalkStateUpdated(IUser user) {
            if (mChannelAdapter != null) {
                mChannelAdapter.notifyDataSetChanged();
                updateOverlaySizeToContent();
            }
        }

        @Override
        public void onUserStateUpdated(IUser user) {
            if (user == null) return;

            try {
                if (user.getChannel() != null &&
                        mService.getSessionChannel() != null &&
                        user.getChannel().equals(mService.getSessionChannel())) {
                    if (mChannelAdapter != null) {
                        mChannelAdapter.notifyDataSetChanged();
                        updateOverlaySizeToContent();
                    }
                }
            } catch (IllegalStateException e) {
                Log.d(TAG, "exception in onUserStateUpdated: " + e);
            }
        }

        @Override
        public void onUserJoinedChannel(IUser user, IChannel newChannel, IChannel oldChannel) {
            int selfSession;
            try {
                selfSession = mService.getSessionId();
            } catch (IllegalStateException e) {
                Log.d(TAG, "exception in onUserJoinedChannel: " + e);
                return;
            }

            try {
                if (user.getSession() == selfSession) {
                    // Session user has changed channels
                    if (mChannelAdapter != null) {
                        mChannelAdapter.setChannel(mService.getSessionChannel());
                        mChannelAdapter.notifyDataSetChanged();
                        updateOverlaySizeToContent();
                    }
                } else if (mService.getSessionChannel() != null &&
                        (newChannel.getId() == mService.getSessionChannel().getId()
                                || oldChannel.getId() == mService.getSessionChannel().getId())) {
                    if (mChannelAdapter != null) {
                        mChannelAdapter.notifyDataSetChanged();
                    }
                }
            } catch (IllegalStateException e) {
                Log.d(TAG, "exception while updating overlay channel: " + e);
            }
        }
    };

    private final View mOverlayView;
    private final ListView mOverlayList;
    private ChannelAdapter mChannelAdapter;
    private Button mTalkButton;
    private final ImageView mCloseButton;
    private final ImageView mDragButton;
    private final View mTitleView;
    private final WindowManager.LayoutParams mOverlayParams;
    private boolean mShown = false;

    public static MumlaService mService;

    public MumlaOverlay(MumlaService service) {
        mService = service;
        mOverlayView = View.inflate(service, R.layout.overlay, null);
        mTalkButton = (Button) mOverlayView.findViewById(R.id.overlay_talk);
        mTalkButton.setText("PTT");
        mDragButton = mOverlayView.findViewById(R.id.overlay_drag);
        mCloseButton = mOverlayView.findViewById(R.id.overlay_close);
        mTitleView = mOverlayView.findViewById(R.id.overlay_title);
        mOverlayList = mOverlayView.findViewById(R.id.overlay_list);

        // Make sure the overlay tree is clickable.
        mOverlayView.setClickable(true);
        mOverlayView.setFocusable(false);
        mOverlayList.setClickable(true);
        mOverlayList.setLongClickable(true);

        // Drag by title bar.
        mTitleView.setOnTouchListener(new View.OnTouchListener() {
            private final WindowManager mWindowManager =
                    (WindowManager) mService.getSystemService(Context.WINDOW_SERVICE);

            private float mInitialTouchX;
            private float mInitialTouchY;
            private int mInitialParamX;
            private int mInitialParamY;
            private boolean mDragging;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        mInitialTouchX = event.getRawX();
                        mInitialTouchY = event.getRawY();
                        mInitialParamX = mOverlayParams.x;
                        mInitialParamY = mOverlayParams.y;
                        mDragging = false;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        int dx = (int) (event.getRawX() - mInitialTouchX);
                        int dy = (int) (event.getRawY() - mInitialTouchY);

                        if (Math.abs(dx) > 4 || Math.abs(dy) > 4) {
                            mDragging = true;
                            mOverlayParams.x = mInitialParamX + dx;
                            mOverlayParams.y = mInitialParamY + dy;
                            mWindowManager.updateViewLayout(mOverlayView, mOverlayParams);
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        return true;

                    default:
                        return false;
                }
            }
        });
            // Resize by drag handle.
        mDragButton.setOnTouchListener(new View.OnTouchListener() {
            private final WindowManager mWindowManager =
                    (WindowManager) mService.getSystemService(Context.WINDOW_SERVICE);

            private float mInitialX;
            private float mInitialY;
            private int mInitialWidth;
            private int mInitialHeight;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        mInitialX = event.getRawX();
                        mInitialY = event.getRawY();
                        mInitialWidth = mOverlayView.getWidth();
                        mInitialHeight = mOverlayView.getHeight();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        int newWidth = (int) (mInitialWidth + (event.getRawX() - mInitialX));
                        int newHeight = (int) (mInitialHeight + (event.getRawY() - mInitialY));

                        // Prevent tiny unusable overlay goblin mode.
                        mOverlayParams.width = Math.max(dpToPx(160), newWidth);
                        mOverlayParams.height = Math.max(dpToPx(120), newHeight);

                        mWindowManager.updateViewLayout(mOverlayView, mOverlayParams);
                        return true;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        return true;

                    default:
                        return false;
                }
            }
        });

        // Proper PTT touch handling.
        mTalkButton.setClickable(true);
        mTalkButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        mTalkButton.setText("TX");
                        mService.onTalkKeyDown();
                        return true;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.setPressed(false);
                        mTalkButton.setText("PTT");
                        mService.onTalkKeyUp();
                        return true;

                    default:
                        return false;
                }
            }
        });

        Settings settings = Settings.getInstance(service);
        boolean usingPtt = Settings.ARRAY_INPUT_METHOD_PTT.equals(settings.getInputMethod());
        setPushToTalkShown(usingPtt);

        mCloseButton.setClickable(true);
        mCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hide();
            }
        });

        mOverlayParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
        );
        mOverlayParams.gravity = Gravity.TOP | Gravity.LEFT;
        mOverlayParams.windowAnimations = android.R.style.Animation_Dialog;
    }

    public boolean isShown() {
        return mShown;
    }

    public void show() {
        if (mShown) return;

        try {
            mChannelAdapter = new ChannelAdapter(mService, mService.getSessionChannel());
            mOverlayList.setAdapter(mChannelAdapter);
            mChannelAdapter.notifyDataSetChanged();
            updateOverlaySizeToContent();
            mService.registerObserver(mObserver);

            WindowManager windowManager =
                    (WindowManager) mService.getSystemService(Context.WINDOW_SERVICE);
            windowManager.addView(mOverlayView, mOverlayParams);
            mShown = true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to show overlay", e);
        }
    }

    private void updateOverlaySizeToContent() {
        if (mOverlayList == null || mChannelAdapter == null) return;

        int adapterCount = mChannelAdapter.getCount();
        if (adapterCount <= 0) return;

        int visibleRows;
        if (adapterCount == 1) {
            visibleRows = 1;
        } else {
            visibleRows = Math.min(MAX_VISIBLE_ROWS,
                    Math.max(MIN_VISIBLE_ROWS_WHEN_MULTIPLE, adapterCount));
        }

        int totalRowHeight = 0;
        int maxRowWidth = 0;

        for (int i = 0; i < visibleRows; i++) {
            View row = mChannelAdapter.getView(i, null, mOverlayList);

            int widthSpec = View.MeasureSpec.makeMeasureSpec(dpToPx(320), View.MeasureSpec.AT_MOST);
            int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            row.measure(widthSpec, heightSpec);

            totalRowHeight += row.getMeasuredHeight();
            maxRowWidth = Math.max(maxRowWidth, row.getMeasuredWidth());
        }

        // Measure title/header area
        mTitleView.measure(
                View.MeasureSpec.makeMeasureSpec(dpToPx(320), View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );
        int titleHeight = mTitleView.getMeasuredHeight();
        int titleWidth = mTitleView.getMeasuredWidth();

        // Measure talk button if visible
        int talkHeight = 0;
        int talkWidth = 0;
        if (mTalkButton.getVisibility() == View.VISIBLE) {
            mTalkButton.measure(
                    View.MeasureSpec.makeMeasureSpec(dpToPx(320), View.MeasureSpec.AT_MOST),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            );
            talkHeight = mTalkButton.getMeasuredHeight();
            talkWidth = mTalkButton.getMeasuredWidth();
        }

        int contentWidth = Math.max(titleWidth, Math.max(maxRowWidth, talkWidth));
        contentWidth = Math.max(contentWidth, dpToPx(MIN_OVERLAY_WIDTH_DP));

        int verticalPadding =
                mOverlayView.getPaddingTop() + mOverlayView.getPaddingBottom()
                        + mOverlayList.getPaddingTop() + mOverlayList.getPaddingBottom();

        int contentHeight = titleHeight + totalRowHeight + talkHeight + verticalPadding;

        // Set list height exactly to visible content
        android.view.ViewGroup.LayoutParams listLp = mOverlayList.getLayoutParams();
        listLp.height = totalRowHeight;
        mOverlayList.setLayoutParams(listLp);

        mOverlayParams.width = contentWidth;
        mOverlayParams.height = WindowManager.LayoutParams.WRAP_CONTENT;

        if (mShown) {
            try {
                WindowManager windowManager =
                        (WindowManager) mService.getSystemService(Context.WINDOW_SERVICE);
                windowManager.updateViewLayout(mOverlayView, mOverlayParams);
            } catch (Exception e) {
                Log.w(TAG, "Failed to update overlay size", e);
            }
        }
    }

    public void hide() {
        if (!mShown) return;

        mShown = false;
        mService.unregisterObserver(mObserver);
        mOverlayList.setAdapter(null);

        try {
            WindowManager windowManager =
                    (WindowManager) mService.getSystemService(Context.WINDOW_SERVICE);
            windowManager.removeView(mOverlayView);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Overlay already removed", e);
        }
    }

    public void setPushToTalkShown(boolean showPtt) {
        mTalkButton.setVisibility(showPtt ? View.VISIBLE : View.GONE);
        mOverlayView.post(new Runnable() {
            @Override
            public void run() {
                updateOverlaySizeToContent();
            }
        });
    }

    private int dpToPx(int dp) {
        float density = mService.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}