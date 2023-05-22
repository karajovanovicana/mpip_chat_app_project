package com.example.chatapp.data

class User {
    var name: String?=null
    var email: String?=null
    var uid: String?=null
    var deviceToken: String?=null

    constructor()

    constructor(name: String?, email: String?, uid: String?, deviceToken: String) {
        this.name = name
        this.email = email
        this.uid = uid
        this.deviceToken = deviceToken
    }
}