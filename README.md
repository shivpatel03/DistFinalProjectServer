## Project Information
- This is a portion of our final project for the Distributed Systems course
- The code within the 'demo' folder is **ONLY** the server-side code for our project
- The goal of this project overall is to develop a health-monitoring system using off-the-shelf wearable devices, running Android's Wear OS

## Setup 
- To run this, clone this repository:
```
git clone https://github.com/shivpatel03/DistFinalProjectServer.git\
```
- Go into the `demo` directory: <br>
```
cd demo
```
- Compile the project: <br>
```
mvn clean compile
```
- Run the required Java file, called 'Server.java', using Maven <br>
```
mvn exec:java -Dexec.mainClass="com.example.distfinalproject.presentation.Server"
```

You now have the **Server Portion** of this project running! <br>
Please go [to this](https://github.com/shivpatel03/DistFinalProjectApp) repository to get the application portion (Client-side code)
