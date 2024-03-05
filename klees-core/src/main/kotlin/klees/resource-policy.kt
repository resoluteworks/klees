package klees

private typealias ActionCondition<Principal, Resource> = ActionContext<Principal, Resource>.() -> Boolean
private typealias ActionMatcher = (String) -> Boolean

class ActionContext<Principal : Any, Resource : Any>(
    val principal: Principal,
    val resource: Resource,
    val derivedRoles: Set<String>
) {
    val p: Principal = principal
    val r: Resource = resource

    fun hasDerivedRole(derivedRole: String): Boolean {
        return derivedRoles.contains(derivedRole)
    }

    fun hasAnyDerivedRole(vararg derivedRoles: String): Boolean {
        return derivedRoles.any { hasDerivedRole(it) }
    }
}

class ResourcePolicy<Principal : Any, Resource : Any> {
    private val rules = mutableListOf<Rule<Principal, Resource>>()

    fun allow(vararg actions: String, condition: ActionCondition<Principal, Resource>) {
        rules.add(Rule({ it in actions }, condition, Rule.Effect.ALLOW))
    }

    fun allowAll(condition: ActionCondition<Principal, Resource>) {
        rules.add(Rule({ true }, condition, Rule.Effect.ALLOW))
    }

    fun denyAll(condition: ActionCondition<Principal, Resource>) {
        rules.add(Rule({ true }, condition, Rule.Effect.DENY))
    }

    fun deny(vararg actions: String, condition: ActionCondition<Principal, Resource>) {
        rules.add(Rule({ it in actions }, condition, Rule.Effect.DENY))
    }

    fun allowed(principal: Principal, resource: Resource, action: String, derivedRoles: Set<String>): Boolean {
        val actionContext = ActionContext(principal, resource, derivedRoles)
        val matchingRules = rules.filter { it.matchesContext(action, actionContext) }

        // If no rules match then this action should be denied
        if (matchingRules.isEmpty()) {
            return false
        }

        // Only return true if none of the rules are of type DENY
        return matchingRules.none { it.effect == Rule.Effect.DENY }
    }

    class Rule<Principal : Any, Resource : Any>(
        val actionMatcher: ActionMatcher,
        val condition: ActionCondition<Principal, Resource>,
        val effect: Effect
    ) {

        fun matchesContext(action: String, context: ActionContext<Principal, Resource>): Boolean {
            return actionMatcher(action) && condition(context)
        }

        enum class Effect {
            ALLOW,
            DENY
        }
    }
}
