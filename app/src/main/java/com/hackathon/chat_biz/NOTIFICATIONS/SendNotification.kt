package com.hackathon.chat_biz.NOTIFICATIONS

//this is the object that we send as the body in POST Method. The API expects a JSON body of this structure:

// {
//    data:{
//            data1: ...,
//            data2: ...,
//            data3: ...
//         }
//    to: ...
// }


//here 'to' is the Token of the receiver
//We have to name it 'to' as it is defined so in the API of the FCM//to is the TOKEN of the receiver
data class SendNotification(val data: NotificationData, val to: String) {
}