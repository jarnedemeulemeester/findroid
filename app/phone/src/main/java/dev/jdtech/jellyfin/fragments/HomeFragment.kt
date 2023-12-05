package dev.jdtech.jellyfin.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.CastState
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.AppPreferences
import dev.jdtech.jellyfin.adapters.ViewListAdapter
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.chromecast.ExpandedControlsActivity
import dev.jdtech.jellyfin.chromecast.SyncPlayDataSource
import dev.jdtech.jellyfin.chromecast.SyncPlayMedia
import dev.jdtech.jellyfin.databinding.FragmentHomeBinding
import dev.jdtech.jellyfin.dialogs.ErrorDialogFragment
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.FindroidShow
import dev.jdtech.jellyfin.models.PlayerItem
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.utils.Globals
import dev.jdtech.jellyfin.utils.checkIfLoginRequired
import dev.jdtech.jellyfin.utils.restart
import dev.jdtech.jellyfin.viewmodels.HomeViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.jellyfin.sdk.api.client.extensions.syncPlayApi
import org.jellyfin.sdk.api.sockets.addGeneralCommandsListener
import org.jellyfin.sdk.api.sockets.addListener
import org.jellyfin.sdk.api.sockets.addPlayStateCommandsListener
import org.jellyfin.sdk.api.sockets.addSyncPlayCommandsListener
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.GroupUpdateType
import org.jellyfin.sdk.model.api.JoinGroupRequestDto
import org.jellyfin.sdk.model.socket.GeneralCommandMessage
import org.jellyfin.sdk.model.socket.PlayMessage
import org.jellyfin.sdk.model.socket.PlayStateMessage
import org.jellyfin.sdk.model.socket.SessionsMessage
import org.jellyfin.sdk.model.socket.SyncPlayCommandMessage
import org.jellyfin.sdk.model.socket.SyncPlayGroupUpdateMessage
import timber.log.Timber
import javax.inject.Inject
import dev.jdtech.jellyfin.core.R as CoreR


