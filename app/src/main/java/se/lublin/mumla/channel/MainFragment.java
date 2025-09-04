/*
 * Copyright (C) 2014 Andrew Comminos
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package se.lublin.mumla.channel;
import static se.lublin.mumla.app.DrawerAdapter.mProvider;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.text.TextUtils;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import java.lang.reflect.Method;

import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.PagerTabStrip;
import androidx.viewpager.widget.ViewPager;

import java.util.ArrayList;
import java.util.List;

import se.lublin.humla.HumlaService;
import se.lublin.humla.IHumlaService;
import se.lublin.humla.IHumlaSession;
import se.lublin.humla.model.IUser;
import se.lublin.humla.model.WhisperTarget;
import se.lublin.humla.util.HumlaDisconnectedException;
import se.lublin.humla.util.HumlaObserver;
import se.lublin.humla.util.IHumlaObserver;
import se.lublin.humla.util.VoiceTargetMode;
import se.lublin.mumla.R;
import se.lublin.mumla.Settings;
import se.lublin.mumla.app.DrawerAdapter;
import se.lublin.mumla.util.HumlaServiceFragment;

/**
 * Class to encapsulate both a ChannelListFragment and ChannelChatFragment.
 * Created by andrew on 02/08/13.
 */
public class MainFragment extends HumlaServiceFragment implements SharedPreferences.OnSharedPreferenceChangeListener, ChatTargetProvider {
    private static final String TAG = MainFragment.class.getName();

    private ViewPager mViewPager;
    private PagerTabStrip mTabStrip;
    private Button mTalkButton;
    private View mTalkView;

    private View mTargetPanel;
    private ImageView mTargetPanelCancel;

    private TextView mTargetPanelText;

    private TextView mHeaderTitle;    // R.id.active_channel_title
    private TextView mHeaderSubtitle; // R.id.active_channel_subtitle

    private ChatTarget mChatTarget;
    /** Chat target listeners, notified when the chat target is changed. */
    private List<OnChatTargetSelectedListener> mChatTargetListeners = new ArrayList<OnChatTargetSelectedListener>();

    /** True iff the talk button has been hidden (e.g. when muted) */
    private boolean mTalkButtonHidden;

