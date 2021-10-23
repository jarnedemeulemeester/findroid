package dev.jdtech.jellyfin.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.utils.SortBy
import dev.jdtech.jellyfin.viewmodels.LibraryViewModel
import org.jellyfin.sdk.model.api.SortOrder
import java.lang.IllegalStateException
import java.util.*

class SortDialogFragment(
    private val parentId: UUID,
    private val libraryType: String?,
    private val viewModel: LibraryViewModel,
    private val sortType: String
) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val sp = PreferenceManager.getDefaultSharedPreferences(it.applicationContext)
            val builder = MaterialAlertDialogBuilder(it)

            // Current sort by
            val currentSortByString = sp.getString("sortBy", SortBy.defaultValue.name)!!
            val currentSortBy = SortBy.fromString(currentSortByString)

            // Current sort order
            val currentSortOrderString = sp.getString("sortOrder", SortOrder.ASCENDING.name)!!
            val currentSortOrder = try {
                SortOrder.valueOf(currentSortOrderString)
            } catch (e: java.lang.IllegalArgumentException) {
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
                            sp.edit().putString("sortBy", sortByValues[which].name).apply()
                            viewModel.loadItems(
                                parentId,
                                libraryType,
                                sortBy = sortByValues[which],
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
                            sp.edit().putString("sortOrder", sortOrderValues[which].name).apply()

                            val sortOrder = try {
                                sortOrderValues[which]
                            } catch (e: IllegalArgumentException) {
                                SortOrder.ASCENDING
                            }

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