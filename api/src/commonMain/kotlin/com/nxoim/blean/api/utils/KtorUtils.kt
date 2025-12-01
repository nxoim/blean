package com.nxoim.blean.api.utils

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.http.Headers
import io.ktor.http.HttpMethod
import io.ktor.http.Parameters
import io.ktor.http.ParametersBuilder
import io.ktor.http.takeFrom

fun Request(
    url: String,
    method: HttpMethod,
    headers: Headers,
    parameters: Parameters? = null,
) = HttpRequestBuilder().apply {
    this.url.takeFrom(url)
    this.method = method
    this.headers.appendAll(headers)
    if (parameters != null) this.url.parameters.appendAll(parameters)
}

inline fun <reified T> Request(
    url: String,
    method: HttpMethod,
    headers: Headers,
    parameters: Parameters? = null,
    body: T
) = Request(url, method, headers, parameters).apply { setBody(body) }

suspend inline fun HttpClient.request(
    url: String,
    method: HttpMethod,
    headers: Headers,
    parameters: Parameters? = null,
) = this.request(Request(url, method, headers, parameters))

suspend inline fun <reified T> HttpClient.request(
    url: String,
    method: HttpMethod,
    headers: Headers,
    parameters: Parameters? = null,
    body: T
) = this.request(Request(url, method, headers, parameters, body))

suspend inline fun HttpClient.get(
    url: String,
    headers: Headers,
    parameters: Parameters? = null,
) = this.request(url, HttpMethod.Get, headers, parameters)

suspend inline fun <reified T> HttpClient.get(
    url: String,
    headers: Headers,
    parameters: Parameters? = null,
    body: T
) = this.request(url, HttpMethod.Get, headers, parameters, body)

suspend inline fun HttpClient.post(
    url: String,
    headers: Headers,
    parameters: Parameters? = null,
) = this.request(url, HttpMethod.Post, headers, parameters)

suspend inline fun <reified T> HttpClient.post(
    url: String,
    headers: Headers,
    parameters: Parameters? = null,
    body: T
) = this.request(url, HttpMethod.Post, headers, parameters, body)

suspend inline fun HttpClient.put(
    url: String,
    headers: Headers,
    parameters: Parameters? = null,
) = this.request(url, HttpMethod.Put, headers, parameters)

suspend inline fun <reified T> HttpClient.put(
    url: String,
    headers: Headers,
    parameters: Parameters? = null,
    body: T
) = this.request(url, HttpMethod.Put, headers, parameters, body)

suspend inline fun HttpClient.delete(
    url: String,
    headers: Headers,
    parameters: Parameters? = null,
) = this.request(url, HttpMethod.Delete, headers, parameters)

suspend inline fun <reified T> HttpClient.delete(
    url: String,
    headers: Headers,
    parameters: Parameters? = null,
    body: T
) = this.request(url, HttpMethod.Delete, headers, parameters, body)

suspend inline fun HttpClient.patch(
    url: String,
    headers: Headers,
    parameters: Parameters? = null,
) = this.request(url, HttpMethod.Patch, headers, parameters)

suspend inline fun <reified T> HttpClient.patch(
    url: String,
    headers: Headers,
    parameters: Parameters? = null,
    body: T
) = this.request(url, HttpMethod.Patch, headers, parameters, body)

suspend inline fun HttpClient.head(
    url: String,
    headers: Headers,
    parameters: Parameters? = null,
) = this.request(url, HttpMethod.Head, headers, parameters)

suspend inline fun <reified T> HttpClient.head(
    url: String,
    headers: Headers,
    parameters: Parameters? = null,
    body: T
) = this.request(url, HttpMethod.Head, headers, parameters, body)

suspend inline fun HttpClient.options(
    url: String,
    headers: Headers,
    parameters: Parameters? = null,
) = this.request(url, HttpMethod.Options, headers, parameters)

suspend inline fun <reified T> HttpClient.options(
    url: String,
    headers: Headers,
    parameters: Parameters? = null,
    body: T
) = this.request(url, HttpMethod.Options, headers, parameters, body)

inline fun formDataContentFromParameters(block: ParametersBuilder.() -> Unit) =
    FormDataContent(Parameters.build(block))



