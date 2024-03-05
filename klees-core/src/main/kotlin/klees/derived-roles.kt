package klees

class ResourceDerivedRoleContext<Principal : Any, Resource : Any>(
    val principal: Principal,
    val resource: Resource,
) {
    val p: Principal = principal
    val r: Resource = resource
}

typealias ResourceDerivedRoleCondition<Principal, Resource> = ResourceDerivedRoleContext<Principal, Resource>.() -> Boolean

class ResourceDerivedRolesPolicy<Principal : Any, Resource : Any> {
    private val rules = mutableListOf<Rule<Principal, Resource>>()

    operator fun String.invoke(condition: ResourceDerivedRoleCondition<Principal, Resource>) {
        rules.add(Rule(this, condition))
    }

    fun getDerivedRoles(principal: Principal, resource: Resource): Set<String> {
        val context = ResourceDerivedRoleContext(principal, resource)
        return rules.filter { it.condition(context) }.map { it.role }.toSet()
    }

    class Rule<Principal : Any, Resource : Any>(
        val role: String,
        val condition: ResourceDerivedRoleCondition<Principal, Resource>
    )
}
