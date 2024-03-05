package klees

import io.kotest.core.spec.style.StringSpec

class DerivedRolesTest : StringSpec({

    "document owner" {
        data class Principal(val id: String = uuid())
        data class Document(
            val createdBy: String,
            val collaborators: Set<String> = emptySet()
        )

        val authorizer = authorizationPolicy<Principal> {
            derivedRoles<Document> {
                "owner" {
                    principal.id == resource.createdBy
                }
                "collaborator" {
                    principal.id in resource.collaborators
                }
            }

            resourcePolicy<Document> {

                // Anyone can read
                allow("read") { true }

                // Only owner and collaborators can write
                allow("write") { hasDerivedRole("owner") || hasDerivedRole("collaborator") }

                // Only the owner can archive it
                allow("archive") { "owner" in derivedRoles }
            }
        }

        val ownerId = uuid()
        val collaboratorId = uuid()
        val owner = Principal(ownerId)
        val collaborator = Principal(collaboratorId)
        val document = Document(ownerId, setOf(collaboratorId))
        val someOtherUser = Principal()

        authorizer.shouldAllow(owner, "read", document)
        authorizer.shouldAllow(collaborator, "read", document)
        authorizer.shouldAllow(someOtherUser, "read", document)

        authorizer.shouldAllow(owner, "write", document)
        authorizer.shouldAllow(collaborator, "write", document)
        authorizer.shouldNotAllow(someOtherUser, "write", document)

        authorizer.shouldAllow(owner, "archive", document)
        authorizer.shouldNotAllow(collaborator, "archive", document)
        authorizer.shouldNotAllow(someOtherUser, "archive", document)
    }
})
