# GoBoxClient
This repository contains the source of the GoBox Client and also the Storage.

## What is GoBox
GoBox is a platform that let you create your personal cloud easily. You just need
to create a new account, download and install the Storage, the software that prepare
your pc to manage your cloud.

Then, log in with your account and access to your file from the WebApp or installing the Client to another computer.

## Current structure
Currently GoBox uses the GoBoxServer to connect together the clients and the storage of each user.
The GoBoxJavaApi repository contains all the classes and the interfaces to communicate with the storahe
in Java.
GoBoxClient is the pc client and also the storage software, and uses the GoBoxJavaApi.
Then we have the GoBoxWebApp which is the client web and the GoBoxPhoto, the Android photo uploader applicaiton.

## Next changes
The next move for GoBox is to switch to WebRTC and Firebase and remove the GoBoxServer, which isn't very scalable.
Once that the new protocol will be implemented, GoBox will be released for everyone!

## Contributing
Currently there are no contributors, but GoBox needs your help to be improved!