@AndroidEntryPoint
class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private val viewModel: HomeViewModel by viewModels()

    private var originalSoftInputMode: Int? = null

    private lateinit var errorDialog: ErrorDialogFragment

    private lateinit var spinner: Spinner

    private var groups = ArrayList<String>()

    private var test: JellyfinApi? = null

    private var groupIds = ArrayList<java.util.UUID>()

    private var syncPlayDataSource: SyncPlayDataSource? = null
    @Inject
    lateinit var appPreferences: AppPreferences


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        setupView()
        bindState()

        return binding.root
    }

    private fun loadRemoteMedia(
        position: Long,
        mCastSession: CastSession,
        mediaInfo: MediaInfo,
        streamUrl: String,
        item: PlayerItem,
        episode: BaseItemDto,
    ) {
        if (mCastSession == null) {
            return
        }

        val remoteMediaClient = mCastSession.remoteMediaClient ?: return
        var previousSubtitleTrackIds: LongArray? = null
        var newIndex = -1
        var subtitleIndex = -1
        var newAudioIndex = 1

        val callback = object : RemoteMediaClient.Callback() {

            override fun onSendingRemoteMediaRequest() {


            }

            override fun onStatusUpdated() {
                val mediaStatus = remoteMediaClient.mediaStatus


            }
        }

        remoteMediaClient.registerCallback(callback)
        remoteMediaClient.load(
            MediaLoadRequestData.Builder()
                .setMediaInfo(mediaInfo)
                .setAutoplay(true)
                .setCurrentTime(position.toLong()).build(),
        )
        val mediaStatus = remoteMediaClient.mediaStatus
        val activeMediaTracks = mediaStatus?.activeTrackIds
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        test = JellyfinApi.getInstance(this.requireContext())
        syncPlayDataSource = SyncPlayDataSource(test!!)
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(CoreR.menu.home_menu, menu)
                    CastButtonFactory.setUpMediaRouteButton(
                        requireContext(),
                        menu,
                        CoreR.id.media_route_menu_item,
                    )


                    val spinnerItem = menu.add(Menu.NONE, Menu.NONE, 0, "Select an option")
                    spinnerItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)

                    val spinner = Spinner(requireContext())
                    spinner.adapter = createSpinnerAdapter()
                    val layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    layoutParams.width = 125
                    spinner.layoutParams = layoutParams
                    spinnerItem.actionView = spinner
                    spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            parent: AdapterView<*>,
                            view: View,
                            position: Int,
                            id: Long
                        ) {

                            if (position > 1) {

                                var item = BaseItemDto
                                var groupJoinRequest =
                                    JoinGroupRequestDto(groupIds.get(position - 2))

                                val castContext = CastContext.getSharedInstance(requireContext().applicationContext)
                                val session = castContext.sessionManager.currentCastSession
                                if (session == null || castContext.castState != CastState.CONNECTED) {

                                } else {
                                    val remoteMediaClient = session.remoteMediaClient ?: return
                                    remoteMediaClient.registerCallback(object :
                                        RemoteMediaClient.Callback() {
                                        override fun onStatusUpdated() {
                                            val intent = Intent(
                                                requireActivity(),
                                                ExpandedControlsActivity::class.java
                                            )
                                            startActivity(intent)
                                            remoteMediaClient.unregisterCallback(this)
                                        }
                                    })
                                }
                               Globals.syncPlay = true

                                viewModel.viewModelScope.launch {


                                    test!!.api.syncPlayApi.syncPlayJoinGroup(groupJoinRequest)
                                    val recentUpdate = syncPlayDataSource!!.latestUpdate.collectLatest {
                                        message ->
                                        print(message)
                                    }
                                    /*SyncPlayCast.startCast(
                                        test!!,
                                        groupIds.get(position - 2),
                                        requireContext(),
                                        viewModel.getRepository(),
                                        groupJoinRequest
                                    )*/

                                    //var mediaItem = getItemID(test!!, requireContext(), viewModel.getRepository())
                                    //print(mediaItem)
                                    //var streamUrl = viewModel.getRepository().getStreamCastUrl(mediaItem!!.itemID, mediaItem!!.itemID.toString().replace("-",""))
                                    print(recentUpdate)
                                    val mCastSession = CastContext.getSharedInstance(requireContext()).sessionManager.currentCastSession
                                    val remoteMediaClient = mCastSession!!.remoteMediaClient ?: return@launch
                                    //loadRemoteMedia(mediaItem.timestamp, mCastSession,MediaInfo.Builder("wjat",).setContentUrl(streamUrl).build(), api = test!! )
                                }


                            }

                            if (position == 1) {
                                // Execute your action here
                                Toast.makeText(
                                    requireContext(),
                                    "You selected: ${spinner.selectedItem} position + ${position}",
                                    Toast.LENGTH_SHORT
                                ).show()

                                CoroutineScope(Dispatchers.IO).launch {
                                    test!!.api.syncPlayApi.syncPlayLeaveGroup()
                                }
                                Globals.syncPlay = false
                            }
                        }

                        override fun onNothingSelected(parent: AdapterView<*>) {
                            // Do nothing if nothing is selected
                        }
                    }

                    val settings = menu.findItem(CoreR.id.action_settings)

                    val search = menu.findItem(CoreR.id.action_search)
                    val searchView = search.actionView as SearchView
                    searchView.queryHint = getString(CoreR.string.search_hint)

                    search.setOnActionExpandListener(
                        object : MenuItem.OnActionExpandListener {
                            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                                settings.isVisible = false
                                return true
                            }

                            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                                settings.isVisible = true
                                return true
                            }
                        },
                    )

                    searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                        override fun onQueryTextSubmit(p0: String?): Boolean {
                            if (p0 != null) {
                                navigateToSearchResultFragment(p0)
                            }
                            return true
                        }

                        override fun onQueryTextChange(p0: String?): Boolean {
                            return false
                        }
                    })
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        CoreR.id.action_settings -> {
                            navigateToSettingsFragment()
                            true
                        }

                        else -> false
                    }
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED,
        )
    }
    private fun loadRemoteMedia(
        position: Long,
        mCastSession: CastSession,
        mediaInfo: MediaInfo,
        api: JellyfinApi,
    ) {
        if (mCastSession == null) {
            return
        }
        var instance = api.api.ws()


        val remoteMediaClient = mCastSession.remoteMediaClient ?: return

        remoteMediaClient.load(
            MediaLoadRequestData.Builder()
                .setMediaInfo(mediaInfo)
                .setAutoplay(true)
                .setCurrentTime(position.toLong()).build(),
        )
    }
    private fun getItemID(
        test: JellyfinApi,
        requireContext: Context,
        repository: JellyfinRepository
    ): SyncPlayMedia? {
        val session =
            CastContext.getSharedInstance(requireContext()).sessionManager.currentCastSession

        var hasItemId: Boolean = false
        var ItemId: java.util.UUID? = null
        var startPositionTicks = 0
        var userID = ""

        //var instance = SocketInstance
        var mediaId: String
        var mediaUrl = ""
        var mediaIDString = ""

        var castUrl: String
        var instance = test.api.ws()
        var Groupmessage: JsonElement

        instance.addGeneralCommandsListener { message ->
            // type of message is GeneralCommandMessage
            println("Received a message: $message")
        }

        instance.addPlayStateCommandsListener { message ->
            // type of message is PlayStateMessage
            println("Received a message: $message")
        }

        instance.addListener<PlayMessage> { message ->
            // type of message is UserDataChangedMessage
            println("Received a message: $message")
        }

        instance.addListener<PlayStateMessage> { message ->
            // type of message is UserDataChangedMessage
            println("Received a message: $message")
        }

        instance.addListener<SyncPlayCommandMessage> { message ->
            // type of message is UserDataChangedMessage
            println("Received a message: $message")
        }

        instance.addListener<SessionsMessage> { message ->
            // type of message is UserDataChangedMessage
            println("Received a message: $message")
        }

        instance.addListener<GeneralCommandMessage> { message ->
            // type of message is UserDataChangedMessage
            println("Received a message: $message")
        }

        instance.addListener<SyncPlayGroupUpdateMessage> {

                message ->
            println("msg" + message)
            if (message.update.type == GroupUpdateType.PLAY_QUEUE) {
                Groupmessage = message.update.data
                print(Groupmessage)
                var element = Groupmessage.jsonObject
                var startTime = element.get("StartPositionTicks").toString().toLong() / 10000
                var playList = element.get("Playlist")!!
                var ItemIdsArray = playList.jsonArray.get(0)
                mediaIDString = ItemIdsArray.jsonObject.get("ItemId").toString()
                mediaIDString = mediaIDString.replace("\"", "")
                var r = Groupmessage.toString()
                var mediaInfo: MediaInfo? = null
                print(r + ItemIdsArray + mediaIDString)
                val regex = Regex("""([0-z]{8})([0-z]{4})([0-z]{4})([0-z]{4})([0-z]{12})""")
                mediaId = regex.replace(mediaIDString) { match ->
                    "${match.groups[1]?.value}-${match.groups[2]?.value}-${match.groups[3]?.value}-${match.groups[4]?.value}-${match.groups[5]?.value}"
                }
                ItemId = java.util.UUID.fromString(mediaId)
                startPositionTicks = startTime.toInt()
                hasItemId = true

            }


            instance.addSyncPlayCommandsListener { message ->


                println("Received a message: $message")
            }

        }

        while(ItemId==null){

        }
        var mediaItem = SyncPlayMedia(ItemId!!, startPositionTicks.toLong(), true)
        return mediaItem


    }


    private fun createSpinnerAdapter(): ArrayAdapter<String> {

        // Create an ArrayAdapter with the desired items and layout


        groups.clear()
        groups.add("Join a syncplay group")
        groups.add("Leave Groups")

        CoroutineScope(Dispatchers.IO).launch {

            var syncPlayGetGroups = test!!.api.syncPlayApi.syncPlayGetGroups()
            print(syncPlayGetGroups)

            for (groupInfoDto in syncPlayGetGroups.content) {
                groups.add(groupInfoDto.groupName)
                groupIds.add(groupInfoDto.groupId)
            }

        }




        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            groups
        )

        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        // Add an empty item at the beginning

        return adapter
    }

    override fun onStart() {
        super.onStart()

        requireActivity().window.let {
            originalSoftInputMode = it.attributes?.softInputMode
            it.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        }
    }

    override fun onResume() {
        super.onResume()

        viewModel.loadData()
    }

    override fun onStop() {
        super.onStop()

        originalSoftInputMode?.let { activity?.window?.setSoftInputMode(it) }
    }

    private fun setupView() {


        binding.refreshLayout.setOnRefreshListener {
            viewModel.loadData()
        }

        binding.viewsRecyclerView.adapter = ViewListAdapter(
            onClickListener = { navigateToLibraryFragment(it) },
            onItemClickListener = {
                navigateToMediaItem(it)
            },
            onOnlineClickListener = {
                appPreferences.offlineMode = false
                activity?.restart()
            },
        )

        binding.errorLayout.errorRetryButton.setOnClickListener {
            viewModel.loadData()
        }

        binding.errorLayout.errorDetailsButton.setOnClickListener {
            errorDialog.show(parentFragmentManager, ErrorDialogFragment.TAG)
        }
    }

    private fun bindState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    Timber.d("$uiState")
                    when (uiState) {
                        is HomeViewModel.UiState.Normal -> bindUiStateNormal(uiState)
                        is HomeViewModel.UiState.Loading -> bindUiStateLoading()
                        is HomeViewModel.UiState.Error -> bindUiStateError(uiState)
                    }
                }
            }
        }
    }

    private fun bindUiStateNormal(uiState: HomeViewModel.UiState.Normal) {
        uiState.apply {
            val adapter = binding.viewsRecyclerView.adapter as ViewListAdapter
            adapter.submitList(uiState.homeItems)
        }
        binding.loadingIndicator.isVisible = false
        binding.refreshLayout.isRefreshing = false
        binding.viewsRecyclerView.isVisible = true
    }

    private fun bindUiStateLoading() {
        binding.loadingIndicator.isVisible = true
        binding.errorLayout.errorPanel.isVisible = false
    }

    private fun bindUiStateError(uiState: HomeViewModel.UiState.Error) {
        errorDialog = ErrorDialogFragment.newInstance(uiState.error)
        binding.loadingIndicator.isVisible = false
        binding.refreshLayout.isRefreshing = false
        binding.viewsRecyclerView.isVisible = false
        binding.errorLayout.errorPanel.isVisible = true
        checkIfLoginRequired(uiState.error.message)
    }

    private fun navigateToLibraryFragment(view: dev.jdtech.jellyfin.models.View) {
        findNavController().navigate(
            HomeFragmentDirections.actionNavigationHomeToLibraryFragment(
                libraryId = view.id,
                libraryName = view.name,
                libraryType = view.type,
            ),
        )
    }

    private fun navigateToMediaItem(item: FindroidItem) {
        when (item) {
            is FindroidMovie -> {
                findNavController().navigate(
                    HomeFragmentDirections.actionNavigationHomeToMovieFragment(
                        item.id,
                        item.name,
                    ),
                )
            }

            is FindroidShow -> {
                findNavController().navigate(
                    HomeFragmentDirections.actionNavigationHomeToShowFragment(
                        item.id,
                        item.name,
                    ),
                )
            }

            is FindroidEpisode -> {
                findNavController().navigate(
                    HomeFragmentDirections.actionNavigationHomeToEpisodeBottomSheetFragment(
                        item.id,
                    ),
                )
            }
        }
    }

    private fun navigateToSettingsFragment() {
        findNavController().navigate(
            HomeFragmentDirections.actionHomeFragmentToSettingsFragment(),
        )
    }

    private fun navigateToSearchResultFragment(query: String) {
        findNavController().navigate(
            HomeFragmentDirections.actionHomeFragmentToSearchResultFragment(query),
        )
    }
}
