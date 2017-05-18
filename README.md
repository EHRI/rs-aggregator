# rs-aggregator
Aggregate [ResourceSync](http://www.openarchives.org/rs/1.0.9/resourcesync) Sources

---
- The component in this repository is intended for system administrators and developers.
- Source location: [https://github.com/EHRI/rs-aggregator](https://github.com/EHRI/rs-aggregator).
- In case of questions [contact](https://github.com/EHRI/rs-aggregator/issues/new) the EHRI team.

---

The `Destination` in a 
[ResourceSync Framework](http://www.openarchives.org/rs/1.0.9/resourcesync) configuration keeps zero or 
more `sets of resources` from zero or more `Sources` synchronized.

## Quick start

1. Clone this repository to your local drive
```bash
# git clone https://github.com/EHRI/rs-aggregator.git
```
2. Start a [Docker](https://www.docker.com/) daemon, if it is not already running, switch to the 
 `docker` directory and run the start-script
```bash
# cd docker
# ./start.sh
```
If you see the `rs-aggregator` logo...
```bash
_________________________________________________________________________________________________________
     ______  ______       ___     ______  ______   ______   _____  _____    ___________   ______   ______
    / __  / / ____/      /***|   / ___ / / ___ /  / __  |  / ___| / ___/   /   ___  ___| / __   | / __  |
   / /_/ / / /___  ___  /*_**|  / / _   / / _    / /_/ /  / /__  / / _    / _  |  | |   / /  / / / /_/ / 
  /  _  | /___  / /__/ /*/_|*| / / | | / / | |  /  _  |  / ___/ / / | |  / /_| |  | |  / /  / / /  _  |  
 /  / | | ___/ /      /*___ *|/ /__| |/ /__| | /  / | | / /____/ /__| | / ___  |  | | / /__/ / /  / | |  
/__/  |_|/____/      /_/   |_||______/|______//__/  |_|/______/|______//_/   |_|  |_| |_____/ /__/  |_|  
__________________________________________________________________________________________________________
```
Congrats! You have just built a Java8 capable docker container, imported required libraries,
compiled, tested and packaged the source code and started the rs-aggregator application.
To stop it you can run the stop-script
```bash
# ./stop.sh
```