package com.automattic.simplenote;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.automattic.simplenote.utils.DisplayUtils;
import com.automattic.simplenote.utils.ThemeUtils;
import com.automattic.simplenote.widgets.NoteEditorViewPager;

import org.wordpress.passcodelock.AppLockManager;

import java.util.ArrayList;

public class NoteEditorActivity extends AppCompatActivity {
    private TabLayout mTabLayout;
    private NoteEditorFragmentPagerAdapter mNoteEditorFragmentPagerAdapter;
    private NoteEditorViewPager mViewPager;
    private boolean isMarkdownEnabled;
    private String mNoteId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.setTheme(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_note_editor);

        // No title, please.
        setTitle("");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        NoteEditorFragment noteEditorFragment;
        NoteMarkdownFragment noteMarkdownFragment;

        mNoteEditorFragmentPagerAdapter =
                new NoteEditorFragmentPagerAdapter(getSupportFragmentManager());
        mViewPager = findViewById(R.id.pager);
        mTabLayout = findViewById(R.id.tabs);

        Intent intent = getIntent();
        mNoteId = intent.getStringExtra(NoteEditorFragment.ARG_ITEM_ID);

        if (savedInstanceState == null) {
            // Create the note editor fragment
            Bundle arguments = new Bundle();
            arguments.putString(NoteEditorFragment.ARG_ITEM_ID, mNoteId);

            boolean isNewNote = intent.getBooleanExtra(NoteEditorFragment.ARG_NEW_NOTE, false);
            arguments.putBoolean(NoteEditorFragment.ARG_NEW_NOTE, isNewNote);
            if (intent.hasExtra(NoteEditorFragment.ARG_MATCH_OFFSETS))
                arguments.putString(NoteEditorFragment.ARG_MATCH_OFFSETS,
                        intent.getStringExtra(NoteEditorFragment.ARG_MATCH_OFFSETS));

            noteEditorFragment = new NoteEditorFragment();
            noteEditorFragment.setArguments(arguments);
            noteMarkdownFragment = new NoteMarkdownFragment();
            noteMarkdownFragment.setArguments(arguments);

            mNoteEditorFragmentPagerAdapter.addFragment(
                    noteEditorFragment,
                    getString(R.string.tab_edit)
            );
            mNoteEditorFragmentPagerAdapter.addFragment(
                    noteMarkdownFragment,
                    getString(R.string.tab_preview)
            );
            mViewPager.setPagingEnabled(false);
            mViewPager.addOnPageChangeListener(
                    new NoteEditorViewPager.OnPageChangeListener() {
                        @Override
                        public void onPageSelected(int position) {
                            if (position == 1) {
                                final InputMethodManager imm = (InputMethodManager) getSystemService(
                                        Context.INPUT_METHOD_SERVICE);
                                if (imm != null) {
                                    imm.hideSoftInputFromWindow(mViewPager.getWindowToken(), 0);
                                }
                            }
                        }

                        @Override
                        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                        }

                        @Override
                        public void onPageScrollStateChanged(int state) {
                        }
                    }
            );

            isMarkdownEnabled = intent.getBooleanExtra(NoteEditorFragment.ARG_MARKDOWN_ENABLED, false);
        } else {
            mNoteEditorFragmentPagerAdapter.addFragment(
                    getSupportFragmentManager().getFragment(savedInstanceState, getString(R.string.tab_edit)),
                    getString(R.string.tab_edit)
            );
            mNoteEditorFragmentPagerAdapter.addFragment(
                    getSupportFragmentManager().getFragment(savedInstanceState, getString(R.string.tab_preview)),
                    getString(R.string.tab_preview)
            );

            isMarkdownEnabled = savedInstanceState.getBoolean(NoteEditorFragment.ARG_MARKDOWN_ENABLED);
        }

        mViewPager.setAdapter(mNoteEditorFragmentPagerAdapter);
        mTabLayout.setupWithViewPager(mViewPager);

        // Show tabs if markdown is enabled for the current note.
        if (isMarkdownEnabled) {
            showTabs();
        }
    }

    @Override
    protected void onPause() {
        if (AppLockManager.getInstance().isAppLockFeatureEnabled()) {
            AppLockManager.getInstance().getAppLock().setExemptActivities(null);
        }

        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mNoteEditorFragmentPagerAdapter.getCount() > 0 && mNoteEditorFragmentPagerAdapter.getItem(0).isAdded()) {
            getSupportFragmentManager()
                    .putFragment(outState, getString(R.string.tab_edit), mNoteEditorFragmentPagerAdapter.getItem(0));
        }
        if (mNoteEditorFragmentPagerAdapter.getCount() > 1 && mNoteEditorFragmentPagerAdapter.getItem(1).isAdded()) {
            getSupportFragmentManager()
                    .putFragment(outState, getString(R.string.tab_preview), mNoteEditorFragmentPagerAdapter.getItem(1));
        }
        outState.putBoolean(NoteEditorFragment.ARG_MARKDOWN_ENABLED, isMarkdownEnabled);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // If changing to large screen landscape, we finish the activity to go back to
        // NotesActivity with the note selected in the multipane layout.
        if (DisplayUtils.isLargeScreen(this) &&
                newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE && mNoteId != null) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra(Simplenote.SELECTED_NOTE_ID, mNoteId);
            resultIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            setResult(Activity.RESULT_OK, resultIntent);
            finish();
        }
    }

    protected NoteMarkdownFragment getNoteMarkdownFragment() {
        return (NoteMarkdownFragment) mNoteEditorFragmentPagerAdapter.getItem(1);
    }

    public void hideTabs() {
        mTabLayout.setVisibility(View.GONE);
        mViewPager.setPagingEnabled(false);
    }

    public void showTabs() {
        mTabLayout.setVisibility(View.VISIBLE);
        mViewPager.setPagingEnabled(true);
    }

    private static class NoteEditorFragmentPagerAdapter extends FragmentPagerAdapter {
        private final ArrayList<Fragment> mFragments = new ArrayList<>();
        private final ArrayList<String> mTitles = new ArrayList<>();

        NoteEditorFragmentPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public int getCount() {
            return mFragments.size();
        }

        @Override
        public Fragment getItem(int position) {
            return mFragments.get(position);
        }

        @Override
        public int getItemPosition(@NonNull Object object) {
            return PagerAdapter.POSITION_NONE;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mTitles.get(position);
        }

        void addFragment(Fragment fragment, String title) {
            mFragments.add(fragment);
            mTitles.add(title);
            notifyDataSetChanged();
        }
    }
}
