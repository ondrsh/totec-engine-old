package tothrosch.util

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase
import java.net.URI


class HttpDeleteWithBody : HttpEntityEnclosingRequestBase {
	
	override fun getMethod(): String {
		return METHOD_NAME
	}
	
	constructor(uri: String) : super() {
		setURI(URI.create(uri))
	}
	
	constructor(uri: URI) : super() {
		setURI(uri)
	}
	
	constructor() : super() {}
	
	companion object {
		val METHOD_NAME = "DELETE"
	}
}