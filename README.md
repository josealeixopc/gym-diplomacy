# dip-q

This repository contains the source code for the Diplomacy Open AI Gym environment and for the agent that'll play the game.

## Arquitecture

TODO

## Directory structure

### java-modules

This directory contains all the Java modules. At the moment only BANDANA and the OpenAIAgent are in it.

### protobuf

This directory contains the Protobuf messages that are exchanged between modules.

### python-modules

This directory contains all the Python modules. This include the Diplomacy Open AI environment and the Python module (`dip-q-brain`) that acts on that environment.

### tutorials

This directory is only for sand-boxing purposes, such as experimenting new tools and frameworks.

## Running

### With Docker

To run with Docker, you must first build the Java artifacts using `mvn package`. Then simply build the Dockerfile in the root of this directory:

``` bash
docker build --rm -f "Dockerfile" -t dip-q:latest .
```

This will create an image with every dependency needed to execute the project. The default environment for the image is `development`, which means you are able to compile Java code and run Maven inside the container.

To run a container and navigate inside:

``` bash
docker run --rm -it dip-q:latest
```

#### Strategic agent

**Don't forget to set tup the `TournamentRunner` class in order to run the strategic board, player and adapter.**

To run the strategic agent inside the container, navigate to the `deepdip` folder and run the following command:

```bash
pipenv run python deepdip_brain.py
```

#### Negotiation agent

TODO