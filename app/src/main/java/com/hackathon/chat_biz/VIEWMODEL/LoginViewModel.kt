package com.hackathon.chat_biz.VIEWMODEL


import androidx.hilt.lifecycle.ViewModelInject

import androidx.lifecycle.ViewModel

import com.hackathon.chat_biz.REPOSITORY.LoginRepository

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult


private const val TAG = "ProfileViewModel"

class LoginViewModel @ViewModelInject constructor(
    private val repository: LoginRepository
) : ViewModel() {

    fun loginUser(email: String, password: String): Task<AuthResult> {
        return repository.loginUser(email, password)

    }


}