package com.bugsnag.android

import java.lang.Thread

/**
 * Accesses the session tracker and flushes all stored sessions
 */
internal fun flushAllSessions() {
    Bugsnag.getClient().sessionTracker.flushStoredSessions()
}

internal fun flushErrorStoreAsync(client: Client) {
    client.eventStore.flushAsync()
}

internal fun flushErrorStoreOnLaunch(client: Client) {
    client.eventStore.flushOnLaunch()
}

/**
 * Creates a delivery API client with a 500ms delay, emulating poor network connectivity
 */
internal fun createSlowDelivery(): Delivery {
    val delivery = createDefaultDelivery()

    return object : Delivery {
        override fun deliver(report: Report, deliveryParams: DeliveryParams): DeliveryStatus {
            Thread.sleep(500)
            return delivery.deliver(report, deliveryParams)
        }

        override fun deliver(payload: SessionPayload, deliveryParams: DeliveryParams): DeliveryStatus {
            Thread.sleep(500)
            return delivery.deliver(payload, deliveryParams)
        }
    }
}

internal fun createCustomHeaderDelivery(): Delivery {
    val delivery = createDefaultDelivery()

    return object : Delivery {
        override fun deliver(payload: SessionPayload, deliveryParams: DeliveryParams): DeliveryStatus {
            return delivery.deliver(payload, mutateDeliveryParams(deliveryParams))
        }

        override fun deliver(report: Report, deliveryParams: DeliveryParams): DeliveryStatus {
            return delivery.deliver(report, mutateDeliveryParams(deliveryParams))
        }

        fun mutateDeliveryParams(params: DeliveryParams): DeliveryParams {
            val map = params.headers.toMutableMap()
            map["Custom-Client"] = "Hello World"
            return DeliveryParams(params.endpoint, map.toMap())
        }
    }
}

internal fun createDefaultDelivery(): Delivery { // use reflection as DefaultDelivery is internal
    val clz = Class.forName("com.bugsnag.android.DefaultDelivery")
    return clz.constructors[0].newInstance(null, object: Logger {
        override fun e(msg: String) = Unit
        override fun e(msg: String, throwable: Throwable) = Unit
        override fun w(msg: String) = Unit
        override fun w(msg: String, throwable: Throwable) = Unit
        override fun i(msg: String) = Unit
        override fun i(msg: String, throwable: Throwable) = Unit
        override fun d(msg: String) = Unit
        override fun d(msg: String, throwable: Throwable) = Unit
    }) as Delivery
}

internal fun writeErrorToStore(client: Client) {
    val event = BugsnagPluginInterface.createAnrEvent(RuntimeException(), client)
    client.eventStore.write(event)
}