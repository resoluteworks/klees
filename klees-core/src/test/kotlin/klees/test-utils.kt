package klees

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.spyk
import io.mockk.verify
import java.util.*

fun <P : Any, R : Any> AuthorizationPolicy<P>.shouldAllow(principal: P, action: String, resource: R) {
    val authorizer = spyk(this)
    authorizer.check { principal can action(resource) }
    authorizer.check { principal.can(action, resource) }
    authorizer.checkAll(principal, resource, action)
    verify(exactly = 3) { authorizer.allowed(principal, action, resource) }

    authorizer.allowed(principal, action, resource) shouldBe true
    authorizer.allAllowed(principal, resource, action) shouldBe true
}

fun <P : Any, R : Any> AuthorizationPolicy<P>.shouldNotAllow(principal: P, action: String, resource: R) {
    val authorizer = spyk(this)

    shouldThrow<PermissionDeniedException> {
        authorizer.check { principal can action(resource) }
    }

    shouldThrow<PermissionDeniedException> {
        authorizer.check { principal.can(action, resource) }
    }

    shouldThrow<PermissionDeniedException> {
        authorizer.checkAll(principal, resource, action)
    }
    verify(exactly = 3) { authorizer.allowed(principal, action, resource) }

    authorizer.allowed(principal, action, resource) shouldBe false
    authorizer.allAllowed(principal, resource, action) shouldBe false
}

fun uuid(): String = UUID.randomUUID().toString()
