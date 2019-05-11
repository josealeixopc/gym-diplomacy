#!/bin/bash

kill $(ps -e -o pid,pcpu,command --sort=-pcpu | grep "parlance\|agents\|open-ai\|usr/bin/java\|tournament-runner.jar" | awk '{print $1}')
