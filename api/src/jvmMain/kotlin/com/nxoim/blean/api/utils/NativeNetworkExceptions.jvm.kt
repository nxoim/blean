package com.nxoim.blean.api.utils

import java.net.NoRouteToHostException

actual fun Exception.isNoRouteHostException() = this is NoRouteToHostException