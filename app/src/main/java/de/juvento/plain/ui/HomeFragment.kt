package de.juvento.plain.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.children
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import de.juvento.plain.MainActivity
import de.juvento.plain.MainViewModel
import de.juvento.plain.R
import de.juvento.plain.data.AppModel
import de.juvento.plain.data.Constants.Action
import de.juvento.plain.data.Constants.AppDrawerFlag
import de.juvento.plain.data.HomeApp
import de.juvento.plain.data.PREF_ACTIVE_SET
import de.juvento.plain.data.PREF_APP_SETS
import de.juvento.plain.data.Prefs
import de.juvento.plain.databinding.FragmentHomeBinding
import de.juvento.plain.helper.ActionService
import de.juvento.plain.helper.expandNotificationDrawer
import de.juvento.plain.helper.expandQuickSettings
import de.juvento.plain.helper.getUserHandleFromString
import de.juvento.plain.helper.hideStatusBar
import de.juvento.plain.helper.initActionService
import de.juvento.plain.helper.isDefaultLauncher
import de.juvento.plain.helper.openAccessibilitySettings
import de.juvento.plain.helper.openAlarmApp
import de.juvento.plain.helper.openCalendar
import de.juvento.plain.helper.openCameraApp
import de.juvento.plain.helper.openDialerApp
import de.juvento.plain.helper.showStatusBar
import de.juvento.plain.listener.SwipeTouchListener
import de.juvento.plain.style.UiStyle

private const val HOME_APPS_MIN_TOP_DP = 40
private const val HOME_APPS_BOTTOM_DP = 40

