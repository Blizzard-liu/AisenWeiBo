package org.aisen.weibo.sina.ui.activity.basic;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.m.common.context.GlobalContext;
import com.m.common.utils.ActivityHelper;
import com.m.common.utils.SystemBarUtils;
import com.m.support.inject.ViewInject;
import com.m.ui.activity.basic.BaseActivity;
import com.m.ui.fragment.ABaseFragment;
import com.m.ui.fragment.AStripTabsFragment;
import com.melnykov.fab.FloatingActionButton;

import org.aisen.weibo.sina.R;
import org.aisen.weibo.sina.base.AppContext;
import org.aisen.weibo.sina.base.AppSettings;
import org.aisen.weibo.sina.support.bean.MenuBean;
import org.aisen.weibo.sina.support.utils.AisenUtils;
import org.aisen.weibo.sina.ui.activity.publish.PublishActivity;
import org.aisen.weibo.sina.ui.fragment.basic.MenuFragment;
import org.aisen.weibo.sina.ui.fragment.basic.MenuGenerator;
import org.aisen.weibo.sina.ui.fragment.comment.CommentTabsFragment;
import org.aisen.weibo.sina.ui.fragment.draft.DraftFragment;
import org.aisen.weibo.sina.ui.fragment.mention.MentionTabsFragment;
import org.aisen.weibo.sina.ui.fragment.settings.AboutWebFragment;
import org.aisen.weibo.sina.ui.fragment.settings.SettingsPagerFragment;
import org.aisen.weibo.sina.ui.fragment.timeline.GroupSortFragment;
import org.aisen.weibo.sina.ui.fragment.timeline.TimelineTabsFragment;

/**
 * Created by wangdan on 15/4/12.
 */
public class MainActivity extends BaseActivity implements AisenActivityHelper.EnableSwipeback {

    private static final String ACTION_LOGIN = "org.aisen.sina.weibo.ACTION_LOGIN";
    private static final String ACTION_NOTIFICATION = "org.aisen.sina.weibo.ACTION_NOTIFICATION";
    private static final String ACTION_NOTIFICATION_MS = "org.aisen.sina.weibo.ACTION_NOTIFICATION_MS";
    private static final String ACTION_NOTIFICATION_MC = "org.aisen.sina.weibo.ACTION_NOTIFICATION_MC";

    public static final String FRAGMENT_TAG = "MainFragment";

    public static void login() {
        Intent intent = new Intent(GlobalContext.getInstance(), MainActivity.class);
        intent.setAction(ACTION_LOGIN);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        GlobalContext.getInstance().startActivity(intent);
    }

    public static Fragment getContentFragment(MainActivity activity) {
        return activity.getFragmentManager().findFragmentByTag("MainFragment");
    }

    @ViewInject(id = R.id.drawer)
    private DrawerLayout mDrawerLayout;

    private ActionBarDrawerToggle mDrawerToggle;

    private MenuBean lastSelectedMenu;

    private MenuFragment menuFragment;

    @ViewInject(id = R.id.fab, click = "newPublish")
    private FloatingActionButton btnFab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.as_ui_main);

        if (Build.VERSION.SDK_INT >= 19) {
            ViewGroup drawerRoot = (ViewGroup) findViewById(R.id.layDrawerRoot);
            drawerRoot.setPadding(drawerRoot.getPaddingLeft(),
                                    SystemBarUtils.getStatusBarHeight(this),
                                    drawerRoot.getPaddingRight(),
                                    drawerRoot.getBottom());
        }
        if (Build.VERSION.SDK_INT == 19) {
            ViewGroup rootMain = (ViewGroup) findViewById(R.id.layMainRoot);
            rootMain.setPadding(rootMain.getPaddingLeft(),
                                    rootMain.getPaddingTop(),
                                    rootMain.getPaddingRight(),
                                    rootMain.getBottom() + SystemBarUtils.getNavigationBarHeight(this));
        }

        mToolbar = getToolbar();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(false);

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                getToolbar(), R.string.draw_open, R.string.draw_close) {

            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);

                invalidateOptionsMenu();

                if (isToolbarShown())
                    btnFab.show(true);
            }

            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);

                invalidateOptionsMenu();

                btnFab.hide(true);
            }

        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        lastSelectedMenu = savedInstanceState == null ? null : (MenuBean) savedInstanceState.getSerializable("menu");

        if (ActivityHelper.getBooleanShareData("isFirstLaunch", true)) {
            ActivityHelper.putBooleanShareData("isFirstLaunch", false);

            mDrawerLayout.openDrawer(Gravity.LEFT);
            btnFab.hide();

            getSupportActionBar().setTitle(R.string.draw_timeline);
        } else {
            if (lastSelectedMenu != null)
                getSupportActionBar().setTitle(lastSelectedMenu.getTitleRes());
            else
                getSupportActionBar().setTitle(R.string.draw_timeline);
        }

        if (savedInstanceState == null) {
            String action = getIntent() != null ? getIntent().getAction() : null;
            String type = getActionType(action);

            menuFragment = MenuFragment.newInstance(type);
            getFragmentManager().beginTransaction().add(R.id.menu_frame, menuFragment, "MenuFragment").commit();

            // 添加右侧的抽屉
//            GroupsMenuFragment groupsMenuFragment = GroupsMenuFragment.newInstance();
//            getFragmentManager().beginTransaction().add(R.id.groups_frame, groupsMenuFragment, "GroupsMenuFragment").commit();
        } else {
            menuFragment = (MenuFragment) getFragmentManager().findFragmentByTag("MenuFragment");

            // 2014-8-30 解决因为状态保存而导致的耗时阻塞
            if (lastSelectedMenu.getType().equals("1"))
                onMenuSelected(lastSelectedMenu, true, null);
        }

