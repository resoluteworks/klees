package klees

import io.kotest.core.spec.style.StringSpec

class AllowOnlyPoliciesTest : StringSpec({

    "basic policy" {
        data class Principal(val id: String = uuid())
        data class Document(val createdBy: String)

        val authorizer = authorizationPolicy<Principal> {
            resourcePolicy<Document> {
                allow("read") { true }
                allow("write") { resource.createdBy == principal.id }
            }
        }

        val creatorId = uuid()
        val document = Document(creatorId)
        val creator = Principal(creatorId)
        val someOtherUser = Principal()

        authorizer.shouldAllow(creator, "read", document)
        authorizer.shouldAllow(creator, "write", document)
        authorizer.shouldAllow(someOtherUser, "read", document)
        authorizer.shouldNotAllow(someOtherUser, "write", document)
    }

    "policy with multiple resources" {
        data class Principal(val id: String = uuid())
        data class Document(val createdBy: String)
        data class Folder(val createdBy: String)

        val authorizer = authorizationPolicy<Principal> {
            resourcePolicy<Document> {
                // Only owning user can read/write a document
                allow("read", "write") { resource.createdBy == principal.id }
            }

            resourcePolicy<Folder> {
                // Only owning user can add a document
                allow("add-document") { resource.createdBy == principal.id }
            }
        }

        val creatorId = uuid()
        val document = Document(creatorId)
        val folder = Folder(creatorId)
        val creator = Principal(creatorId)
        val someOtherUser = Principal()

        authorizer.shouldAllow(creator, "read", document)
        authorizer.shouldAllow(creator, "write", document)
        authorizer.shouldNotAllow(someOtherUser, "read", document)
        authorizer.shouldNotAllow(someOtherUser, "write", document)

        authorizer.shouldAllow(creator, "add-document", folder)
        authorizer.shouldNotAllow(someOtherUser, "add-document", document)
    }

    "allowAll" {
        data class Principal(val id: String, val role: String)
        data class Document(val createdBy: String)

        val authorizer = authorizationPolicy<Principal> {
            resourcePolicy<Document> {
                allow("read", "write") { resource.createdBy == principal.id }
                allowAll { principal.role == "admin" }
            }
        }

        val document = Document(createdBy = "creatorId")

        val creator = Principal("creatorId", "user")
        authorizer.shouldAllow(creator, "read", document)
        authorizer.shouldAllow(creator, "write", document)
        authorizer.shouldNotAllow(creator, "delete", document)

        val admin = Principal("adminId", "admin")
        authorizer.shouldAllow(admin, "read", document)
        authorizer.shouldAllow(admin, "write", document)
        authorizer.shouldAllow(admin, "delete", document)
    }

    "expenses" {
        data class Principal(
            val id: String,
            val role: UserRole,
            val department: String,
            val directReports: Set<String> = emptySet()
        )

        data class Expense(
            val submittedBy: String,
            val sum: Long,
            val status: ExpenseStatus = ExpenseStatus.PENDING
        )

        val authorizer = authorizationPolicy<Principal> {
            resourcePolicy<Expense> {

                // The only ones that can read are
                // - the user that created the Expense
                // - any user in Finance department
                // - any MANAGER to which the user reports to
                allow("read") {
                    resource.submittedBy == principal.id
                            || principal.department == "Finance"
                            || (principal.role == UserRole.MANAGER && resource.submittedBy in principal.directReports)
                }

                // Only the user themselves can update an expense IF it's still PENDING
                allow("update") {
                    principal.id == resource.submittedBy && resource.status == ExpenseStatus.PENDING
                }

                // Anyone in Finance can approve if the sum is <= 100, otherwise they have to be a MANAGER in Finance
                allow("approve") {
                    principal.department == "Finance" && (resource.sum <= 100 || principal.role == UserRole.MANAGER)
                }

                // Admin can do anything
                allowAll { principal.role == UserRole.ADMIN }
            }
        }

        val submittedBy = uuid()
        val expense = Expense(submittedBy, 80)
        val largeExpense = Expense(submittedBy, 120)
        val submitter = Principal(submittedBy, UserRole.USER, "IT")
        val managerOfSubmitter = Principal(uuid(), UserRole.MANAGER, "IT", setOf(submittedBy))
        val someOtherManager = Principal(uuid(), UserRole.MANAGER, "HR", setOf(uuid()))
        val someOtherUser = Principal(uuid(), UserRole.USER, "Ops", setOf(uuid()))
        val userInFinance = Principal(uuid(), UserRole.USER, "Finance")
        val managerInFinance = Principal(uuid(), UserRole.MANAGER, "Finance")

        authorizer.shouldAllow(submitter, "read", expense)
        authorizer.shouldAllow(userInFinance, "read", expense)
        authorizer.shouldAllow(managerInFinance, "read", expense)
        authorizer.shouldAllow(managerOfSubmitter, "read", expense)
        authorizer.shouldNotAllow(someOtherManager, "read", expense)
        authorizer.shouldNotAllow(someOtherUser, "read", expense)

        authorizer.shouldAllow(submitter, "update", expense)
        authorizer.shouldNotAllow(submitter, "update", Expense(submittedBy, 50, ExpenseStatus.APPROVED))
        authorizer.shouldNotAllow(userInFinance, "update", expense)
        authorizer.shouldNotAllow(managerInFinance, "update", expense)
        authorizer.shouldNotAllow(managerOfSubmitter, "update", expense)
        authorizer.shouldNotAllow(someOtherManager, "update", expense)

        // Anyone in Finance can approve if the sum is <= 100, otherwise they have to be a MANAGER in Finance
        authorizer.shouldAllow(userInFinance, "approve", expense)
        authorizer.shouldAllow(managerInFinance, "approve", expense)
        authorizer.shouldNotAllow(userInFinance, "approve", largeExpense)
        authorizer.shouldAllow(managerInFinance, "approve", largeExpense)
        authorizer.shouldNotAllow(submitter, "approve", expense)
        authorizer.shouldNotAllow(managerOfSubmitter, "approve", expense)
        authorizer.shouldNotAllow(someOtherManager, "approve", expense)
        authorizer.shouldNotAllow(someOtherUser, "approve", expense)
        authorizer.shouldNotAllow(someOtherUser, "approve", expense)

        val admin = Principal(uuid(), UserRole.ADMIN, "Doesn't matter")
        authorizer.shouldAllow(admin, "read", expense)
        authorizer.shouldAllow(admin, "update", expense)
        authorizer.shouldAllow(admin, "approve", expense)
        authorizer.shouldAllow(admin, "approve", largeExpense)
    }
}) {

    private enum class UserRole { USER, MANAGER, ADMIN }
    private enum class ExpenseStatus { PENDING, APPROVED }
}
