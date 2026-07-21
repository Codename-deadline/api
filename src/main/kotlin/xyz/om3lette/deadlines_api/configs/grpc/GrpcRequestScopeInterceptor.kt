package xyz.om3lette.deadlines_api.configs.grpc

import io.grpc.ForwardingServerCallListener
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.grpc.server.GlobalServerInterceptor
import org.springframework.stereotype.Component
import org.springframework.web.context.request.AbstractRequestAttributes
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder
import java.util.concurrent.atomic.AtomicBoolean

// TODO: Remove if https://github.com/spring-projects/spring-grpc/issues/392 is resolved
@Component
@GlobalServerInterceptor
@Order(Ordered.HIGHEST_PRECEDENCE)
class GrpcRequestScopeInterceptor : ServerInterceptor {
    override fun <ReqT, RespT> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>
    ): ServerCall.Listener<ReqT> {
        val attributes = GrpcRequestAttributes()
        val completed = AtomicBoolean()

        fun complete() {
            if (!completed.compareAndSet(false, true)) return
            attributes.requestCompleted()
        }

        val delegate = try {
            withRequestAttributes(attributes) { next.startCall(call, headers) }
        } catch (error: Throwable) {
            complete()
            throw error
        }

        return object : ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(delegate) {
            private fun invokeDelegate(completeAfterwards: Boolean = false, action: () -> Unit) {
                try {
                    withRequestAttributes(attributes, action)
                } catch (error: Throwable) {
                    complete()
                    throw error
                } finally {
                    if (completeAfterwards) complete()
                }
            }

            override fun onMessage(message: ReqT) = invokeDelegate { super.onMessage(message) }

            override fun onHalfClose() = invokeDelegate { super.onHalfClose() }

            override fun onReady() = invokeDelegate { super.onReady() }

            override fun onCancel() = invokeDelegate(completeAfterwards = true) { super.onCancel() }

            override fun onComplete() = invokeDelegate(completeAfterwards = true) { super.onComplete() }
        }
    }

    private fun <T> withRequestAttributes(attributes: RequestAttributes, action: () -> T): T {
        val previous = RequestContextHolder.getRequestAttributes()
        RequestContextHolder.setRequestAttributes(attributes)
        return try {
            action()
        } finally {
            if (previous == null) {
                RequestContextHolder.resetRequestAttributes()
            } else {
                RequestContextHolder.setRequestAttributes(previous)
            }
        }
    }
}

private class GrpcRequestAttributes : AbstractRequestAttributes() {
    private val attributes = mutableMapOf<String, Any>()
    private val sessionMutex = Any()

    override fun getAttribute(name: String, scope: Int): Any? {
        requireRequestScope(scope)
        return attributes[name]
    }

    override fun setAttribute(name: String, value: Any, scope: Int) {
        requireRequestScope(scope)
        attributes[name] = value
    }

    override fun removeAttribute(name: String, scope: Int) {
        requireRequestScope(scope)
        attributes.remove(name)
        removeRequestDestructionCallback(name)
    }

    override fun getAttributeNames(scope: Int): Array<String> {
        requireRequestScope(scope)
        return attributes.keys.toTypedArray()
    }

    override fun registerDestructionCallback(name: String, callback: Runnable, scope: Int) {
        requireRequestScope(scope)
        registerRequestDestructionCallback(name, callback)
    }

    override fun resolveReference(key: String): Any? = null

    override fun getSessionId(): String = "grpc"

    override fun getSessionMutex(): Any = sessionMutex

    override fun updateAccessedSessionAttributes() = Unit

    private fun requireRequestScope(scope: Int) {
        require(scope == SCOPE_REQUEST) { "gRPC does not provide a session scope" }
        check(isRequestActive) { "gRPC request is no longer active" }
    }
}
