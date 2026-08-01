package com.mahdiMb55.prices.feature.startup

import com.mahdiMb55.prices.data.local.session.StoredPairedSessionMetadata
import com.mahdiMb55.prices.data.repository.PersistSessionResult
import com.mahdiMb55.prices.data.repository.SecureSessionRepository
import com.mahdiMb55.prices.data.repository.SessionCleanupResult
import com.mahdiMb55.prices.data.repository.StartupResolution
import com.mahdiMb55.prices.data.repository.StartupSessionResolver
import com.mahdiMb55.prices.data.remote.InMemoryAccessTokenProvider
import com.mahdiMb55.prices.data.security.SecureTokenStorage
import com.mahdiMb55.prices.data.session.PairedSession
import com.mahdiMb55.prices.data.session.InMemorySessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StartupViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun startsLoadingThenResolvesOnboarding() = runTest(dispatcher.scheduler) {
        val viewModel = viewModel(FakeResolver(mutableListOf(StartupResolution.Onboarding)))
        assertTrue(viewModel.uiState.value === StartupUiState.Loading)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value === StartupUiState.Onboarding)
    }

    @Test fun resolvesPairingProductsAndRetryableFailureWithoutTokenInUiState() = runTest(dispatcher.scheduler) {
        val resolver = FakeResolver(
            mutableListOf(
                StartupResolution.RetryableVerificationFailure(
                    com.mahdiMb55.prices.data.repository.StartupVerificationFailure.RetryableNetworkFailure,
                    "Example Store",
                ),
                StartupResolution.Products("Example Store"),
            ),
        )
        val viewModel = viewModel(resolver)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is StartupUiState.RetryableVerificationFailure)
        viewModel.retryVerification()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is StartupUiState.Products)
        assertFalse(viewModel.uiState.toString().contains("token"))
    }

    @Test fun retryStartsOneRequestAndCanResolveProducts() = runTest(dispatcher.scheduler) {
        val resolver = FakeResolver(
            mutableListOf(
                StartupResolution.RetryableVerificationFailure(
                    com.mahdiMb55.prices.data.repository.StartupVerificationFailure.RetryableTimeout,
                    "Example Store",
                ),
                StartupResolution.Products("Example Store"),
            ),
        )
        val viewModel = viewModel(resolver)
        advanceUntilIdle()
        viewModel.retryVerification()
        viewModel.retryVerification()
        advanceUntilIdle()
        assertEquals(2, resolver.calls)
        assertTrue(viewModel.uiState.value is StartupUiState.Products)
    }

    @Test fun returnToPairingAndChangeStoreUseCleanupBoundary() = runTest(dispatcher.scheduler) {
        val cleanup = FakeSecureSessionRepository()
        val viewModel = StartupViewModel(FakeResolver(mutableListOf(StartupResolution.Products("Example"))), cleanup)
        advanceUntilIdle()
        viewModel.returnToPairing()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value === StartupUiState.Pairing)
        assertEquals(1, cleanup.localClears)

        viewModel.changeStore()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value === StartupUiState.Onboarding)
        assertEquals(1, cleanup.storeClears)
    }

    private fun viewModel(resolver: FakeResolver) = StartupViewModel(
        resolver,
        FakeSecureSessionRepository(),
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
private class FakeResolver(
    val results: MutableList<StartupResolution>,
) : StartupSessionResolver {
    var calls = 0
    override suspend fun resolve(): StartupResolution {
        calls++
        return results.removeAt(0)
    }
}

private class FakeSecureSessionRepository : SecureSessionRepository {
    var localClears = 0
    var storeClears = 0
    override fun clearInMemorySession() = Unit
    override suspend fun persistVerifiedSession(token: String, metadata: StoredPairedSessionMetadata, session: PairedSession) = PersistSessionResult.Success
    override suspend fun clearLocalSession(): SessionCleanupResult {
        localClears++
        return SessionCleanupResult.Success
    }
    override suspend fun clearAllForStoreChange(): SessionCleanupResult {
        storeClears++
        return SessionCleanupResult.Success
    }
}
