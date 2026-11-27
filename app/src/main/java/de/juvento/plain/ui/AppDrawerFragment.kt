package de.juvento.plain.ui

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.juvento.plain.MainViewModel
import de.juvento.plain.R
import de.juvento.plain.data.AppModel
import de.juvento.plain.data.Constants
import de.juvento.plain.data.Constants.AppDrawerFlag
import de.juvento.plain.data.Keywords
import de.juvento.plain.data.Prefs
import de.juvento.plain.databinding.DialogAppEditBinding
import de.juvento.plain.databinding.FragmentAppDrawerBinding
import de.juvento.plain.helper.openAppInfo
import de.juvento.plain.helper.uninstallApp
import de.juvento.plain.helper.unpinShortcut
import de.juvento.plain.style.UiStyle

class AppDrawerFragment : Fragment() {

    private var _binding: FragmentAppDrawerBinding? = null
    private val binding get() = _binding!!

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private lateinit var adapter: AppDrawerAdapter
    private lateinit var flag: AppDrawerFlag
    private var editDialog: AlertDialog? = null

    private val searchText: TextView
        get() = binding.search.findViewById(androidx.appcompat.R.id.search_src_text)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAppDrawerBinding.inflate(inflater, container, false)
        prefs = Prefs(requireContext())
        if (prefs.firstOpen()) binding.appDrawerTip.visibility = View.VISIBLE
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        flag = AppDrawerFlag.valueOf(arguments?.getString("flag") ?: AppDrawerFlag.LaunchApp.name)
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        val gravity = when (prefs.drawerAlignment) {
            Constants.Gravity.Left -> Gravity.LEFT
            Constants.Gravity.Center -> Gravity.CENTER
            Constants.Gravity.Right -> Gravity.RIGHT
        }
        val style = UiStyle.snapshot(requireContext(), prefs)

        adapter = AppDrawerAdapter(
            gravity = gravity,
            textSize = prefs.textSize,
            groupFavorites = flag == AppDrawerFlag.LaunchApp,
            style = style,
            clickListener = ::onAppClicked,
            longClickListener = ::showEditDialog,
            singleMatchListener = { app ->
                if (flag == AppDrawerFlag.LaunchApp && prefs.autoOpenApp) onAppClicked(app)
            },
        )

        searchText.gravity = gravity
        searchText.typeface = style.typeface
        searchText.setTextColor(style.color)
        searchText.setHintTextColor(style.color)
        style.apply(binding.appDrawerTip, binding.listEmptyHint)

        initViewModel()

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        binding.recyclerView.addOnScrollListener(getRecyclerViewOnScrollListener())

