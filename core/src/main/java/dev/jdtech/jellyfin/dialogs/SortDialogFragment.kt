package dev.jdtech.jellyfin.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.jdtech.jellyfin.AppPreferences
import dev.jdtech.jellyfin.core.R
import dev.jdtech.jellyfin.models.SortBy
import dev.jdtech.jellyfin.viewmodels.LibraryViewModel
import java.lang.IllegalStateException
import java.util.UUID
import org.jellyfin.sdk.model.api.SortOrder

class SortDialogFragment(
    private val parentId: UUID,
    private val libraryType: String?,
    private val viewModel: LibraryViewModel,
    private val sortType: String
) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val preferences = AppPreferences(PreferenceManager.getDefaultSharedPreferences(it.applicationContext))
            val builder = MaterialAlertDialogBuilder(it)

            // Current sort by
            val currentSortByString = preferences.sortBy
            val currentSortBy = SortBy.fromString(currentSortByString)

            // Current sort order
            val currentSortOrderString = preferences.sortOrder
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
                            sortByOptions, currentSortBy.ordinal
                        ) { dialog, which ->
                            val sortBy = sortByValues[which]
                            preferences.sortBy = sortBy.name
                            viewModel.loadItems(
                                parentId,
                                libraryType,
                                sortBy = sortBy,
                                sortOrder = currentSortOrder
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
                            sortByOptions, currentSortOrder.ordinal
                        ) { dialog, which ->
                            val sortOrder = try {
                                sortOrderValues[which]
                            } catch (e: IllegalArgumentException) {
                                SortOrder.ASCENDING
                            }

                            preferences.sortOrder = sortOrder.name

                            viewModel.loadItems(
                                parentId,
                                libraryType,
                                sortBy = currentSortBy,
                                sortOrder = sortOrder
                            )
                            dialog.dismiss()
                        }
                }
            }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
