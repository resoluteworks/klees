package klees

import kotlin.reflect.KClass

class PermissionDeniedException(message: String) : RuntimeException(message)

fun <Principal : Any> authorizationPolicy(init: AuthorizationPolicy<Principal>.() -> Unit): AuthorizationPolicy<Principal> {
    val authorizer = AuthorizationPolicy<Principal>()
    init(authorizer)
    return authorizer
}

class AuthorizationPolicy<Principal : Any> {

    private val resourcePolicies = mutableMapOf<KClass<*>, ResourcePolicy<Principal, *>>()
    private val resourceDerivedRolesPolicies = mutableMapOf<KClass<*>, ResourceDerivedRolesPolicy<Principal, *>>()

    inline fun <reified Resource : Any> derivedRoles(init: ResourceDerivedRolesPolicy<Principal, Resource>.() -> Unit) {
        val derivedRole = ResourceDerivedRolesPolicy<Principal, Resource>()
        init(derivedRole)
        addDerivedRole(Resource::class, derivedRole)
    }

    inline fun <reified Resource : Any> resourcePolicy(init: ResourcePolicy<Principal, Resource>.() -> Unit) {
        val policy = ResourcePolicy<Principal, Resource>()
        init(policy)
        addPolicy(Resource::class, policy)
    }

    fun <Resource : Any> addPolicy(cls: KClass<Resource>, policy: ResourcePolicy<Principal, Resource>) {
        resourcePolicies[cls] = policy
    }

    fun <Resource : Any> addDerivedRole(cls: KClass<Resource>, derivedRole: ResourceDerivedRolesPolicy<Principal, Resource>) {
        resourceDerivedRolesPolicies[cls] = derivedRole
    }

    /**
     * Returns a boolean indicating whether the principal is allowed the specified action against the resource.
     * Use this method when you wish to perform a permission check and handle a boolean value rather than an exception.
     */
    fun <Resource : Any> allowed(principal: Principal, action: String, resource: Resource): Boolean {
        val resourcePolicy = resourcePolicies[resource::class]
            ?: throw IllegalArgumentException("No policies found for class ${resource::class}")

        val derivedRolesForResource = resourceDerivedRolesPolicies[resource::class] as ResourceDerivedRolesPolicy<Principal, Resource>?
        val derivedRoles = derivedRolesForResource?.getDerivedRoles(principal, resource) ?: emptySet()

        return (resourcePolicy as ResourcePolicy<Principal, Resource>).allowed(principal, resource, action, derivedRoles)
    }

    fun <Resource : Any> allAllowed(principal: Principal, resource: Resource, vararg actions: String): Boolean {
        return actions.all { allowed(principal, it, resource) }
    }

    /**
     * Checks that the action by the principal is allowed against the resource.
     * Throws a [PermissionDeniedException] when the action is not allowed.
     * Use this method when you wish to perform a permission check and throw an exception.
     */
    @Throws(PermissionDeniedException::class)
    fun <Resource : Any> check(principal: Principal, action: String, resource: Resource) {
        if (!allowed(principal, action, resource)) {
            throw PermissionDeniedException("Action not allowed")
        }
    }

    fun <Resource : Any> checkAll(principal: Principal, resource: Resource, vararg actions: String) {
        actions.forEach { check(principal, it, resource) }
    }

    fun check(block: AuthorizationPolicy<Principal>.() -> PermissionCheck<Principal>) {
        check(block(this))
    }

    private fun check(check: PermissionCheck<Principal>) {
        check(
            principal = check.principal,
            action = check.action,
            resource = check.resource
        )
    }

    infix fun Principal.can(resourceAction: ResourceAction): PermissionCheck<Principal> {
        return PermissionCheck(this, resourceAction)
    }

    fun Principal.can(vararg actions: ResourceAction): Collection<PermissionCheck<Principal>> {
        return actions.map { PermissionCheck(this, it) }
    }

    fun Principal.can(action: String, resource: Any): PermissionCheck<Principal> {
        return PermissionCheck(this, action, resource)
    }

    infix operator fun String.invoke(resource: Any): ResourceAction {
        return ResourceAction(resource, this)
    }
}

data class ResourceAction(val resource: Any, val action: String)
data class PermissionCheck<Principal : Any>(
    val principal: Principal,
    val action: String,
    val resource: Any
) {
    constructor(principal: Principal, resourceAction: ResourceAction) : this(
        principal = principal,
        action = resourceAction.action,
        resource = resourceAction.resource
    )
}

