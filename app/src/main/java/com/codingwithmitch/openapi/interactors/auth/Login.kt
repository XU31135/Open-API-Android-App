package com.codingwithmitch.openapi.interactors.auth

import com.codingwithmitch.openapi.api.auth.OpenApiAuthService
import com.codingwithmitch.openapi.api.handleUseCaseException
import com.codingwithmitch.openapi.models.Account
import com.codingwithmitch.openapi.models.AuthToken
import com.codingwithmitch.openapi.persistence.account.AccountDao
import com.codingwithmitch.openapi.persistence.account.toEntity
import com.codingwithmitch.openapi.persistence.auth.AuthTokenDao
import com.codingwithmitch.openapi.persistence.auth.toEntity
import com.codingwithmitch.openapi.util.*
import com.codingwithmitch.openapi.util.ErrorHandling.Companion.ERROR_SAVE_AUTH_TOKEN
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import java.lang.Exception

class Login(
    private val service: OpenApiAuthService,
    private val accountDao: AccountDao,
    private val authTokenDao: AuthTokenDao,
){
    fun execute(
        email: String,
        password: String,
    ): Flow<DataState<AuthToken>> = flow {
        emit(DataState.loading<AuthToken>())
        val loginResponse = service.login(email, password)
        // Incorrect login credentials counts as a 200 response from server, so need to handle that
        if(loginResponse.response.equals(ErrorHandling.GENERIC_AUTH_ERROR)){
            throw Exception(ErrorHandling.INVALID_CREDENTIALS)
        }

        // cache the Account information (don't know the username yet)
        accountDao.insertOrIgnore(
            Account(
                pk = loginResponse.pk,
                email = loginResponse.email,
                username = ""
            ).toEntity()
        )

        // cache the auth token
        val authToken = AuthToken(
            loginResponse.pk,
            loginResponse.token
        )
        val result = authTokenDao.insert(authToken.toEntity())
        // can't proceed unless token can be cached
        if(result < 0){
            throw Exception(ERROR_SAVE_AUTH_TOKEN)
        }
        emit(DataState.data(data = authToken, response = null))
    }.catch { e ->
        emit(handleUseCaseException(e))
    }
}














