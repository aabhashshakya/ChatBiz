# chat-app
Chat App w/ Firebase

## Features:

**Send text and image messages**

**Customize your profile**

**Receive realtime notifications**


## Steps to make it work: 

**Connecting Firebase:**

1. Connect Firebase to this app: *https://firebase.google.com/docs/android/setup*
2. Enable the database, authentication and FCM services of Firebase.


**Get the Maps api credentials:**

1. Log in to Google Cloud Platform console. 
2. Create a new project.
2. Enable Maps api.
3. Create a credential for Maps api and copy it.
4. Goto res/strings.xml in Android Studio and create a string resource with name 'google_maps_key' with the api key as value


**Get the Firebase server key for FCM:**

1. Goto Firebase Console -> *project_name* -> Project Settings -> Cloud Messaging 
2. Copy the server key.
3. Paste the key in FCMAPIService.kt class file where it says YOUR_API_KEY_HERE

Good to go. 
