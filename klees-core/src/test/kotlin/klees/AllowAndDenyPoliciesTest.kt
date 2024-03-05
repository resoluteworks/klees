package klees

import io.kotest.core.spec.style.StringSpec

class AllowAndDenyPoliciesTest : StringSpec({

    "deny based on resource state" {
        data class Principal(val id: String = uuid())
        data class Document(val createdBy: String, val status: DocumentStatus)

        val authorizer = authorizationPolicy<Principal> {
            resourcePolicy<Document> {
                allow("read") { true }

                // Only the owner can delete
                allow("delete") { resource.createdBy == principal.id }

                // No one can delete a published document
                deny("delete") { resource.status == DocumentStatus.PUBLISHED }
            }
        }

        val creatorId = uuid()
        val creator = Principal(creatorId)
        val someOtherUser = Principal()

        authorizer.shouldAllow(creator, "read", Document(creatorId, DocumentStatus.DRAFT))
        authorizer.shouldAllow(someOtherUser, "read", Document(creatorId, DocumentStatus.DRAFT))

        authorizer.shouldAllow(creator, "delete", Document(creatorId, DocumentStatus.DRAFT))
        authorizer.shouldNotAllow(someOtherUser, "delete", Document(creatorId, DocumentStatus.DRAFT))

        // Deny takes precedence, even when the owner is the creator
        authorizer.shouldNotAllow(creator, "delete", Document(creatorId, DocumentStatus.PUBLISHED))
        authorizer.shouldNotAllow(someOtherUser, "delete", Document(creatorId, DocumentStatus.PUBLISHED))
    }

    "holidays" {
        data class Principal(
            val id: String = uuid(),
            val role: UserRole,
            val directReports: Set<String> = emptySet(),
        )

        data class HolidayRequest(
            val requestedBy: String,
            val days: Int
        )

        val authorizer = authorizationPolicy<Principal> {
            resourcePolicy<HolidayRequest> {

                // Only the owning user and their manager can read a holiday request
                allow("read") {
                    (resource.requestedBy == principal.id) || (resource.requestedBy in principal.directReports)
                }

                // Only their manager can approve a holiday request
                allow("approve") {
                    resource.requestedBy in principal.directReports
                }

                // No one can approve holidays that are longer than 15 days
                deny("approve") {
                    resource.days > 15
                }
            }
        }

        val requestedBy = uuid()
        val request = HolidayRequest(requestedBy, 10)
        val requester = Principal(requestedBy, UserRole.USER)
        val managerOfRequester = Principal(uuid(), UserRole.MANAGER, setOf(requestedBy))
        val someOtherManager = Principal(uuid(), UserRole.MANAGER, setOf(uuid()))
        val someOtherUser = Principal(uuid(), UserRole.USER)

        authorizer.shouldAllow(requester, "read", request)
        authorizer.shouldAllow(managerOfRequester, "read", request)
        authorizer.shouldNotAllow(someOtherUser, "read", request)
        authorizer.shouldNotAllow(someOtherManager, "read", request)

        authorizer.shouldAllow(managerOfRequester, "approve", request)
        authorizer.shouldNotAllow(requester, "approve", request)
        authorizer.shouldNotAllow(someOtherUser, "approve", request)
        authorizer.shouldNotAllow(someOtherManager, "approve", request)

        authorizer.shouldNotAllow(requester, "approve", HolidayRequest(requestedBy, 20))
        authorizer.shouldNotAllow(managerOfRequester, "approve", HolidayRequest(requestedBy, 16))
        authorizer.shouldNotAllow(someOtherUser, "approve", HolidayRequest(requestedBy, 25))
        authorizer.shouldNotAllow(someOtherManager, "approve", HolidayRequest(requestedBy, 20))
    }

}) {
    private enum class DocumentStatus { DRAFT, PUBLISHED }
    private enum class UserRole { USER, MANAGER }
}