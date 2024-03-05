package klees

import io.kotest.core.spec.style.StringSpec

class OrdersTest : StringSpec({

    "online order management" {
        data class Order(
            val status: OrderStatus,
            var customerId: String,
            var handlingEmployee: String? = null,
            var courier: String? = null,
        )

        data class Principal(
            val id: String,
            val role: UserRole,
        )

        val checkoutOrder = "checkoutOrder"
        val cancelOrder = "cancelOrder"
        val dispatchOrder = "dispatchOrder"
        val markOrderOutForDelivery = "markOrderOutForDelivery"
        val markOrderDelivered = "markOrderDelivered"

        val orderCustomer = "orderCustomer"
        val orderHandler = "orderHandler"
        val orderCourier = "orderCourier"

        val authorizer = authorizationPolicy<Principal> {
            derivedRoles<Order> {
                orderCustomer {
                    resource.customerId == principal.id
                }

                orderHandler {
                    resource.handlingEmployee == principal.id
                }

                orderCourier {
                    resource.courier == principal.id
                }
            }

            resourcePolicy<Order> {

                // Only a customer can check out an order and only if it's in a DRAFT state
                allow(checkoutOrder) {
                    principal.role == UserRole.CUSTOMER && resource.status == OrderStatus.DRAFT
                }

                // Only the customer and the handling employee can cancel an order
                allow(cancelOrder) {
                    hasAnyDerivedRole(orderCustomer, orderHandler)
                }

                // Orders can't be cancelled when dispatched, out for delivery, delivered
                deny(cancelOrder) {
                    resource.status in setOf(
                        OrderStatus.DISPATCHED,
                        OrderStatus.OUT_FOR_DELIVERY,
                        OrderStatus.DELIVERED
                    )
                }

                // Only the employee handling the order can dispatch it, and not if it has payment issues
                allow(dispatchOrder) {
                    hasDerivedRole(orderHandler) && resource.status != OrderStatus.PAYMENT_ISSUE
                }

                // Only the courier for this order can mark order as out for delivery or delivered
                allow(markOrderOutForDelivery, markOrderDelivered) {
                    principal.id == resource.courier
                }

                // No one can do anything with an order once it's delivered
                denyAll {
                    resource.status == OrderStatus.DELIVERED
                }
            }
        }

        val customerId = uuid()
        val customer = Principal(customerId, UserRole.CUSTOMER)
        val handlingEmployee = Principal(uuid(), UserRole.EMPLOYEE)
        val otherEmployee = Principal(uuid(), UserRole.EMPLOYEE)
        val courier = Principal(uuid(), UserRole.COURIER)
        val otherCourier = Principal(uuid(), UserRole.COURIER)

        fun AuthorizationPolicy<Principal>.shouldNotAllowStatuses(principal: Principal, action: String, vararg orderStatuses: OrderStatus) {
            orderStatuses.forEach {
                this.shouldNotAllow(principal, action, Order(it, customerId, handlingEmployee.id, courier.id))
            }
        }

        fun AuthorizationPolicy<Principal>.shouldNotAllowAnyStatus(principal: Principal, action: String) {
            OrderStatus.entries.forEach {
                this.shouldNotAllow(principal, action, Order(it, customerId, handlingEmployee.id, courier.id))
            }
        }

        // Only a customer can check out an order and only if it's in a DRAFT state
        authorizer.shouldAllow(customer, checkoutOrder, Order(OrderStatus.DRAFT, customerId))
        authorizer.shouldNotAllow(customer, checkoutOrder, Order(OrderStatus.CHECKED_OUT, customerId))
        authorizer.shouldNotAllow(customer, checkoutOrder, Order(OrderStatus.PROCESSING, customerId))
        authorizer.shouldNotAllowAnyStatus(handlingEmployee, checkoutOrder)
        authorizer.shouldNotAllowAnyStatus(otherEmployee, checkoutOrder)
        authorizer.shouldNotAllowAnyStatus(courier, checkoutOrder)
        authorizer.shouldNotAllowAnyStatus(otherCourier, checkoutOrder)

        // Only the customer and the handling employee can cancel an order
        authorizer.shouldAllow(customer, cancelOrder, Order(OrderStatus.CHECKED_OUT, customerId))
        authorizer.shouldAllow(handlingEmployee, cancelOrder, Order(OrderStatus.CHECKED_OUT, customerId, handlingEmployee.id))
        authorizer.shouldNotAllowAnyStatus(otherEmployee, cancelOrder)
        authorizer.shouldNotAllowAnyStatus(courier, cancelOrder)
        authorizer.shouldNotAllowAnyStatus(otherCourier, cancelOrder)

        // Orders can't be cancelled when dispatched, out for delivery, delivered
        authorizer.shouldNotAllowStatuses(customer, cancelOrder, OrderStatus.DISPATCHED, OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED)
        authorizer.shouldNotAllowStatuses(handlingEmployee, cancelOrder, OrderStatus.DISPATCHED, OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED)
        authorizer.shouldNotAllowStatuses(otherEmployee, cancelOrder, OrderStatus.DISPATCHED, OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED)
        authorizer.shouldNotAllowStatuses(courier, cancelOrder, OrderStatus.DISPATCHED, OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED)
        authorizer.shouldNotAllowStatuses(otherCourier, cancelOrder, OrderStatus.DISPATCHED, OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED)

        // Only the employee handling the order can dispatch it, and not if it has payment issues
        authorizer.shouldAllow(handlingEmployee, dispatchOrder, Order(OrderStatus.PROCESSING, customerId, handlingEmployee.id))
        authorizer.shouldNotAllow(otherEmployee, dispatchOrder, Order(OrderStatus.PROCESSING, customerId, handlingEmployee.id))
        authorizer.shouldNotAllow(handlingEmployee, dispatchOrder, Order(OrderStatus.PAYMENT_ISSUE, customerId, handlingEmployee.id))

        // Only the courier for this order can mark order as out for delivery or delivered
        val fullOrder = Order(OrderStatus.DISPATCHED, customerId, handlingEmployee.id, courier.id)
        authorizer.shouldAllow(courier, markOrderOutForDelivery, fullOrder)
        authorizer.shouldAllow(courier, markOrderDelivered, fullOrder)
        authorizer.shouldNotAllow(otherCourier, markOrderOutForDelivery, fullOrder)
        authorizer.shouldNotAllow(otherCourier, markOrderDelivered, fullOrder)
        authorizer.shouldNotAllow(handlingEmployee, markOrderDelivered, fullOrder)

        fun AuthorizationPolicy<Principal>.shouldNotAllowAnyActionWhenDelivered(principal: Principal) {
            this.shouldNotAllow(principal, checkoutOrder, Order(OrderStatus.DELIVERED, customerId, handlingEmployee.id, courier.id))
            this.shouldNotAllow(principal, cancelOrder, Order(OrderStatus.DELIVERED, customerId, handlingEmployee.id, courier.id))
            this.shouldNotAllow(principal, dispatchOrder, Order(OrderStatus.DELIVERED, customerId, handlingEmployee.id, courier.id))
            this.shouldNotAllow(principal, markOrderOutForDelivery, Order(OrderStatus.DELIVERED, customerId, handlingEmployee.id, courier.id))
            this.shouldNotAllow(principal, markOrderDelivered, Order(OrderStatus.DELIVERED, customerId, handlingEmployee.id, courier.id))
        }

        // No one can do anything with an order once it's delivered
        authorizer.shouldNotAllowAnyActionWhenDelivered(customer)
        authorizer.shouldNotAllowAnyActionWhenDelivered(handlingEmployee)
        authorizer.shouldNotAllowAnyActionWhenDelivered(otherEmployee)
        authorizer.shouldNotAllowAnyActionWhenDelivered(courier)
        authorizer.shouldNotAllowAnyActionWhenDelivered(otherCourier)
    }
}) {

    private enum class UserRole {
        CUSTOMER,
        EMPLOYEE,
        COURIER
    }

    private enum class OrderStatus {
        DRAFT, // The customer is still choosing items
        CHECKED_OUT, // The customer sent the order to the company (finished buying online)
        CANCELLED, // The customer or the company cancelled the order (customer changed their mind, or company doesn't have it in stock)
        PAYMENT_ISSUE, // The company found problems with the order payment (card rejected, etc.)
        PROCESSING, // The company is reviewing the order, confirming stock, packaging, etc
        DISPATCHED, // The order is dispatched
        OUT_FOR_DELIVERY, // The order is out for delivery to the customer
        DELIVERED, // The courier had to re-arrange delivery to customer
    }
}
