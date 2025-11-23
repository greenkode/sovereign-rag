package ai.sovereignrag.identity.core.invitation.query

import ai.sovereignrag.identity.core.invitation.dto.UserListItem
import an.awesome.pipelinr.Command

class GetUsersQuery(
) : Command<GetUsersResult>

data class GetUsersResult(
    val users: List<UserListItem>
)