class HomeFragment : Fragment() {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // SharedPreferences only keeps weak references, so the listener has to be a field.
    private val setsChangedListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == PREF_APP_SETS || key == PREF_ACTIVE_SET) _binding?.let { renderHomeApps(UiStyle.snapshot(requireContext(), prefs)) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        prefs = Prefs(requireContext())

        if (prefs.firstSettingsOpen()) {
            binding.firstRunTips.visibility = View.VISIBLE
            binding.setDefaultLauncher.visibility = View.GONE
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        binding.dateTimeLayout.addOnLayoutChangeListener { _, _, top, _, bottom, _, oldTop, _, oldBottom ->
            if (top != oldTop || bottom != oldBottom) updateHomeAppsPadding()
        }
        binding.touchArea.setOnTouchListener(gestureListener(requireContext(), null))
        binding.homeAppsScroll.emptyAreaTouchListener = gestureListener(requireContext(), null)
        binding.clock.setOnClickListener {
            val action = prefs.clickClockAction
            if (action == Action.OpenApp) openClickClockApp() else handleOtherAction(action)
        }
        binding.date.setOnClickListener {
            val action = prefs.clickDateAction
            if (action == Action.OpenApp) openClickDateApp() else handleOtherAction(action)
        }
        binding.setDefaultLauncher.setOnClickListener { (requireActivity() as MainActivity).requestDefaultLauncher() }
    }

    override fun onStart() {
        super.onStart()
        applyPrefs()
        prefs.registerChangeListener(setsChangedListener)
    }

    override fun onStop() {
        prefs.unregisterChangeListener(setsChangedListener)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        // only show "set as default"-button if tips are GONE
        if (binding.firstRunTips.visibility == View.GONE) {
            binding.setDefaultLauncher.visibility =
                if (isDefaultLauncher(requireContext())) View.GONE else View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun applyPrefs() {
        if (prefs.showStatusBar) showStatusBar(requireActivity()) else hideStatusBar(requireActivity())

        binding.clock.textSize = prefs.textSize * 2.5f
        binding.date.textSize = prefs.textSize.toFloat()
        binding.dateTimeLayout.gravity = prefs.clockAlignment.value()
        binding.clock.visibility = if (prefs.showTime) View.VISIBLE else View.GONE
        binding.date.visibility = if (prefs.showDate) View.VISIBLE else View.GONE

        updateHomeAppsPadding()

        val verticalGravity = if (prefs.homeAlignmentBottom) Gravity.BOTTOM else Gravity.CENTER_VERTICAL
        binding.homeAppsLayout.gravity = prefs.homeAlignment.value() or verticalGravity

        val style = UiStyle.snapshot(requireContext(), prefs)
        style.apply(binding.clock, binding.date, binding.setDefaultLauncher, binding.emptySetHint)
        style.apply(*binding.firstRunTips.children.filterIsInstance<TextView>().toList().toTypedArray())
        renderHomeApps(style)
    }

    // The home apps start below clock and date; without them they may use the space at the top.
    private fun updateHomeAppsPadding() {
        val dateTime = binding.dateTimeLayout
        val dateTimeVisible = prefs.showTime || prefs.showDate
        val top = if (dateTimeVisible) dateTime.bottom else 0
        binding.homeAppsScroll.updatePadding(
            top = maxOf(top, HOME_APPS_MIN_TOP_DP.dp()),
            bottom = HOME_APPS_BOTTOM_DP.dp(),
        )
    }

    private fun Int.dp() = (this * resources.displayMetrics.density).toInt()

    private fun renderHomeApps(style: UiStyle.Snapshot) {
        val container = binding.homeAppsLayout
        container.removeAllViews()

        val apps = prefs.activeSet.apps
        binding.emptySetHint.visibility = if (apps.isEmpty()) View.VISIBLE else View.GONE

        val gravity = prefs.homeAlignment.value()
        val width = if (prefs.extendHomeAppsArea) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT
        for (app in apps) {
            val view = layoutInflater.inflate(R.layout.home_app_button, container, false) as TextView
            view.text = app.label
            view.textSize = prefs.textSize.toFloat()
            view.gravity = gravity
            view.layoutParams = LinearLayout.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            view.setOnTouchListener(gestureListener(requireContext(), view) { launchHomeApp(app) })
            style.apply(view)
            container.addView(view)
        }
    }

    private fun launchHomeApp(app: HomeApp) {
        viewModel.launchApp(app.appPackage, app.activityName, getUserHandleFromString(requireContext(), app.user), app.shortcutId)
    }

    private fun launchApp(appModel: AppModel) = viewModel.launchApp(appModel)

    private fun showAppList() {
        viewModel.getAppList()
        navigate(R.id.action_mainFragment_to_appListFragment, bundleOf("flag" to AppDrawerFlag.LaunchApp.name))
    }

    private fun openSettings() = navigate(R.id.action_mainFragment_to_settingsFragment)

    private fun navigate(actionId: Int, args: Bundle? = null) {
        try {
            findNavController().navigate(actionId, args)
        } catch (e: IllegalArgumentException) {
            // Already navigating away (e.g. a second gesture event); nothing to do.
        }
    }

    private fun openSwipeRightApp() {
        if (prefs.appSwipeRight.appPackage.isNotEmpty()) launchApp(prefs.appSwipeRight)
        else openDialerApp(requireContext())
    }

    private fun openSwipeDownApp() {
        if (prefs.appSwipeDown.appPackage.isNotEmpty()) launchApp(prefs.appSwipeDown)
        else openDialerApp(requireContext())
    }

    private fun openSwipeUpApp() {
        if (prefs.appSwipeUp.appPackage.isNotEmpty()) launchApp(prefs.appSwipeUp)
        else showAppList()
    }

    private fun openClickClockApp() {
        if (prefs.appClickClock.appPackage.isNotEmpty()) launchApp(prefs.appClickClock)
        else openAlarmApp(requireContext())
    }

    private fun openClickDateApp() {
        if (prefs.appClickDate.appPackage.isNotEmpty()) launchApp(prefs.appClickDate)
        else openCalendar(requireContext())
    }

    private fun openSwipeLeftApp() {
        if (prefs.appSwipeLeft.appPackage.isNotEmpty()) launchApp(prefs.appSwipeLeft)
        else openCameraApp(requireContext())
    }

    private fun openDoubleTapApp() {
        if (prefs.appDoubleTap.appPackage.isNotEmpty()) launchApp(prefs.appDoubleTap)
        else openCameraApp(requireContext())
    }

    // Handles all actions that do not depend on the gesture they were bound to.
    private fun handleOtherAction(action: Action) {
        when (action) {
            Action.ShowNotification -> expandNotificationDrawer(requireContext())
            Action.LockScreen -> lockPhone()
            Action.ShowAppList -> showAppList()
            Action.OpenQuickSettings -> expandQuickSettings(requireContext())
            Action.ShowRecents -> initActionService(requireContext())?.showRecents()
            Action.OpenApp, Action.Disabled -> {} // OpenApp is handled by the respective gesture
        }
    }

    private fun lockPhone() {
        val actionService = ActionService.instance()
        if (actionService != null) actionService.lockScreen() else openAccessibilitySettings(requireContext())
    }

    private fun runGesture(action: Action, openApp: () -> Unit) {
        if (action == Action.OpenApp) openApp() else handleOtherAction(action)
    }

    /** Gestures of the empty home screen area ([view] == null) and of the home app entries. */
    private fun gestureListener(context: Context, view: View?, onTap: (() -> Unit)? = null) =
        object : SwipeTouchListener(context, view) {
            override fun onSwipeLeft() = runGesture(prefs.swipeLeftAction, ::openSwipeLeftApp)
            override fun onSwipeRight() = runGesture(prefs.swipeRightAction, ::openSwipeRightApp)
            override fun onSwipeUp() = runGesture(prefs.swipeUpAction, ::openSwipeUpApp)
            override fun onSwipeDown() = runGesture(prefs.swipeDownAction, ::openSwipeDownApp)
            override fun onLongClick() = openSettings()

            override fun onClick() {
                onTap?.invoke()
            }

            override fun onDoubleClick() {
                if (view == null) runGesture(prefs.doubleTapAction, ::openDoubleTapApp)
            }
        }
}