        if (flag == AppDrawerFlag.HiddenApps) binding.search.queryHint = getString(R.string.hidden_apps)
        binding.search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                adapter.launchFirstInList()
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.filter.filter(newText.orEmpty().trim())
                return false
            }
        })
    }

    private fun initViewModel() {
        val source = if (flag == AppDrawerFlag.HiddenApps) viewModel.hiddenApps else viewModel.appList
        source.observe(viewLifecycleOwner) { appList ->
            appList ?: return@observe
            binding.listEmptyHint.visibility = if (appList.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerView.layoutAnimation =
                AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_anim_from_bottom)
            adapter.setAppList(appList.toMutableList())
        }
    }

    override fun onStart() {
        super.onStart()
        showKeyboard()
    }

    override fun onStop() {
        editDialog?.dismiss()
        hideKeyboard()
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun hideKeyboard() {
        val search = _binding?.search ?: return
        search.clearFocus()
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(search.windowToken, 0)
    }

    private fun showKeyboard() {
        if (!prefs.autoShowKeyboard) return
        val field = searchText
        field.requestFocus()
        field.postDelayed({
            if (_binding == null) return@postDelayed
            field.requestFocus()
            WindowInsetsControllerCompat(requireActivity().window, field).show(WindowInsetsCompat.Type.ime())
        }, 100)
    }

    private fun onAppClicked(app: AppModel) {
        viewModel.launchApp(app)
        findNavController().popBackStack(R.id.mainFragment, false)
    }

    private fun showEditDialog(app: AppModel) {
        val dialogBinding = DialogAppEditBinding.inflate(layoutInflater)
        dialogBinding.editOriginalName.text = app.appLabel
        dialogBinding.editName.setText(app.appAlias)
        dialogBinding.editName.hint = app.appLabel
        dialogBinding.editKeywords.setText(Keywords.format(app.keywords))
        if (app.isShortcut) dialogBinding.actionUninstall.setText(R.string.remove_shortcut)
        dialogBinding.actionHide.setText(if (flag == AppDrawerFlag.HiddenApps) R.string.show else R.string.hide)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.save) { _, _ ->
                saveAppEdit(app, dialogBinding.editName, dialogBinding.editKeywords)
            }
            .setNeutralButton(R.string.reset) { _, _ ->
                saveAppEdit(app, null, null)
            }
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialogBinding.actionHide.setOnClickListener {
            dialog.dismiss()
            toggleHidden(app)
        }
        dialogBinding.actionFavorite.setText(if (app.isFavorite) R.string.remove_favorite else R.string.favorite)
        dialogBinding.actionFavorite.setOnClickListener {
            dialog.dismiss()
            toggleFavorite(app)
        }
        dialogBinding.actionInfo.setOnClickListener {
            dialog.dismiss()
            openAppInfo(requireContext(), app.user, app.appPackage)
            findNavController().popBackStack(R.id.mainFragment, false)
        }
        dialogBinding.actionUninstall.setOnClickListener {
            dialog.dismiss()
            if (app.isShortcut) {
                unpinShortcut(requireContext(), app)
                adapter.remove(app)
            } else {
                uninstallApp(requireContext(), app.appPackage)
            }
        }

        editDialog = dialog
        dialog.show()
    }

    /** Passing null fields resets the drawer name and the keywords. */
    private fun saveAppEdit(app: AppModel, nameField: EditText?, keywordsField: EditText?) {
        val name = nameField?.text?.toString()?.trim().orEmpty()
        app.appAlias = if (name == app.appLabel) "" else name
        app.keywords = Keywords.parse(keywordsField?.text?.toString().orEmpty())
        prefs.setAppAliasAndKeywords(app.prefKey, app.appAlias, app.keywords)
        adapter.refresh()
    }

    private fun toggleFavorite(app: AppModel) {
        val favorites = prefs.favoriteApps.toMutableSet()
        app.isFavorite = !app.isFavorite
        if (app.isFavorite) favorites.add(app.identity) else favorites.remove(app.identity)
        prefs.favoriteApps = favorites
        adapter.refresh()
    }

    private fun toggleHidden(app: AppModel) {
        val key = app.identity
        val hidden = prefs.hiddenApps.toMutableSet()
        if (flag == AppDrawerFlag.HiddenApps) hidden.remove(key) else hidden.add(key)
        prefs.hiddenApps = hidden

        adapter.remove(app)
        if (flag == AppDrawerFlag.HiddenApps && adapter.apps.isEmpty()) findNavController().popBackStack()
    }

    private fun getRecyclerViewOnScrollListener(): RecyclerView.OnScrollListener {
        return object : RecyclerView.OnScrollListener() {

            var onTop = false

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                when (newState) {
                    RecyclerView.SCROLL_STATE_DRAGGING -> {
                        onTop = !recyclerView.canScrollVertically(-1)
                        if (onTop) hideKeyboard()
                        if (onTop && !recyclerView.canScrollVertically(1))
                            findNavController().popBackStack()
                    }

                    RecyclerView.SCROLL_STATE_IDLE -> {
                        if (!recyclerView.canScrollVertically(1)) {
                            hideKeyboard()
                        } else if (!recyclerView.canScrollVertically(-1)) {
                            if (onTop) findNavController().popBackStack()
                            else showKeyboard()
                        }
                    }
                }
            }
        }
    }
}
