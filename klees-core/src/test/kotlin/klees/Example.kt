package klees

import io.kotest.core.spec.style.StringSpec

class Example : StringSpec({

    "example" {
        data class Principal(val id: String, val role: String)
        data class Document(val ownerId: String, val locked: Boolean = false)

        val read = "read"
        val write = "write"

        // An authorization policy starts by specifying the principal type
        val authPolicy = authorizationPolicy<Principal> {

            // Derived roles are dynamically evaluated based on the principal and the resource being accessed
            derivedRoles<Document> {
                "owner" { resource.ownerId == principal.id }
            }

            // A resource policy describes principal permissions for actions against a resource type
            // Resource policies are built using allow(), allowAll(), deny() and denyAll() calls.
            // allow() and deny() take a vararg list of action names and a block defining the permission logic (returns Boolean)
            // allowAll / denyAll are used to blanket enable/disable all actions based on the described logic
            resourcePolicy<Document> {
                // Using the derived role defined above
                allow(write) { hasDerivedRole("owner") }

                // Use principal and resource fields to define the logic for the permission
                allowAll { principal.role == "ADMIN" && !resource.locked }

                // Use the p and r shorthand versions for lengthier expressions
                allow("archive") { (p.role == "ADMIN" || p.role == "MANAGER") && !r.locked }

                // Allow any principal to perform this action
                allow(read) { true }

                // Deny all actions when a document is locked. Deny always takes priority over a previously inferred allow.
                denyAll { resource.locked }
            }
        }

        val principal = Principal(uuid(), "USER")
        val document = Document(uuid())

        // Use check() to throw an exception when the operation is not permitted
        authPolicy.check { principal can read(document) }

        // Use allowed() to get back a boolean instead of throwing an exception
        val allowed = authPolicy.allowed(principal, read, document)
    }
})
