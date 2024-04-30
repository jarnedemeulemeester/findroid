package dev.jdtech.jellyfin.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.AppPreferences
import dev.jdtech.jellyfin.core.R
import dev.jdtech.jellyfin.models.CollectionType
import dev.jdtech.jellyfin.models.SortBy
import dev.jdtech.jellyfin.viewmodels.LibraryViewModel
import org.jellyfin.sdk.model.api.SortOrder
import java.lang.IllegalStateException
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class SortDialogFragment(
    private val parentId: UUID,
    private val libraryType: CollectionType,
    private val viewModel: LibraryViewModel,
    private val sortType: String,
) : DialogFragment() {
    @Inject
    lateinit var appPreferences: AppPreferences

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = MaterialAlertDialogBuilder(it)

            // Current sort by
            val currentSortByString = appPreferences.sortBy
            val currentSortBy = SortBy.fromString(currentSortByString)

            // Current sort order
            val currentSortOrderString = appPreferences.sortOrder
            val currentSortOrder = try {
                SortOrder.valueOf(currentSortOrderString)
            } catch (e: IllegalArgumentException) {
                SortOrder.ASCENDING
            }

            when (sortType) {
                "sortBy" -> {
                    val sortByOptions = resources.getStringArray(R.array.sort_by_options)
                    val sortByValues = SortBy.values()
                    builder
                        .setTitle(getString(R.string.sort_by))
                        .setSingleChoiceItems(
                            sortByOptions,
                            currentSortBy.ordinal,
                        ) { dialog, which ->
                            val sortBy = sortByValues[which]
                            appPreferences.sortBy = sortBy.name
                            viewModel.loadItems(
                                parentId,
                                libraryType,
                                sortBy = sortBy,
                                sortOrder = currentSortOrder,
                            )
                            dialog.dismiss()
                        }
                }
                "sortOrder" -> {
                    val sortByOptions = resources.getStringArray(R.array.sort_order_options)
                    val sortOrderValues = SortOrder.values()

                    builder
                        .setTitle(getString(R.string.sort_order))
                        .setSingleChoiceItems(
                            sortByOptions,
                            currentSortOrder.ordinal,
                        ) { dialog, which ->
                            val sortOrder = try {
                                sortOrderValues[which]
                            } catch (e: IllegalArgumentException) {
                                SortOrder.ASCENDING
                            }

                            appPreferences.sortOrder = sortOrder.name

                            viewModel.loadItems(
                                parentId,
                                libraryType,
                                sortBy = currentSortBy,
                                sortOrder = sortOrder,
                            )
                            dialog.dismiss()
                        }
                }
            }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
