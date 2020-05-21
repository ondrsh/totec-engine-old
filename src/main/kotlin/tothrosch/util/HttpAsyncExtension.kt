package tothrosch.util

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.cancelFutureOnCancellation
import kotlinx.coroutines.suspendCancellableCoroutine
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.concurrent.FutureCallback
import org.apache.http.nio.client.HttpAsyncClient
import java.util.concurrent.Future
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun HttpAsyncClient.execute(request: HttpUriRequest): HttpResponse {
	
	return suspendCancellableCoroutine { cont: CancellableContinuation<HttpResponse> ->
		val future: Future<HttpResponse> = this.execute(request, object : FutureCallback<HttpResponse> {
			override fun completed(result: HttpResponse) = cont.resume(result)
			override fun failed(ex: Exception) = cont.resumeWithException(ex)
			override fun cancelled() {}
		})
		
		// TODO was cancelFutureOnCompletion earlier, check if still working
		cont.cancelFutureOnCancellation(future)
	}
	
}