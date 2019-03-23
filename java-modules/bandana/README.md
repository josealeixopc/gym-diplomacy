# bandana

This module of DipQ is responsible the BANDANA framework (v1.3.1) management. It contains:

- The classes for the creation of a Diplomacy game
    - `src\main\java\ddejonge.bandana\tournament`
- Examples of BANDANA bots
    - `src\main\java\ddejonge.bandana\exampleAgents`
- The DipQ bot tactical module and negotiation module. This bot will retrieve information from the game, communicate it through sockets to the Python modules, receive the actions to take and transmit them to the Parlance server.
    - `src\main\java\cruz.agents`

The whole dependencies are managed by Maven.

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Prerequisites

These are the prerequisites for development

- JDK
- Maven
- This repo (including the Python modules directory)
- Maybe something is missing...

```
Give examples
```

### Installing (TODO)

A step by step series of examples that tell you how to get a development env running.

```
Give the example
```

And repeat

```
until finished
```

End with an example of getting some data out of the system or using it for a little demo

## Running

### Creating a new tournament

The `build.gradle` file already contains the task for creating and running a new tournament. Simply execute the following command on the directory containing the Gradle file:

```
gradle runTournament
```

This command will execute the `tournament.TournamentRunner` class. It **executes the Parlance server initialization script** and then launches the tournament with the settings and agents specified in the `run` method.

**Be aware that the location to the Parlance server is relative, so you must fix it if you change this repo's structure.**

### Playing with a custom agent

In the BANDANA framework, an agent is contained inside a `.jar` file. Therefore, when building a custom agent, the agent and all its dependencies should be built and compiled into a JAR file that can then be referred to inside the `tournament.TournamentRunner` class definition.

### Running DipQBot

To run the main method of the DipQBot, simply execute:

```
gradle run
```

This should be used **only** for testing.

## Running the tests


## Deployment


## Built With


## Authors

* [José Aleixo](https://github.com/jazzchipc)

## License


## Acknowledgments

