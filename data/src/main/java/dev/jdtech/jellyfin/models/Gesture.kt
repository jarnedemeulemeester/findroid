package dev.jdtech.jellyfin.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "gestures",
    foreignKeys = [
        ForeignKey(
            entity = GestureProfile::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("profileId"),
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class Gesture(
    @PrimaryKey
    val id: UUID,
    @ColumnInfo(index = true)
    val profileId: UUID,
    val tapType: GestureTapType,
    val tapLocation: GestureTapLocation,
    val action: GestureAction,
)

enum class GestureTapType(val raw: String) {
// TODO: TBD
//    Single("Single Tap"),
//    Double("Double Tap"),
//    Triple("Triple Tap"),
//    Long("Long Press"),
}

enum class GestureTapLocation(val raw: String) {
// TODO: TBD
//    Right("Right"),
//    Middle("Middle"),
//    Left("Left"),
}

enum class GestureAction(val raw: String) {
// TODO: TBD
//    Play("Play"),
//    Pause("Pause"),
//    Next("Next"),
//    Previous("Previous"),
//    Stop("Stop"),
//    VolumeUp("Volume Up"),
//    VolumeDown("Volume Down"),
//    Mute("Mute"),
//    SeekForward("Seek Forward"),
//    SeekBackward("Seek Backward"),
}