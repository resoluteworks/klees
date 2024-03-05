# Klees
![GitHub release (latest by date)](https://img.shields.io/github/v/release/resoluteworks/klees)
![Coveralls](https://img.shields.io/coverallsCoverage/github/resoluteworks/klees)

Klees is a Kotlin framework for implementing Attribute-based access control (ABAC) using a typesafe DSL.

## Dependency
```groovy
implementation("io.resoluteworks:klees-core:${kleesVersion}")
```

## Getting started

 See this [example](https://github.com/resoluteworks/klees/blob/main/klees-core/src/test/kotlin/klees/Example.kt) for a version with detailed comments.

```kotlin
data class Principal(val id: String, val role: String)
data class Document(val ownerId: String, val locked: Boolean = false)

val read = "read"
val write = "write"

val authPolicy = authorizationPolicy<Principal> {

    derivedRoles<Document> {
        "owner" { resource.ownerId == principal.id }
    }

    resourcePolicy<Document> {
        
        allow(write) { hasDerivedRole("owner") }
        
        allowAll { principal.role == "ADMIN" && !resource.locked }
        
        allow("archive") { (p.role == "ADMIN" || p.role == "MANAGER") && !r.locked }
        
        allow(read) { true }
        
        denyAll { resource.locked }
    }
}

val principal = Principal(uuid(), "USER")
val document = Document(uuid())

authPolicy.check { principal can read(document) }
val allowed = authPolicy.allowed(principal, read, document)
```

## Guiding principles
The core motivation for Klees to provide the ability to reason about authorization permissions as easily as possible, and using type safety
to define and check authorization logic.

At the same time, we wanted something that's unintrusive, which is why we don't have marker interfaces like
`Principal` or `Resource` and we've opted for the generic arguments instead.

This allows the client code to bring its own representation for these entities, and for the framework to act as a drop-in or extension.

For the same reasons, the core library only provides the basic elements of operating an authorization policy: definition and verification.
The consumer application can then make its own decisions about how these elements are wired (filters, proxies, explicit calls, etc).

## Inspiration
Klees borrows principles and approaches from [Cerbos](https://www.cerbos.dev/), but it doesn't (cannot) stand as an alternative in the same space.
Klees is not an authorization platform, but a mini authorization framework for Kotlin/JVM.
