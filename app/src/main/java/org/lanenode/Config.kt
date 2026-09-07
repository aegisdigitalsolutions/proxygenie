package org.lanenode

import android.content.Context

data class Config(
    val listenPort: Int = 8899,
    val useUpstream: Boolean = true,
    val upstreamHost: String = "",
    val upstreamPort: Int = 20027,
    val upstreamUser: String = "",
    val upstreamPass: String = ""
) {
    companion object {
        private const val P = "lanenode"

        fun load(ctx: Context): Config {
            val sp = ctx.getSharedPreferences(P, Context.MODE_PRIVATE)
            return Config(
                listenPort = sp.getInt("port", 8899),
                useUpstream = sp.getBoolean("useUpstream", true),
                upstreamHost = sp.getString("uHost", BuildConfig.UPSTREAM_HOST)
                    ?: BuildConfig.UPSTREAM_HOST,
                upstreamPort = sp.getInt("uPort", BuildConfig.UPSTREAM_PORT),
                upstreamUser = sp.getString("uUser", BuildConfig.UPSTREAM_USER)
                    ?: BuildConfig.UPSTREAM_USER,
                upstreamPass = sp.getString("uPass", BuildConfig.UPSTREAM_PASS)
                    ?: BuildConfig.UPSTREAM_PASS
            )
        }

        fun save(ctx: Context, c: Config) {
            ctx.getSharedPreferences(P, Context.MODE_PRIVATE).edit()
                .putInt("port", c.listenPort)
                .putBoolean("useUpstream", c.useUpstream)
                .putString("uHost", c.upstreamHost)
                .putInt("uPort", c.upstreamPort)
                .putString("uUser", c.upstreamUser)
                .putString("uPass", c.upstreamPass)
                .apply()
        }
    }
}
