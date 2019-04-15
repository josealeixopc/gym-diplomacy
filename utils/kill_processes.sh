#!/bin/bash

kill $(ps -e -o pid,pcpu,command --sort=-pcpu | grep "parlance\|agents\|open-ai\|usr/bin/java" | awk '{print $1}')
