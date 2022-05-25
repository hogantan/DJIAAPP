
# DJIAAPP (GCS)

Welcome to the DJI Android App (**DJIAAPP**) or Ground Control Station (**GCS**) application. **DJIAAP** is built primarily with the DJI Mobile Android SDK alongside various libraries. 

**DJIAAPP** is designed to have the controlled DJI drone follow and track down a drone via the use of computer vision and a controller script.

Features that **DJIAAPP** offers:
- Uploading and executing waypoint missions (linear or curved)
- Virtual joystick control of drone
- Live streaming video feed of drone (RTSP and RTMP)
- Autonomous movement control of drone
- Logging of information such as telemetry, video quality and drone speed
- Geo-fencing

Compatibility (**DJIAAPP** has been tested using): 
- Mavic 2 Pro
- Phantom 3 Pro
- DJI Smart Controller (Android 5.0 and above)
- Phantom 3 Controler + Sony Xperia XZ2


## Table of Content
1. [Quickstart](#quickstart)
2. [Architecture](#architecture)
3. [Features](#third-example)
4. [Testing](#fourth-examplehttpwwwfourthexamplecom)
5. [Integrating with Deepstream and Controller Script](#fourth-examplehttpwwwfourthexamplecom)

## Quickstart
This section provides a quickstart guide of using **DJIAAPP**. This quickstart guide assumes that the following requirements are met:
- GPU Laptop that has Deepstream application (`dvd.py`) and **Controller Script** (`receiveampq3.py`)
- DJI Drone (Mavic 2 Pro)
- DJI Smart Controller

After meeting the above requirements, follow the steps below:

1. Download the apk [here](https://github.com/hogantan/DJIAAPP2/releases/tag/v1)
2. Install apk in DJI Smart Controller
3. Power on drone 
> Note: Where you power on the drone is where the drone's home point is. Therefore, power it on in a safe region for takeoff and landing
4. Connect DJI Smart Controller to GPU Laptop
5. Open terminal in GPU Laptop and run: 
```
$ cdgo
$ ./djiaapp_init.sh
```
6. Key in GPU Laptop password in new tab
7. Open **DJIAAPP** on Smart Controller 
8. After connecting successfully to drone, hit the `Start` button on screen to go to video view
9. Hit the `Start RTSP button` on screen and the drone's live video should be streaming to the RTSP server on the GPU Laptop
> To check whether stream is received by the server, check RTSP server terminal on GPU Laptop if the RTSP stream is not opened, it could be that the IP address of the RTSP server is incorrect in which case hit back and change the IP address in the settings. 
10. Launch Deepstream on the GPU Laptop by running the command: `$ py dvd.py -i rtsp://localhost:8554/test` 
11. Takeoff the drone by hitting the `Toggle UI button` then the  `Takeoff button` on screen
12. Position the drone in desired position using `Virtual Joystick buttons` on screen 
13. Turn on the command listener on DJIAAP by hitting the `Command Listener button`
14. Launch **Controller Script** on GPU Laptop by running the command: `$ py receiveamqp3.py` 
15. Viola! Your drone should be following a drone in the video view! (or at least something else that has been detected)

## Architecture

### Fully Integrated System
The following diagram illustrates the entire architecture of the entire system (**DJIAAPP** + **Deepstream Application** + **Controller Script**).

![fullsystem](https://user-images.githubusercontent.com/65152263/170230251-f38f414e-34a8-4451-980f-08ba6ca643bd.png)

1. **DJIAAPP** obtains live video feed from DJI drone
2. **DJIAAPP** streams out the live video feed via RTSP or RTMP to a server on the GPU Laptop
3. **Deepstream application** on GPU Laptop consumes RTSP stream from server and runs inference,etc.
4. **Deepstream application** upon inference outputs coordinates of bounding boxes to **Controller Script** running on GPU Laptop
5. **Controller Script** processes coordinates and sends out movement commands to DJIAAPP to consume
6. **DJIAAPP** consumes movement commands and executes movement commands

### DJIAAPP

**DJIAAPP** follows the Model View ViewModel (MVVM) architecture. It contains only 3 activities namely: Connection, Home and Video. The following images depicts the various activities and provides information on each activity. 

#### Connection Activity

![connection](https://user-images.githubusercontent.com/65152263/170230391-e8ae7ba7-82ca-4d62-8835-6a9f2e961bbb.jpg)

This is the landing page of **DJIAAPP** when the user first starts the application. 

It establishes connection with DJI's Mobile SDK as well as the DJI Drone. This activity is only opened upon launching the application.  Upon successfully connecting, this activity will not be able to returned to.

#### Home Activity

![home](https://user-images.githubusercontent.com/65152263/170230455-ae066a61-6807-4986-a0dc-7777862523dc.jpg)

UI Legend:
1. `Start button` to move to [Video Activity](#video-activity)
2. `Upload Mission button` to open file browser and select mission to be uploaded
3. `Settings button` to change certain settings such as RTSP server address, drone speed, etc. 

This is the home page of **DJIAAPP** where users can tune certain settings as well as upload a waypoint mission. 

#### Video Activity

VideoActivity (Untoggled):

![video1](https://user-images.githubusercontent.com/65152263/170230494-b3b6ffc8-8d89-4871-8e33-e8a93828c521.jpg)

VideoActivity (Toggled):

![video2](https://user-images.githubusercontent.com/65152263/170230520-684c2c9a-6ef4-4ebe-8fc6-1308eafc1905.jpg)

UI Legend:
1. `Back button` to return to [Home Activity](#home-activity)
2. `RTSP button` to toggle start/stop of RTSP stream
3. `Command Listener button` to toggle start/stop of listening to commands from the **Controller Script**
4. `RTMP button` to toggle start/stop of RTMP stream
5. `Mission button` to toggle start/stop of executing waypoint mission
> Note: this button is only present if user has uploaded a mission previously in [Home Activity](#home-activity).
6. `Toggle UI button` to toggle UI
7. `Current Mode of Drone` represents the current mode of drone 
8. `Altitude and velocity indicators` displays the real time altitude and velocity of drone
9. `Virtual joystick buttons` to control movement of drone
10. `Land button` for drone to return to home and land
11. `Takeoff button` for drone to takeoff 

This is the main page of **DJIAAPP** where most key features are located in namely live streaming of live video feed and virtual control of drone.

> Note: Both Home and Video activity are only created **once** that means that there is only one instance of each activity every time the application ran. That is both activities are able to switch/toggle between each other without having the need to create a new activity every time.

## Features
### 1. Waypoint Mission

This section explains the various logic in the `WaypointMissionhandler` class. 

**1. Parsing Waypoint Mission files** 

Parses a **XML** file and generates the various waypoints to be uploaded. Below is a sample of such file. 
```
<?xml version="1.0" encoding="UTF-8"?>
<kml xmlns="http://www.opengis.net/kml/2.2" xmlns:gx="http://www.google.com/kml/ext/2.2">
  <Document>
    <name>KML Drone Path</name>
    <Folder>
      <Placemark>
        <Point>
          <altitudeMode>relativeToGround</altitudeMode>
          <coordinates>103.7843396,1.3300671,50.0,15,0.5</coordinates>
        </Point>
      </Placemark>
      <Placemark>
        <Point>
          <altitudeMode>relativeToGround</altitudeMode>
          <coordinates>103.78434365960734,1.3300008989011576,50.0,15,0.5</coordinates>
        </Point>
      </Placemark>
    </Folder>
    <Placemark>
      <LineString>
        <altitudeMode>relativeToGround</altitudeMode>
        <coordinates>
		103.7843396,1.3300671,50.0,15,0.5 103.78434365960734,1.3300008989011576,50.0,15,0.5
        </coordinates>
      </LineString>
    </Placemark>
  </Document>
</kml>

```

Key point to note here is that the format under the coordinates tag is as follows: (latitude, longitude, altitude, speed, turn radius). 

Refer to [here](https://developer.dji.com/api-reference/android-api/Components/Missions/DJIWaypoint.html) for more information on what each field means. 

Setting turn radius allows for waypoint missions to be 'smoother' if turn radius = 0 then drone will pause at each waypoint before moving to the next.

**2. Uploading Waypoint Mission** 

Uploads parse waypoints to the drone. 

Waypoints have to meet certain requirements in order to be successfully uploaded. See [here](https://developer.dji.com/api-reference/android-api/Components/SDKError/DJIError_DJIMissionManagerError.html?search=waypoint&i=7&#djierror_djisdkmissionerror_waypointerrorspeed_inline).
> Note: That are instances where uploading of missions can fail most prominently due to poor connection / interference between DJI controller and the drone. If so, try readjusting or going nearer to drone. 

**3. Executing Waypoint Mission** 

Executes successfully uploaded waypoint mission. When executing mission, drone will start listening to commands. 

In other words, if a movement command is sent via the **Controller Script**, the drone will switch to Chase Mode. 

### 2. Virtual Control
### 3. Live Streaming
### 4. Logging
### 5. Failsafe