    private HumlaObserver mObserver = new HumlaObserver() {
        @Override
        public void onUserTalkStateUpdated(IUser user) {
            if (getService() == null || !getService().isConnected()) {
                return;
            }
            int selfSession;
            try {
                selfSession = getService().HumlaSession().getSessionId();
            } catch (HumlaDisconnectedException|IllegalStateException e) {
                Log.d(TAG, "exception in onUserTalkStateUpdated: " + e);
                return;
            }
            if (user != null && user.getSession() == selfSession) {
                // Manually set button selection colour when we receive a talk state update.
                // This allows representation of talk state when using hot corners and PTT toggle.
                switch (user.getTalkState()) {
                case TALKING:
                case SHOUTING:
                case WHISPERING:
                    mTalkButton.setPressed(true);
                    break;
                case PASSIVE:
                    mTalkButton.setPressed(false);
                    break;
                }
            }
            if (isAdded()) requireActivity().runOnUiThread(new Runnable() {
                @Override public void run() { updateHeaderFromActionBar(); }
            });
        }

        @Override
        public void onUserStateUpdated(IUser user) {
            if (getService() == null || !getService().isConnected()) {
                return;
            }
            int selfSession;
            try {
                selfSession = getService().HumlaSession().getSessionId();
            } catch (IllegalStateException e) {
                Log.d(TAG, "exception in onUserStateUpdated: " + e);
                return;
            }
            if (user != null && user.getSession() == selfSession) {
                configureInput();
            }
            if (isAdded()) requireActivity().runOnUiThread(new Runnable() {
                @Override public void run() { updateHeaderFromActionBar(); }
            });
        }

        @Override
        public void onVoiceTargetChanged(VoiceTargetMode mode) {
            configureTargetPanel();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // New XML keeps all existing IDs your code uses.
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        updateHeaderFromActionBar();
        mHeaderTitle   = view.findViewById(R.id.active_channel_title);
        mHeaderSubtitle= view.findViewById(R.id.active_channel_subtitle);

        // Preserve your original look: we still find and use these
        mViewPager = (ViewPager) view.findViewById(R.id.channel_view_pager);
        mTabStrip  = (PagerTabStrip) view.findViewById(R.id.channel_tab_strip);

        if (mTabStrip != null) {
            int[] attrs = new int[] { R.attr.colorPrimary, android.R.attr.textColorPrimaryInverse };
            TypedArray a = getActivity().obtainStyledAttributes(attrs);
            int titleStripBackground = a.getColor(0, -1);
            int titleStripColor      = a.getColor(1, -1);
            a.recycle();

            mTabStrip.setTextColor(titleStripColor);
            mTabStrip.setTabIndicatorColor(titleStripColor);
            mTabStrip.setBackgroundColor(titleStripBackground);
            mTabStrip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);

            // Hide the old tabs to show the single “active channel” PTT screen
            mTabStrip.setVisibility(View.GONE);
        }

        // We keep the ViewPager present so the rest of your lifecycle stays intact,
        // but hide it for the “single screen” design.
        if (mViewPager != null) {
            mViewPager.setVisibility(View.GONE);
        }

        final View ringView = view.findViewById(R.id.ptt_ring);
        final View talkView = view.findViewById(R.id.pushtotalk_view);
        View talkBtn = view.findViewById(R.id.pushtotalk);

        talkBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (getService()!=null) getService().onTalkKeyDown();
                        talkView.setPressed(true);   // inner circle color change
                        ringView.setPressed(true);   // outer ring turns red
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (getService()!=null) getService().onTalkKeyUp();
                        talkView.setPressed(false);
                        ringView.setPressed(false);
                        v.performClick();            // accessibility
                        return true;
                }
                return false;
            }
        });


        // Target/whisper panel (unchanged, IDs preserved)
        mTargetPanel        = view.findViewById(R.id.target_panel);
        mTargetPanelCancel  = (ImageView) view.findViewById(R.id.target_panel_cancel);
        mTargetPanelText    = (TextView) view.findViewById(R.id.target_panel_warning);

        mTargetPanelCancel.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (getService() == null || !getService().isConnected()) return;
                IHumlaSession session = getService().HumlaSession();
                if (session.getVoiceTargetMode() == VoiceTargetMode.WHISPER) {
                    byte target = session.getVoiceTargetId();
                    session.setVoiceTargetId((byte) 0);
                    session.unregisterWhisperTarget(target);
                }
            }
        });

        // Optional: if you want a placeholder until we wire the exact API
        TextView activeTitle = (TextView) view.findViewById(R.id.active_channel_title);
        if (activeTitle != null) {
            activeTitle.setText(getString(R.string.touch_and_hold_to_talk));
        }

        configureInput(); // your original method
        return view;
    }

    private void updateHeaderFromActionBar() {
        CharSequence title = null, subtitle = null;

        if (getActivity() instanceof AppCompatActivity) {
            ActionBar ab = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (ab != null) {
                title    = mProvider.getConnectedServerName();    // group/channel name is already put here by the app
                subtitle = mProvider.getConnectedServerName(); // server/host is already put here by the app
            }
        } else if (getActivity() != null) {
            title = getActivity().getTitle();
        }

        if (mHeaderTitle != null && title != null && title.length() > 0) {
            mHeaderTitle.setText(title);
        }
        if (mHeaderSubtitle != null && subtitle != null && subtitle.length() > 0) {
            mHeaderSubtitle.setText(subtitle);
        }
    }


    private String getActionBarSubtitle() {
        try {
            if (getActivity() instanceof AppCompatActivity) {
                ActionBar ab = ((AppCompatActivity) getActivity()).getSupportActionBar();
                if (ab != null && !isEmpty(ab.getSubtitle())) return ab.getSubtitle().toString();
            }
        } catch (Throwable ignored) { }
        return null;
    }

    private String tryGetServerNameReflectively() {
        // Try on service first, then session, with several common method names
        Object[] targets = new Object[] {
                getService(),
                (getService()!=null ? safeSession() : null)
        };
        String[] methodNames = new String[] {
                "getServerName", "getServerLabel", "getConnectionLabel",
                "getHost", "getHostname", "getAddress"
        };
        for (Object target : targets) {
            if (target == null) continue;
            for (String m : methodNames) {
                String v = tryInvokeString(target, m);
                if (!isEmpty(v)) return v;
            }
        }
        return null;
    }

    private IHumlaSession safeSession() {
        try { return getService().HumlaSession(); } catch (Throwable t) { return null; }
    }

    private String tryInvokeString(Object target, String methodName) {
        try {
            Method mm = target.getClass().getMethod(methodName);
            Object val = mm.invoke(target);
            return (val != null) ? String.valueOf(val) : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private boolean isEmpty(CharSequence cs) {
        return cs == null || cs.length() == 0;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        preferences.registerOnSharedPreferenceChangeListener(this);

        if(mViewPager != null) { // Phone
            ChannelFragmentPagerAdapter pagerAdapter = new ChannelFragmentPagerAdapter(getChildFragmentManager());
            mViewPager.setAdapter(pagerAdapter);
        } else { // Tablet
            ChannelListFragment listFragment = new ChannelListFragment();
            Bundle listArgs = new Bundle();
            listArgs.putBoolean("pinned", isShowingPinnedChannels());
            listFragment.setArguments(listArgs);
            ChannelChatFragment chatFragment = new ChannelChatFragment();

            getChildFragmentManager().beginTransaction()
                    .replace(R.id.list_fragment, listFragment)
                    .replace(R.id.chat_fragment, chatFragment)
                    .commit();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.channel_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Settings settings = Settings.getInstance(getActivity());
        int itemId = item.getItemId();
        if (itemId == R.id.menu_input_voice) {
            settings.setInputMethod(Settings.ARRAY_INPUT_METHOD_VOICE);
            return true;
        } else if (itemId == R.id.menu_input_ptt) {
            settings.setInputMethod(Settings.ARRAY_INPUT_METHOD_PTT);
            return true;
        } else if (itemId == R.id.menu_input_continuous) {
            settings.setInputMethod(Settings.ARRAY_INPUT_METHOD_CONTINUOUS);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getService() != null && getService().isConnected() &&
            !Settings.getInstance(getActivity()).isPushToTalkToggle()) {
            // XXX: This ensures that push to talk is disabled when we pause.
            // We don't want to leave the talk state active if the fragment is paused while pressed.
            getService().HumlaSession().setTalkingState(false);
        }
    }

    @Override
    public void onDestroy() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        preferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    @Override
    public IHumlaObserver getServiceObserver() {
        return mObserver;
    }

    @Override public void onResume() {
        super.onResume();
        View v = getView();
        if (v != null) updateHeaderFromActionBar();
    }


    @Override
    public void onServiceBound(IHumlaService service) {
        super.onServiceBound(service);
        if (service.getConnectionState() == HumlaService.ConnectionState.CONNECTED) {
            configureTargetPanel();
            configureInput();
        }
    }

    private void configureTargetPanel() {
        if (getService() == null || !getService().isConnected()) {
            return;
        }

        IHumlaSession session = getService().HumlaSession();
        VoiceTargetMode mode = session.getVoiceTargetMode();
        if (mode == VoiceTargetMode.WHISPER) {
            WhisperTarget target = session.getWhisperTarget();
            mTargetPanel.setVisibility(View.VISIBLE);
            mTargetPanelText.setText(getString(R.string.shout_target, target.getName()));
        } else {
            mTargetPanel.setVisibility(View.GONE);
        }
    }

    /**
     * @return true if the channel fragment is set to display only the user's pinned channels.
     */
    private boolean isShowingPinnedChannels() {
        return getArguments() != null &&
               getArguments().getBoolean("pinned");
    }

    /**
     * Configures the fragment in accordance with the user's interface preferences.
     */
    private void configureInput() {
        final View root = getView();
        if (root == null) return;

        // PTT views (safe lookups)
        if (mTalkView == null)   mTalkView   = root.findViewById(R.id.pushtotalk_view);
        if (mTalkButton == null) mTalkButton = root.findViewById(R.id.pushtotalk);

        // Legacy pager UI — may be missing/hidden on this layout. Guard everything.
        if (mViewPager == null)  mViewPager  = root.findViewById(R.id.channel_view_pager);
        if (mTabStrip  == null)  mTabStrip   = root.findViewById(R.id.channel_tab_strip);
        final View twoPane = root.findViewById(R.id.two_pane_container);

        if (mViewPager != null) mViewPager.setVisibility(View.GONE);
        if (twoPane != null)    twoPane.setVisibility(View.GONE);

        if (mTabStrip != null) {
            mTabStrip.setVisibility(View.GONE);
            // Never cast to LinearLayout.LayoutParams; use the generic type.
            ViewGroup.LayoutParams lp = mTabStrip.getLayoutParams();
            if (lp instanceof ViewGroup.MarginLayoutParams) {
                // If your old code adjusted margins/height, do it safely here.
                // ((ViewGroup.MarginLayoutParams) lp).topMargin = 0;
            }
            // Only call setLayoutParams if lp != null (it always is, but keep it defensive)
            if (lp != null) mTabStrip.setLayoutParams(lp);
        }

        // Keep any other original input setup below, but guard all view accesses:
        // if (someView != null) { ... }
    }


    private void setTalkButtonHidden(final boolean hidden) {
        mTalkView.setVisibility(hidden ? View.GONE : View.VISIBLE);
        mTalkButtonHidden = hidden;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(Settings.PREF_INPUT_METHOD.equals(key)
            || Settings.PREF_PUSH_BUTTON_HIDE_KEY.equals(key)
            || Settings.PREF_PTT_BUTTON_HEIGHT.equals(key))
            configureInput();
    }

    @Override
    public ChatTarget getChatTarget() {
        return mChatTarget;
    }

    @Override
    public void setChatTarget(ChatTarget target) {
        mChatTarget = target;
        for(OnChatTargetSelectedListener listener : mChatTargetListeners)
            listener.onChatTargetSelected(target);
    }

    @Override
    public void registerChatTargetListener(OnChatTargetSelectedListener listener) {
        mChatTargetListeners.add(listener);
    }

    @Override
    public void unregisterChatTargetListener(OnChatTargetSelectedListener listener) {
        mChatTargetListeners.remove(listener);
    }

    private class ChannelFragmentPagerAdapter extends FragmentPagerAdapter {

        public ChannelFragmentPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            Fragment fragment = null;
            Bundle args = new Bundle();
            switch (i) {
                case 0:
                    fragment = new ChannelListFragment();
                    args.putBoolean("pinned", isShowingPinnedChannels());
                    break;
                case 1:
                    fragment = new ChannelChatFragment();
                    break;
            }
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.channel).toUpperCase();
                case 1:
                    return getString(R.string.chat).toUpperCase();
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    }
}
