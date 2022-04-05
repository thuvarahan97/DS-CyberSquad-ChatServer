# CS4262_Distributed Systems
# Distributed Chat Server

## Project Demo

### Run Server Application
Set Program Argument in the IDE Run Configurations as bellow and run the project.
```bash
-p "servers.txt" -s "s1"
```
OR

Export the project as `chatserver.jar` file and execute the following command inside the folder containing `chatserver.jar`.
```bash
java -jar chatserver.jar -p "servers.txt" -s "s1"
```

### Run Chat Application
Execute the following command inside the folder containing `client.jar` file.
```bash
java -jar client.jar -h localhost -p 4444 -i adel -d
```
