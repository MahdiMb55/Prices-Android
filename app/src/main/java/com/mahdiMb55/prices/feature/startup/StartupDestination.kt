package com.mahdiMb55.prices.feature.startup

import com.mahdiMb55.prices.data.local.connection.StoredConnection

enum class StartupDestination {
    Onboarding,
    Pairing;

    companion object {
        fun from(connection: StoredConnection?): StartupDestination =
            if (connection == null) Onboarding else Pairing
    }
}