//        registerHideableHeaderView(getToolbar());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (lastSelectedMenu != null)
            outState.putSerializable("menu", lastSelectedMenu);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (intent == null)
            return;

        String action = intent.getAction();

        MenuBean menuBean = MenuGenerator.generateMenu(getActionType(action));

        lastSelectedMenu = menuBean;

        onMenuSelected(menuBean, true, null);

        if ("1".equals(menuBean.getType())) {
            menuFragment.setAccountItem();
            menuFragment.setSelectedMenu(menuBean);
        }

        if (isDrawerOpened())
            closeDrawer();
    }

    private String getActionType(String action) {
        String type = null;
        // 处理点击Notification时，设置显示菜单
        if (ACTION_LOGIN.equals(action)) {
            type = "1";
        }
        else if (ACTION_NOTIFICATION.equals(action)) {
            type = getIntent().getStringExtra("type");
        }
        else if (ACTION_NOTIFICATION_MS.equals(action)) {
            ActivityHelper.putShareData("showMensitonType", "showMentionStatus");

            type = "2";
        }
        else if (ACTION_NOTIFICATION_MC.equals(action)) {
            ActivityHelper.putShareData("showMensitonType", "showMentionCmt");

            type = "2";
        }
        return type;
    }

    public boolean onMenuSelected(MenuBean menu, boolean replace, View view) {
        if (!replace && lastSelectedMenu != null && lastSelectedMenu.getType().equals(menu.getType())) {
            closeDrawer();
            return true;
        }

        int type = Integer.parseInt(menu.getType());

        ABaseFragment fragment = null;
        mStripView = null;
        mToolbar.setTranslationY(0);

        switch (type) {
        // 微博首页
        case 1:
            fragment = TimelineTabsFragment.newInstance();
            break;
        // 提及
        case 2:
            fragment = MentionTabsFragment.newInstance();
            break;
        // 评论
        case 3:
            fragment = CommentTabsFragment.newInstance();
            break;
            // 设置
        case 5:
            closeDrawer();

            SettingsPagerFragment.launch(this);
            return true;
        // 草稿箱
        case 6:
            fragment = DraftFragment.newInstance();
            break;
        }

        if (fragment == null)
            return true;

        getSupportActionBar().setSubtitle(null);
        getSupportActionBar().setTitle(menu.getTitleRes());

        FragmentTransaction ft = getFragmentManager().beginTransaction();
//		ft.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out,
//									android.R.animator.fade_in, android.R.animator.fade_out);
        ft.replace(R.id.content_frame, fragment, FRAGMENT_TAG).commit();

        lastSelectedMenu = menu;
        menuFragment.setSelectedMenu(menu);

        return false;
    }

    public void closeDrawer() {
        mDrawerLayout.closeDrawers();
    }

    public boolean isDrawerOpened() {
        return mDrawerLayout.isDrawerOpen(Gravity.LEFT) || mDrawerLayout.isDrawerOpen(Gravity.RIGHT);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        if (mDrawerToggle != null)
            mDrawerToggle.syncState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!AppContext.isLogedin())
            finish();

        // 更新FAB的颜色
        btnFab.setColorNormal(AisenUtils.getThemeColor(this));
        btnFab.setColorPressed(AisenUtils.getThemeColor(this));
        btnFab.setColorRipple(AisenUtils.getThemeColor(this));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // 私信授权成功，跳转到私信界面
        if (requestCode == 1212 && resultCode == Activity.RESULT_OK) {
            onMenuSelected(MenuGenerator.generateMenu("10"), true, null);
        }
    }

    void newPublish(View v) {
        PublishActivity.publishStatus(this, null);
    }

    private boolean canFinish = false;

    @Override
    public boolean onBackClick() {
        if (AppSettings.isAppResident()) {
            if (lastSelectedMenu != null && !"1".equals(lastSelectedMenu.getType())) {
                onMenuSelected(MenuGenerator.generateMenu("1"), true, null);
                return true;
            } else {
                if (isDrawerOpened()) {
                    closeDrawer();

                    return true;
                }
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addCategory(Intent.CATEGORY_HOME);
                startActivity(intent);
            }
            return true;
        }
        else {
            if (!canFinish) {
                canFinish = true;

                showMessage(R.string.comm_hint_exit);

                new Handler().postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        canFinish = false;
                    }

                }, 1500);

                return true;
            }

            setMDestory(true);
            return super.onBackClick();
        }
    }

    private Toolbar mToolbar;
    private View mStripView;

    private boolean isToolbarShown() {
        return mToolbar != null && mToolbar.getTranslationY() >= 0;
    }

    public void hideToolbar() {
        if (isToolbarShown()) {
            toggleToolbarShown(false);
        }
    }

    public void showToolbar() {
        if (!isToolbarShown()) {
            toggleToolbarShown(true);
        }
    }

    private AnimatorSet animatorSet;
    public void toggleToolbarShown(boolean shown) {
        if (mStripView == null) {
            Fragment fragment = getFragmentManager().findFragmentByTag(FRAGMENT_TAG);
            if (fragment != null && fragment instanceof AStripTabsFragment) {
                mStripView = ((AStripTabsFragment) fragment).getSlidingTabLayout();
            }
        }

        if (mToolbar == null)
            return;

        if (animatorSet != null && animatorSet.isRunning())
            return;

        if (isToolbarShown() && shown)
            return;
        else if (!isToolbarShown() && !shown)
            return;

        if (!btnFab.isVisible() && shown) {
            btnFab.show(true);
        }
        else if (btnFab.isVisible() && !shown) {
            btnFab.hide(true);
        }

        PropertyValuesHolder toolBarHolder = null;
        if (shown) {
            toolBarHolder = PropertyValuesHolder.ofFloat("translationY", -1 * mToolbar.getHeight(), 0);
        }
        else {
            toolBarHolder = PropertyValuesHolder.ofFloat("translationY", 0, -1 * mToolbar.getHeight());
        }
        ObjectAnimator toolbarObjectAnim = ObjectAnimator.ofPropertyValuesHolder(mToolbar, toolBarHolder);
        toolbarObjectAnim.setDuration(150);

        ObjectAnimator stripObjectAnim = null;
        if (mStripView != null) {
            PropertyValuesHolder stripHolder = null;
            if (shown) {
                stripHolder = PropertyValuesHolder.ofFloat("translationY", -1 * mStripView.getHeight(), 0);
            }
            else {
                stripHolder = PropertyValuesHolder.ofFloat("translationY", 0, -1 * mStripView.getHeight());
            }
            stripObjectAnim = ObjectAnimator.ofPropertyValuesHolder(mStripView, stripHolder);
            stripObjectAnim.setDuration(150);
        }

        AnimatorSet animSet = new AnimatorSet();
        animatorSet = animSet;
        if (shown) {
            if (stripObjectAnim != null) {
//                animSet.playSequentially(toolbarObjectAnim, stripObjectAnim);
                animSet.play(toolbarObjectAnim);
                stripObjectAnim.setStartDelay(100);
                animSet.play(stripObjectAnim);
            }
            else {
                animSet.play(toolbarObjectAnim);
            }
        }
        else {
            if (stripObjectAnim != null) {
//                animSet.playSequentially(stripObjectAnim, toolbarObjectAnim);
                animSet.play(stripObjectAnim);
                toolbarObjectAnim.setStartDelay(100);
                animSet.play(toolbarObjectAnim);
            }
            else {
                animSet.play(toolbarObjectAnim);
            }
        }
        animSet.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

//        SubMenu subMenu = menu.addSubMenu(R.id.mainMenus, 333, 1, getString(R.string.main_search));
//        subMenu.setIcon(R.drawable.ic_menu_search);
//        subMenu.set
//        for (int i = 0; i < AppContext.getGroups().getLists().size(); i++) {
//            Group group = AppContext.getGroups().getLists().get(i);
//            subMenu.add(100, i, i, group.getName());
//        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.friendGroups).setVisible(false);

        // 显示的是首页
        if (lastSelectedMenu != null) {
            if (lastSelectedMenu.getType().equals("1"))
                menu.findItem(R.id.friendGroups).setVisible(true);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item))
            return true;

        if (android.R.id.home == item.getItemId()) {
            if (mDrawerLayout.isDrawerVisible(GravityCompat.START))
                mDrawerLayout.closeDrawers();
            else
                mDrawerLayout.openDrawer(GravityCompat.START);

            return true;
        }

        // 关于
        if (item.getItemId() == R.id.about)
            AboutWebFragment.launchAbout(this);
            // 意见反馈
        else if (item.getItemId() == R.id.feedback)
            PublishActivity.publishFeedback(this);
            // 好友分组
        else if (item.getItemId() == R.id.friendGroups)
            GroupSortFragment.lanuch(this);
            // 搜索用户或者微博
//        else if (item.getItemId() == R.id.search)
//            SearchActivity.launch(this);
            // 退出
        else if (item.getItemId() == R.id.exitapp)
            finish();

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean canSwipe() {
        return false;
    }

}