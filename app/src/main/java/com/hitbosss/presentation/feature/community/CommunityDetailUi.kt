package com.hitbosss.presentation.feature.community

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.hitbosss.domain.model.Member
import com.hitbosss.presentation.designsystem.theme.Gray200
import com.hitbosss.presentation.designsystem.theme.Gray400
import com.hitbosss.presentation.designsystem.theme.Gray500
import com.hitbosss.presentation.designsystem.theme.Gray800
import com.hitbosss.presentation.designsystem.theme.HitbosssType
import com.hitbosss.presentation.designsystem.theme.Primary100
import com.hitbosss.presentation.designsystem.theme.Primary700

@Composable
fun DetailCover(url: String?) {
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxWidth().height(170.dp).background(Gray400),
    )
}

@Composable
fun ExerciseChips(exercises: List<String>) {
    if (exercises.isEmpty()) return
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        exercises.take(6).forEach { ex ->
            Text(
                ex.replaceFirstChar { it.uppercase() },
                style = HitbosssType.bodySmallEmphasis,
                color = Primary700,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Primary100)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
fun MembersSection(members: List<Member>) {
    Text("Miembros (${members.size})", style = HitbosssType.titleBody, color = Gray800)
    members.forEach { member ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AsyncImage(
                model = member.profilePic,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(40.dp).clip(CircleShape).background(Gray200),
            )
            Text(member.username, style = HitbosssType.bodyLargeRegular, color = Gray800, modifier = Modifier.weight(1f))
            if (member.isAdmin) {
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Primary100).padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text("Admin", style = HitbosssType.bodySmallEmphasis, color = Primary700)
                }
            }
        }
    }
    if (members.isEmpty()) {
        Text("Sin miembros todavía.", style = HitbosssType.bodySmallRegular, color = Gray500)
    }
}
